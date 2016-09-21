/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 * 		- http://gdz.sub.uni-goettingen.de 
 * 		- http://www.intranda.com 
 * 		- http://www.digiverso.com
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software
 * 
 * This is the extended version updated by intranda
 * Copyright 2012, intranda GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the “License�?);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS�? BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.unigoettingen.sub.commons.contentlib.servlet.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibPdfException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ContentLibUtil;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFBookmark;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFManager;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFManager.PdfPageSize;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFPage;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;

/************************************************************************************
 * pdf action for all kinds of simple pdf handlings first of all validate all request parameters, and than interprete all request parameters for
 * correct image handling
 * 
 * @version 02.01.2009Â 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class GetPdfAction extends AbstractGetAction {

    /************************************************************************************
     * exectute all simple pdf actions and send pdf back to output stream of the servlet, after setting correct mime type
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws URISyntaxException
     * @throws ContentLibException 
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException,
            ContentLibException {
        super.run(servletContext, request, response);
        Watermark myWatermark = null;
        /*
         * get central configuration and retrieve source image from url
         */
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();

        /*
         * parse images and get all image paths into HashMap
         */
        HashMap<Integer, UrlImage> urlMap = new HashMap<Integer, UrlImage>();
        String images = request.getParameter("images");
        StrTokenizer imagetokenizer = new StrTokenizer(images, "$");
        int i = 1;
        for (String image : imagetokenizer.getTokenArray()) {
            URL imagePath = new URL(config.getRepositoryPathImages() + image);
            PDFPage page = new PDFPage();
            page.setURL(imagePath);
            urlMap.put(i++, page);
        }

        /*
         * parse image names and get all into HashMap
         */
        HashMap<Integer, String> nameMap = new HashMap<Integer, String>();
        if (request.getParameter("imageNames") != null) {
            String allnames = request.getParameter("imageNames");
            StrTokenizer imageNametokenizer = new StrTokenizer(allnames, "$");
            i = 1;
            for (String name : imageNametokenizer.getTokenArray()) {
                nameMap.put(i++, name.trim());
            }
        }

        /*
         * parse bookmarks and get all into HashMap
         */
        HashMap<String, PDFBookmark> allBookmarks = new HashMap<String, PDFBookmark>();
        LinkedList<PDFBookmark> topBookmarks = new LinkedList<PDFBookmark>();

        /* run through all bookmark defnition */
        StrTokenizer bookmarkDefTokenizer = new StrTokenizer(request.getParameter("bookmarks"), "$");
        for (String bookmarkDef : bookmarkDefTokenizer.getTokenArray()) {
            /* get all bookmark definition values as strings from tokenizer */
            StrTokenizer bookmarkDefValues = new StrTokenizer(bookmarkDef, ",");
            String defID = bookmarkDefValues.getTokenArray()[0].trim();
            String defParentID = bookmarkDefValues.getTokenArray()[1].trim();
            String defImageNumber = bookmarkDefValues.getTokenArray()[2].trim();
            String defTitle = bookmarkDefValues.getTokenArray()[3].trim();
            /* create bookmark and add it to HashMap */
            PDFBookmark bm = new PDFBookmark(Integer.parseInt(defImageNumber) + 1, defTitle);
            if (defParentID.equals("0")) {
                topBookmarks.add(bm);
            } else {
                allBookmarks.get(defParentID).addChildBookmark(bm);
            }
            allBookmarks.put(defID, bm);
        }

        /*
         * create new PDFManager to create the PDF files
         */
        PDFManager pdfmanager = new PDFManager(urlMap);
        /* add bookmarks */
        pdfmanager.setStructureList(topBookmarks);
        /* add image names if present */
        if (request.getParameter("imageNames") != null) {
            pdfmanager.setImageNames(nameMap);
        }

        if (config.getWatermarkUse()) {
            File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
            myWatermark = Watermark.generateWatermark(request, watermarkfile);
        }

        /*
         * define conversion parameters from request or from configuration --------------------------------
         */
        /* alwaysUseRenderedImage */
        if (request.getParameter("alwaysUseRenderedImage") != null) {
            pdfmanager.setAlwaysUseRenderedImage(Boolean.parseBoolean(request.getParameter("alwaysUseRenderedImage")));
        } else {
            pdfmanager.setAlwaysUseRenderedImage(config.getPdfDefaultAlwaysUseRenderedImage());
        }
        /* alwaysCompressToJPEG */
        if (request.getParameter("alwaysCompressToJPEG") != null) {
            pdfmanager.setAlwaysCompressToJPEG(Boolean.parseBoolean(request.getParameter("alwaysCompressToJPEG")));
        } else {
            pdfmanager.setAlwaysCompressToJPEG(config.getPdfDefaultAlwaysCompressToJPEG());
        }

        /*
         * set pdf metadata --------------------------------
         */
        /* author */
        if (request.getParameter("metadataAuthor") != null) {
            pdfmanager.setAuthor(request.getParameter("metadataAuthor").trim());
        }
        /* creator */
        if (request.getParameter("metadataCreator") != null) {
            pdfmanager.setCreator(request.getParameter("metadataCreator").trim());
        }
        /* title */
        if (request.getParameter("metadataTitle") != null) {
            pdfmanager.setTitle(request.getParameter("metadataTitle").trim());
        }
        /* subject */
        if (request.getParameter("metadataSubject") != null) {
            pdfmanager.setSubject(request.getParameter("metadataSubject").trim());
        }
        /* keyword */
        if (request.getParameter("metadataKeyword") != null) {
            pdfmanager.setKeyword(request.getParameter("metadataKeyword").trim());
        }

        /*
         * set file name and attachment header from parameter or from configuration
         */
        StringBuilder targetFileName = new StringBuilder();
        if (config.getSendPdfAsAttachment()) {
            targetFileName.append("attachment; ");
        }
        targetFileName.append("filename=");

        if (request.getParameter("targetFileName") != null) {
            targetFileName.append(request.getParameter("targetFileName"));
        } else {
            String filename = ContentLibUtil.getCustomizedFileName(config.getDefaultFileNamePdf(), ".pdf");
            targetFileName.append(filename);
        }
        response.setHeader("Content-Disposition", targetFileName.toString());
        response.setContentType("application/pdf");

        /*
         * write pdf to response stream
         */
        try {
            pdfmanager.createPDF(response.getOutputStream(), PdfPageSize.ORIGINAL, myWatermark);
        } catch (URISyntaxException e) {
            throw new ContentLibPdfException("error while creating pdf file", e);
        } finally {
            if (response.getOutputStream() != null) {
                response.getOutputStream().flush();
                response.getOutputStream().close();
            }
        }
    }

    /************************************************************************************
     * validate all parameters of request for pdf handling, throws IllegalArgumentException if one request parameter is not valid
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @throws IllegalArgumentException
     ************************************************************************************/
    @Override
    public void validateParameters(HttpServletRequest request) throws IllegalArgumentException {

        /* call super.validation for main parameters of image and pdf actions */
        super.validateParameters(request);

        /*
         * -------------------------------- validate images --------------------------------
         */
        /* validate if parameter images is used */
        if (request.getParameter("images") == null) {
            throw new IllegalArgumentException("no images defined to use for pdf generation (images)");
        }

        /* check if every file has a file extension */
        String images = request.getParameter("images");
        StrTokenizer imagetokenizer = new StrTokenizer(images, "$");
        for (String image : imagetokenizer.getTokenArray()) {
            int dotPos = image.lastIndexOf(".");
            if (dotPos == -1) {
                throw new IllegalArgumentException("no file extension for image: " + image);
            }
        }

        /* validate if parameter imageNames is used and valid */
        if (request.getParameter("imageNames") != null) {
            String imageNames = request.getParameter("imageNames");
            StrTokenizer imageNametokenizer = new StrTokenizer(imageNames, "$");
            if (imageNametokenizer.getTokenArray().length != imagetokenizer.getTokenArray().length) {
                throw new IllegalArgumentException("list of image names (elements: " + imageNametokenizer.getTokenArray().length
                        + ") must have same size as list of images (elements: " + imagetokenizer.getTokenArray().length + "): "
                        + imageNametokenizer.getContent());
            }
        }

        /*
         * -------------------------------- validate bookmarks --------------------------------
         */
        /* validate if parameter bookmarks is used and valid */
        if (request.getParameter("bookmarks") != null) {
            String bookmarks = request.getParameter("bookmarks");
            ArrayList<String> allIDs = new ArrayList<String>();
            allIDs.add("0");
            /* run through all bookmark defnitions */
            StrTokenizer bookmarkDefTokenizer = new StrTokenizer(bookmarks, "$");
            for (String bookmarkDef : bookmarkDefTokenizer.getTokenArray()) {
                /*
                 * each BookmarkDefinition contains of 4 values: id, parentId, imagenumber, title
                 */
                StrTokenizer bookmarkDefValues = new StrTokenizer(bookmarkDef, ",");
                if (bookmarkDefValues.getTokenArray().length != 4) {
                    throw new IllegalArgumentException("bookmark definition does not contain the 4 values id, parentId, imageNumber and title: "
                            + bookmarkDefValues.getContent());
                }

                /* get all bookmark definition values as strings from tokenizer */
                String defID = bookmarkDefValues.getTokenArray()[0].trim();
                String defParentID = bookmarkDefValues.getTokenArray()[1].trim();
                String defImageNumber = bookmarkDefValues.getTokenArray()[2].trim();
                String defTitle = bookmarkDefValues.getTokenArray()[3].trim();

                /* id, parentId and image number have to be numeric */
                if (!StringUtils.isNumeric(defID) || !StringUtils.isNumeric(defParentID) || !StringUtils.isNumeric(defImageNumber)) {
                    throw new IllegalArgumentException("id, parentId and image number of bookmark definition have to be numeric: "
                            + bookmarkDefValues.getContent());
                }

                /* title must not be whitespace or empty */
                if (StringUtils.isBlank(defTitle)) {
                    throw new IllegalArgumentException("title of bookmark definition must not be empty: " + bookmarkDefValues.getContent());
                }

                /* image index number must exist */
                int imageNumber = Integer.parseInt(defImageNumber);
                int allImagesSize = imagetokenizer.getTokenArray().length;
                if (imageNumber < 0 || imageNumber >= allImagesSize) {
                    throw new IllegalArgumentException("image number is greater than defined list of images: " + bookmarkDefValues.getContent());
                }

                /* id have to be unique */
                if (allIDs.contains(defID)) {
                    throw new IllegalArgumentException("id of bookmark have to be unique: " + defID);
                } else {
                    allIDs.add(defID);
                }

                /* parentId must not be the same as id */
                if (defID.equals(defParentID)) {
                    throw new IllegalArgumentException("parentId of bookmark can not be the same as id: " + bookmarkDefValues.getContent());
                }

                /* parentIds have to exist */
                if (!allIDs.contains(defParentID)) {
                    throw new IllegalArgumentException("parentId of bookmark have to exist: " + defID);
                } else {
                    allIDs.add(defID);
                }

            }
        }

    }
}
