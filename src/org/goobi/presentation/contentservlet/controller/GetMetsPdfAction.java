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

import gov.loc.mets.DivType;
import gov.loc.mets.DivType.Mptr;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibPdfException;
import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ContentLibUtil;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFManager;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFManager.PdfPageSize;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFTitlePage;
import de.unigoettingen.sub.commons.contentlib.pdflib.PDFTitlePageLine;
import de.unigoettingen.sub.commons.contentlib.servlet.ServletWatermark;
import de.unigoettingen.sub.commons.contentlib.servlet.Util;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.Action;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.ContentServer;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.simplemets.METSParser;
import de.unigoettingen.sub.commons.simplemets.SimplePDFMetadataExtractor;
import de.unigoettingen.sub.commons.simplemets.SimpleStructureMetadataExtractor;
import de.unigoettingen.sub.commons.util.datasource.Structure;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;

/************************************************************************************
 * pdf action for all kinds of simple pdf handlings first of all validate all request parameters, and than interprete all request parameters for
 * correct image handling
 * 
 * @version 02.01.2009
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class GetMetsPdfAction implements Action {
    private static final Logger LOGGER = Logger.getLogger(GetMetsPdfAction.class);
    private Watermark myWatermark = null;

    /************************************************************************************
     * exectute mets handling and generation of pdf file and send pdf back to output stream of the servlet, after setting correct mime type
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
         * -------------------------------- get central configuration and retrieve source image from url --------------------------------
         */
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();
        if (config.getWatermarkUse()) {
            File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
            myWatermark = Watermark.generateWatermark(request, watermarkfile);
        }
        OutputStream myOutStream = response.getOutputStream();
        ContentCache cc = ContentServer.getPdfCache();
        String myUniqueID = getContentCacheIdForRequest(request, config);
        setTargetNameAndMimeType(request, response, config);
        try {
            /*
             * -------------------------------- ask ContentCache, if object already exists --------------------------------
             */
            PDFManager pdfmanager = null;
            try {
                boolean ignoreCache = false;
                /* check if cache should be ignored */
                if (request.getParameter("ignoreCache") != null) {
                    String ignore = request.getParameter("ignoreCache").trim();
                    ignoreCache = Boolean.parseBoolean(ignore);
                }
                if (cc == null || !config.getPdfCacheUse()) {
                    ignoreCache = true;
                    cc = null;
                    LOGGER.debug("cache deactivated via configuration");
                }

                /* if cache should not be ignored and cache contains file, write it back to stream */
                if (!ignoreCache && cc.cacheContains(myUniqueID, "pdf")) {
                    LOGGER.debug("get file from cache: " + myUniqueID);
                    cc.writeToStream(response.getOutputStream(), myUniqueID, "pdf");
                    return;
                } else if (ignoreCache == false) {
                    LOGGER.debug("file not found in cache: " + myUniqueID);
                }

                /*
                 * -------------------------------- if Cache is not used, parse mets file name and add it to repository path
                 * --------------------------------
                 */
                String metsFile = request.getParameter("metsFile");
                if (!metsFile.endsWith(".xml")) {
                    metsFile += ".xml";
                }
                URL fullMetsPath = new URL(config.getRepositoryPathMets() + metsFile);
                LOGGER.debug("mets file to parse: " + fullMetsPath);

                /*
                 * -------------------------------- open METS file --------------------------------
                 */
                METSParser metsparser = new METSParser(fullMetsPath, true);
                SimplePDFMetadataExtractor spme = new SimplePDFMetadataExtractor();
                spme.activateDFGConfiguration();

                // set the Metadata extractor and the Bookmark metadata extractor
                metsparser.setMetadataextractor(spme);
                metsparser.setStructureMetadataExtractor(new SimpleStructureMetadataExtractor());

                /*
                 * -------------------------------- check if filegroup is defined in request parameter, else take it from config file
                 * --------------------------------
                 */
                String strMetsFileGroup = request.getParameter("metsFileGroup");
                if (strMetsFileGroup == null) {
                    strMetsFileGroup = config.getDefaultMetsFileGroup();
                }
                metsparser.setFilegroupsuseattributevalue(strMetsFileGroup);
                LOGGER.debug("Using METS file group: " + strMetsFileGroup);

                /*
                 * -------------------------------- check divID as parameter, else use upper most divID --------------------------------
                 */
                String divID = request.getParameter("divID");
                DivType pdfdiv = null;
                if (StringUtils.isNotBlank(divID)) {
                    pdfdiv = metsparser.getDIVbyID(divID);
                    // check if the METS file is a monograph, volume or multivolume file
                } else {
                    DivType uplogdiv = metsparser.getUppermostLogicalDiv();
                    if (uplogdiv == null) {
                        throw new ContentLibPdfException("Can't create PDF; div seems to be an anchor.");
                    }

                    // check, if we have <mptr> as children
                    List<Mptr> mptrs = uplogdiv.getMptrList();
                    if ((mptrs == null) || (mptrs.size() == 0)) {
                        // no mptr - must be a monograph
                        // in this case the uppermost logical id is the one we are looking for
                        pdfdiv = uplogdiv;
                    } else {
                        // check, if we have a physical structmap
                        DivType physDiv = metsparser.getUppermostPhysicalDiv();
                        if (physDiv == null) {
                            // it is a multivolume or a periodical or anything like this
                            // in this case the uppermost logical div is the one for which we create the PDF
                            pdfdiv = uplogdiv;
                        } else {
                            // it is the first child div; this represents the volume

                            List<DivType> children = uplogdiv.getDivList();
                            if ((children == null) || (children.size() == 0)) {
                                throw new ContentLibPdfException("Can't create PDF; can't find a div");
                            }
                            pdfdiv = children.get(0); // the first child
                        }
                    }
                }
                spme.calculateMetadata(pdfdiv, metsparser);
                metsparser.getAllFilesForRelatedDivs(pdfdiv.getID()); // get page names

                /*
                 * -------------------------------- get list of files and pagenames --------------------------------
                 */
                Map<Integer, UrlImage> myPages = metsparser.getImageMap();
                // HashMap<Integer, URL> myURLs = metsparser.getPageUrls();
                Map<Integer, String> myNames = metsparser.getPageNames();
                List<? extends Structure> pDFBookmarks = metsparser.getStructureList();
                // PDFManager pdfmanager = new PDFManager(myURLs);
                pdfmanager = new PDFManager(myPages, true);

                setPdfManagerDefaults(request, config, pdfmanager);

                /*
                 * -------------------------------- set pdf meta data --------------------------------
                 */
                if (spme.getPdftitle() != null) {
                    String title = spme.getPdftitle().trim();
                    pdfmanager.setTitle(title);
                }
                if (spme.getPdfcreator() != null) {
                    String creator = spme.getPdfcreator().trim();
                    pdfmanager.setCreator(creator);
                    pdfmanager.setAuthor(creator);
                }
                if (spme.getPdfkeywords() != null) {
                    pdfmanager.setSubject(spme.getPdfkeywords());
                }

                /*
                 * -------------------------------- if configured create PDF title page - either read its path from config file and fill it with mets
                 * content - or if given as url parameter use it directly without any change --------------------------------
                 */
                if (config.getPdfTitlePageUse()) {
                    PDFTitlePage pdftp = new PDFTitlePage();

                    String pdfconfigurl = request.getParameter("pdftitlepage");
                    if (StringUtils.isNotBlank(pdfconfigurl)) {
                        pdftp.readConfiguration(new URI(pdfconfigurl));
                    } else {
                        pdftp.readConfiguration(config.getPdfTitlePageConfigFile());
                        if (spme.getPdfTitlepageLine1() != null) {
                            PDFTitlePageLine ptl = new PDFTitlePageLine(spme.getPdfTitlepageLine1());
                            ptl.setLinetype(2);
                            ptl.setFontsize(14);
                            pdftp.addPDFTitlePageLine(ptl);
                        }

                        if (spme.getPdfTitlepageLine2() != null) {
                            PDFTitlePageLine ptl = new PDFTitlePageLine(spme.getPdfTitlepageLine2());
                            ptl.setLinetype(2);
                            ptl.setFontsize(10);
                            pdftp.addPDFTitlePageLine(ptl);
                        }

                        if (spme.getPdfTitlepageLine3() != null) {
                            PDFTitlePageLine ptl = new PDFTitlePageLine(spme.getPdfTitlepageLine3());
                            ptl.setLinetype(2);
                            ptl.setFontsize(10);
                            pdftp.addPDFTitlePageLine(ptl);
                        }

                        if (spme.getPdfTitlepageLine4() != null) {
                            PDFTitlePageLine ptl = new PDFTitlePageLine(spme.getPdfTitlepageLine4());
                            ptl.setLinetype(2);
                            ptl.setFontsize(10);
                            pdftp.addPDFTitlePageLine(ptl);
                        }
                    }
                    pdfmanager.setPdftitlepage(pdftp);
                }

                /*
                 * -------------------------------- set page names and bookmarks --------------------------------
                 */
                if ((myNames != null) && (myNames.size() > 0)) {
                    pdfmanager.setImageNames(myNames);
                }
                if ((pDFBookmarks != null) && (pDFBookmarks.size() > 0)) {
                    pdfmanager.setStructureList(pDFBookmarks);
                }

                /*
                 * -------------------------------- write pdf to response stream (and cache) --------------------------------
                 */
                /* remove file from cache, if cache should be used and file present */
                if (cc != null) {
                    cc.delete(myUniqueID, "pdf");
                }
                /* if cache size is exceeded write it to response stream only */
                if (cc != null && !cc.isCacheSizeExceeded()) {
                    LOGGER.info("write file to cache and servlet response: " + cc.getFileForId(myUniqueID, "pdf"));
                    myOutStream = new CacheOutputStream(cc.getFileForId(myUniqueID, "pdf"), response.getOutputStream());
                } else if (cc == null) {
                    LOGGER.info("file will not be written to cache, cache is deactivated in configuration");
                } else if (cc.isCacheSizeExceeded()) {
                    LOGGER.info("file will not be written to cache, maximum cache size exceeded defined configuration");
                }
            } catch (NullPointerException e) {
                throw new NullPointerException("Nullpointer occured before pdf-generation");
            }
            /* write to stream */
            if (pdfmanager != null) {
                pdfmanager.createPDF(myOutStream, getPageSize(request), myWatermark);
            }
        } catch (Exception e) {
            LOGGER.error("error during pdf generation (" + e.getClass().getName() + ")", e);
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
            } finally {
                /*
                 * -------------------------------- close all --------------------------------
                 */
                try {
                    if (inputFileStream != null) {
                        inputFileStream.close();
                    }
                    if (pdfdoc != null) {
                        pdfdoc.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                } catch (ExceptionConverter e2) {
                    LOGGER.warn("Caught ExceptionConverter object");
                } finally {

                    /* on errors remove incomplete file from cache */
                    try {
                        if (myOutStream != null) {
                            myOutStream.flush();
                            myOutStream.close();
                        }
                    } catch (Exception e2) {
                        LOGGER.debug("Caught unknown Exception");
                    }
                    if (cc != null && cc.cacheContains(myUniqueID, "pdf")) {
                        cc.delete(myUniqueID, "pdf");
                    }
                }
            }
        } finally {
            if (myOutStream != null) {
                myOutStream.flush();
                myOutStream.close();
            }
        }
    }

    /************************************************************************************
     * set some properties depending on config file
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param config instance of ContentServerConfiguration
     * @param pdfmanager {@link PDFManager} where to set the properties
     * @throws IOException
     ************************************************************************************/
    protected void setPdfManagerDefaults(HttpServletRequest request, ContentServerConfiguration config, PDFManager pdfmanager) throws IOException {

        pdfmanager.setAlwaysUseRenderedImage(config.getPdfDefaultAlwaysUseRenderedImage());
        pdfmanager.setAlwaysCompressToJPEG(config.getPdfDefaultAlwaysCompressToJPEG());

        if (config.getWatermarkUse()) {
            try {
                File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
                if (request.getParameterMap().containsKey("watermarkText")) {
                    myWatermark = new Watermark(watermarkfile, request.getParameter("watermarkText"));
                } else {
                    myWatermark = new Watermark(watermarkfile);
                }
            } catch (WatermarkException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        /*
         * -------------------------------- set ICC profile --------------------------------
         */

        File iccfile = new File(Util.getBaseFolderAsFile(), "sRGB.icc");
        ICC_Profile icc = null;

        if (!iccfile.exists()) {
            InputStream is = GetMetsPdfAction.class.getResourceAsStream("sRGB.icc");
            if (is != null) {
                icc = ICC_Profile.getInstance(is);
            }
        } else {
            icc = ICC_Profile.getInstance(iccfile.getAbsolutePath());
        }

        if (icc != null) {
            pdfmanager.setIccprofile(icc);
        } else {
            LOGGER.error("No ICC profile given!");
        }

        /*
         * -------------------------------- check if pdf should be written as pdf/A, if configured in request parameter, else take it from config file
         * --------------------------------
         */
        boolean pdfa = Boolean.parseBoolean(getParameterFromRequestOrConfig("writeAsPdfA", request));
        pdfmanager.setPdfa(pdfa);
    }

    /************************************************************************************
     * set pagesize of pdf file depending on request parameter or config
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     ************************************************************************************/
    protected PdfPageSize getPageSize(HttpServletRequest request) {
        /*
         * -------------------------------- check if page size is defined in request else take it from config file --------------------------------
         */
        String strPageSize = getParameterFromRequestOrConfig("pagesize", request);

        return PDFManager.getPageSizefromString(strPageSize);
    }

    /************************************************************************************
     * set target file name and mime type for response depending on request and config
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param responst {@link HttpServletResponse} of ServletResponse
     * @param config instance of ContentServerConfiguration
     ************************************************************************************/
    protected void setTargetNameAndMimeType(HttpServletRequest request, HttpServletResponse response, ContentServerConfiguration config) {
        /*
         * -------------------------------- set file name and attachment header from parameter or from configuration --------------------------------
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

        /* if divID is used it can not be empty */
        if (request.getParameter("divID") != null) {
            if (StringUtils.isBlank(request.getParameter("divID"))) {
                throw new IllegalArgumentException("used parameter divID can not be empty");
            }
        }

        /* validate pagesize */
        // TODO: Look a the method getPageSize, this shouldn't be here
        String strPageSize = request.getParameter("pagesize");
        LOGGER.debug("Page size is " + strPageSize);
        if (strPageSize != null && !ContentLibUtil.getAllPdfSizesAsList().contains(strPageSize)) {
            throw new IllegalArgumentException("unknown pagesize used; value has be one of: " + ContentLibUtil.getAllPdfSizesAsList().toString());
        }

        /* metsFile has to be not blank */
        if (StringUtils.isBlank(request.getParameter("metsFile"))) {
            throw new IllegalArgumentException("parameter metsFile can not be null or empty");
        }
        LOGGER.debug("METS file " + request.getParameter("metsFile"));
    }

    /*************************************************************************************
     * generate an ID for a pdf file, to cache it under an unique name
     * 
     * @param request the current {@link HttpServletRequest}
     * @param inConfig current internal {@link ContentServerConfiguration} objekt
     ************************************************************************************/
    private String getContentCacheIdForRequest(HttpServletRequest request, ContentServerConfiguration inConfig) {
        String myId = request.getParameter("metsFile");
        if (request.getParameter("divID") != null) {
            myId += "_" + request.getParameter("divID").trim();
        }

        Boolean useShortFileNames = false;
        try {
            useShortFileNames = inConfig.getContentCacheUseShortFileNames();
        } catch (NoSuchElementException e) {
            LOGGER.warn("Missing configuration for contentCache[@useShortFileNames]", e);
        }

        if (useShortFileNames) {
            myId += "_pdfA" + getParameterFromRequestOrConfig("writeAsPdfA", request);
            myId += "_" + getParameterFromRequestOrConfig("metsFileGroup", request);
            myId += "_" + getParameterFromRequestOrConfig("pdftitlepage", request);
            myId += "_" + getParameterFromRequestOrConfig("pagesize", request);
        }
        return myId;
    }

    /*************************************************************************************
     * get parameter either from request or else the default value from config file
     * 
     * @param inParam the requested parameter
     * @return the value from request or config file
     ************************************************************************************/
    protected String getParameterFromRequestOrConfig(String inParam, HttpServletRequest request) {
        /* take it from request if present */
        String value = request.getParameter(inParam);
        if (StringUtils.isNotBlank(value)) {
            return value.trim();
        } else {
            ContentServerConfiguration config = ContentServerConfiguration.getInstance();
            /* if not in request given, take it from config file */
            if (inParam.equals("writeAsPdfA")) {
                return config.getPdfDefaultWritePdfA().toString();
            }

            if (inParam.equals("metsFileGroup")) {
                return config.getDefaultMetsFileGroup();
            }
            if (inParam.equals("pagesize")) {
                return config.getPdfDefaultPageSize();
            }
        }
        return "";
    }

}
