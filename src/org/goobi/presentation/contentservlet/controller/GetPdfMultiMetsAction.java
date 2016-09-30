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
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goobi.presentation.contentservlet.controller;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.log4j.Logger;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ContentLibUtil;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.pdflib.DocumentPart;
import de.unigoettingen.sub.commons.contentlib.pdflib.DocumentPart.DocumentPartType;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFConfiguration;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFCreator;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFTitlePage;
import de.unigoettingen.sub.commons.contentlib.servlet.ServletWatermark;
import de.unigoettingen.sub.commons.contentlib.servlet.Util;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.simplemets.SimplePDFMetadataExtractor;
import de.unigoettingen.sub.commons.simplemets.SimpleStructureMetadataExtractor;
import de.unigoettingen.sub.commons.simplemets.StructureMetadataExtractor;

/************************************************************************************
 * pdf multi mets action for pdf creation of multiple mets files first of all validate all request parameters, and than interprete all request
 * parameters for correct image handling
 * 
 * @version 25.04.2009
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class GetPdfMultiMetsAction extends GetMetsPdfAction {
    private static final Logger LOGGER = Logger.getLogger(GetPdfMultiMetsAction.class);

    /************************************************************************************
     * exectute multiple mets handling and generation of one pdf file from it to send it back to output stream of the servlet, after setting correct
     * mime type
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws ServletException
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws CacheException
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException,
            ContentLibException, URISyntaxException, CacheException {

        /* first of all validation */
        validateParameters(request);

        /*
         * -------------------------------- get central configuration and --------------------------------
         */
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();
        OutputStream myOutStream = response.getOutputStream();
        setTargetNameAndMimeType(request, response, config);
        Watermark myWatermark = null;
        /* get mets filegroup from request or from configuration */
        String strMetsFileGroup = request.getParameter("metsFileGroup");
        if (strMetsFileGroup == null) {
            strMetsFileGroup = config.getDefaultMetsFileGroup();
        }

        try {
            if (config.getWatermarkUse()) {

                File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
                if (request.getParameterMap().containsKey("watermarkText")) {
                    myWatermark = new Watermark(watermarkfile, request.getParameter("watermarkText"));
                } else {
                    myWatermark = new Watermark(watermarkfile);
                }

            }
            // defining the DocumentPart-List
            LinkedList<DocumentPart> documentparts = new LinkedList<DocumentPart>();

            /* define title page, if configured */
            PDFTitlePage titlepage = new PDFTitlePage();
            if (config.getPdfTitlePageUse()) {
                titlepage.readConfiguration(config.getPdfTitlePageConfigFile());
            }

            /* run through all Documentparts in parameter 'files' */
            StrTokenizer stdocuments = new StrTokenizer(request.getParameter("files"), "$$");
            while (stdocuments.hasNext()) {
                /* get all parameters from each Documentpart */
                StrTokenizer docparams = new StrTokenizer(stdocuments.nextToken(), "$");

                /* create DocumentPart */
                String doctype = docparams.nextToken();
                URL url = new URL(config.getRepositoryPathMets() + docparams.nextToken());
                // URL url2 = new URL("http://gdz.sub.uni-goettingen.de/mets_export.php?PPN=PPN271629789");
                DocumentPart dp;
                if (doctype.equals("pdf")) {
                    dp = new DocumentPart(url, DocumentPartType.PDF);
                } else {
                    dp = new DocumentPart(url, DocumentPartType.METS);
                    dp.setMetsFileGroup(strMetsFileGroup);
                }

                /* possibly a divid */
                if (docparams.size() == 3) {
                    dp.setDivid(docparams.nextToken());
                }

                /* if titlepage should be used, add it now to documentpart */
                if (config.getPdfTitlePageUse()) {
                    dp.setTitlepage(titlepage.clone());
                }

                /* add documentpart to list */
                documentparts.add(dp);
            }

            // create metadata extractor
            SimplePDFMetadataExtractor spme = new SimplePDFMetadataExtractor();
            // create bookmark extractor
            StructureMetadataExtractor bmke = new SimpleStructureMetadataExtractor();
            // create PDF Manager
            PDFCreator pdfcreator = new PDFCreator();

            PDFConfiguration pdfconfig = new PDFConfiguration();
            File iccfile = new File(Util.getBaseFolderAsFile(), "sRGB.icc");
            pdfconfig.setIccinputfilename(iccfile.getAbsolutePath());
            pdfconfig.setPagesize(getPageSize(request));
            pdfconfig.setPdfDefaultAlwaysCompressToJPEG(config.getPdfDefaultAlwaysCompressToJPEG());
            pdfconfig.setPdfDefaultAlwaysUseRenderedImage(config.getPdfDefaultAlwaysUseRenderedImage());
            boolean pdfa = Boolean.parseBoolean(getParameterFromRequestOrConfig("writeAsPdfA", request));
            pdfconfig.setWriteAsPdfA(pdfa);

            pdfcreator.createPDF(myOutStream, documentparts, pdfconfig, spme, bmke, myWatermark);
        } catch (Exception e) {
            LOGGER.error("error while multiple files to pdf generation (" + e.getClass().getName() + ")", e);
            Document pdfdoc = new Document();
            PdfWriter writer;
            try {
                writer = PdfWriter.getInstance(pdfdoc, myOutStream);
            } catch (DocumentException e1) {
                throw new ContentLibException("wrapped DocumentException", e1);
            }
            pdfdoc.open();

            /*---------------------------------
             * Generate Watermark
             *---------------------------------*/
            String errorString = e.getClass().getName() + ": " + e.getMessage();
            String jpgfile = new File(Util.getBaseFolderAsFile(), "errorfile.jpg").getAbsolutePath();
            LOGGER.debug("errorfile to embedd: " + jpgfile);
            FileInputStream inputFileStream = new FileInputStream(jpgfile);
            RenderedImage ri = ServletWatermark.generateErrorWatermark(inputFileStream, errorString).getRenderedImage();

            /*
             * -------------------------------- prepare target and read created image --------------------------------
             */
            BufferedImage buffImage = ImageManipulator.fromRenderedToBuffered(ri);
            Image pdfImage;
            try {
                pdfImage = Image.getInstance(buffImage, null, false);
                pdfdoc.add(pdfImage);
            } catch (BadElementException e1) {
                LOGGER.error("error while adding pdfImage", e);
                throw new ContentLibException("wrapped BadElementException", e1);
            } catch (DocumentException e2) {
                LOGGER.error("error while adding pdfImage", e);
                throw new ContentLibException("wrapped DocumentException", e2);
            }
            /*
             * -------------------------------- close all --------------------------------
             */
            try {
                inputFileStream.close();
                pdfdoc.close();
                writer.close();
            } catch (Error e2) {
                LOGGER.warn("Caught unknown error when closing the pdf writer. Probably due to a broken pipe");
            } finally {
                myOutStream.flush();
                myOutStream.close();
            }
        } finally {
            if (myOutStream != null) {
                myOutStream.flush();
                myOutStream.close();
            }
        }
    }

    /************************************************************************************
     * validate all parameters of request for mets handling, throws IllegalArgumentException if one request parameter is not valid
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @throws IllegalArgumentException
     ************************************************************************************/
    @Override
    public void validateParameters(HttpServletRequest request) throws IllegalArgumentException {
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();

        /* validate repository */
        if (config.getRepositoryPathMets() == null) {
            throw new IllegalArgumentException("no repository url defined");
        }

        /* validate pagesize */
        String strPageSize = request.getParameter("pagesize");
        if (strPageSize != null && !ContentLibUtil.getAllPdfSizesAsList().contains(strPageSize)) {
            throw new IllegalArgumentException("unknown pagesize used; value has be one of: " + ContentLibUtil.getAllPdfSizesAsList().toString());
        }

        /* files has to be not blank */
        if (StringUtils.isBlank(request.getParameter("files"))) {
            throw new IllegalArgumentException("parameter files can not be null or empty");
        } else {
            // TODO: Check if the tokenizer can be moved into contentLib.
            /* run through all Documentparts in parameter files */
            StrTokenizer stk1 = new StrTokenizer(request.getParameter("files"), "$$");
            while (stk1.hasNext()) {
                /* get all parameters from each Documentpart */
                StrTokenizer stk2 = new StrTokenizer(stk1.nextToken(), "$");
                LOGGER.debug("number of parameters: " + stk2.size());
                if (stk2.size() != 2 && stk2.size() != 3) {
                    throw new IllegalArgumentException(
                            "wrong number of parameters, type and url are needed, for mets a third parameter for divid can be used");
                }

                /* check doctype (possible values pdf and mets) as first parameter */
                String doctype = stk2.nextToken();
                LOGGER.debug("document type: " + doctype);
                if (!doctype.equals("pdf") && !doctype.equals("mets")) {
                    throw new IllegalArgumentException("wrong doctype for file (" + doctype + "), possible values are pdf and mets");
                }

                /* get url */
                String url = stk2.nextToken();
                LOGGER.debug("document URL: " + url);

                /* if pdf there is not third parameter required */
                if (doctype.equals("pdf") && stk2.size() == 3) {
                    throw new IllegalArgumentException(
                            "wrong number of parameters, type pdf can only have two parameters for type and url; only mets file can have a third parameter for divid");
                }

                /* if mets there is possibly a divid */
                if (doctype.equals("mets") && stk2.size() == 3) {
                    String divid = stk2.nextToken();
                    LOGGER.debug("document divid: " + divid);
                }
            }

        }
    }
}
