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
package de.unigoettingen.sub.commons.contentlib.servlet.controller;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibPdfException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ContentLibUtil;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageHolder;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.util.CacheObject;

/************************************************************************************
 * Image action for all kinds of image handlings first of all validate all request parameters, and than interprete all request parameters for correct
 * image handling
 * 
 * @version 05.07.2012
 * @author Steffen Hankiewicz
 * @author Igor Toker
 * @author Andrey Kozhushkov
 * @author Florian Alpers
 ************************************************************************************/
public class GetImageAction extends AbstractGetAction {
    private static final Logger LOGGER = Logger.getLogger(GetImageAction.class);

    /************************************************************************************
     * exectute all image actions (rotation, scaling etc.) and send image back to output stream of the servlet, after setting correct mime type
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws ImageInterpreterException 
     * @throws ServletException
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws ContentLibPdfException 
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException,
            ContentLibException {
        long startTime = System.currentTimeMillis();

        super.run(servletContext, request, response);

        /*
         * -------------------------------- get central configuration --------------------------------
         */
        URI sourceImageUrl = null;
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();
        if (!request.getParameter("sourcepath").startsWith("file:") && !request.getParameter("sourcepath").startsWith("http:")) {
            sourceImageUrl = new URI(config.getRepositoryPathImages() + request.getParameter("sourcepath"));
        } else {
            sourceImageUrl = new URI(request.getParameter("sourcepath"));
        }

        try {
            Cache cc = null;
            ServletOutputStream output = response.getOutputStream();
            if (request.getParameter("thumbnail") != null) {
                cc = ContentServer.getThumbnailCache();
            } else {
                cc = ContentServer.getContentCache();
            }
            // String myUniqueID = getContentCacheIdForRequest(request, config);
            String myUniqueID = getContentCacheIdForParamMap(request.getParameterMap(), config);
            String targetExtension = request.getParameter("format");

            boolean ignoreCache = false;
            /* check if cache should be ignored */
            if (request.getParameter("ignoreCache") != null) {
                String ignore = request.getParameter("ignoreCache").trim();
                ignoreCache = Boolean.parseBoolean(ignore);
            }
            boolean useCache = false;
            if (request.getParameter("thumbnail") != null) {
                useCache = config.getThumbnailCacheUse();
            } else {
                useCache = config.getContentCacheUse();
            }
            if (request.getParameterMap().containsKey("highlight")) {
                useCache = false;
            }
            if (cc == null || !useCache) {
                ignoreCache = true;
                cc = null;
                LOGGER.debug("cache deactivated via configuration");
            }

            if (!ignoreCache && cc.isKeyInCache(myUniqueID + "." + targetExtension)) {
                LOGGER.debug("get file from cache: " + myUniqueID + "." + targetExtension);
                CacheObject co;
                try {
                    co = (CacheObject) cc.get(myUniqueID + "." + targetExtension).getObjectValue();
                    ByteArrayInputStream in = new ByteArrayInputStream(co.getData());

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        output.write(buf, 0, len);
                    }
                    in.close();
                    output.flush();
                    output.close();
                    return;
                } catch (NullPointerException e) {
                    LOGGER.debug("element not in cache anymore: " + myUniqueID + "." + targetExtension);
                }

            } else if (!ignoreCache) {
                LOGGER.debug("file not found in cache: " + myUniqueID + "." + targetExtension);
            }

            // if (!ignoreCache && cc.cacheContains(myUniqueID, targetExtension)) {
            // LOGGER.debug("get file from cache: " + myUniqueID);
            // cc.writeToStream(output, myUniqueID, targetExtension);
            // output.flush();
            // output.close();
            // return;
            // } else if (ignoreCache == false) {
            // LOGGER.debug("file not found in cache: " + myUniqueID);
            // }

            // /*
            // * -------------------------------- if there is an internal request from goobiContentServer, you have to overwrite the
            // sourcepath with
            // * given attribute for image url --------------------------------
            // */
            // if (request.getAttribute("sourcepath") != null) {
            // sourceImageUrl = new URI((String) request.getAttribute("sourcepath"));
            // }
            LOGGER.debug("source image:" + sourceImageUrl);

            /*
             * -------------------------------- retrieve source image from url --------------------------------
             */
            ImageManager sourcemanager = new ImageManager(sourceImageUrl.toURL());
            LOGGER.trace("imageManager initialized");

            /*
             * -------------------------------- set the defaults --------------------------------
             */
            int angle = 0;
            int scaleX = 100;
            int scaleY = 100;
            int scaleType = ImageManager.SCALE_BY_PERCENT;
            LinkedList<String> highlightCoordinateList = null;
            Color highlightColor = null;
            Watermark myWatermark = null;
            LOGGER.trace("Variables set");

            /*
             * -------------------------------- rotate --------------------------------
             */
            if (request.getParameterMap().containsKey("rotate")) {
                angle = Integer.parseInt(request.getParameter("rotate"));
                LOGGER.trace("rotate image:" + angle);
            }

            /*
             * -------------------------------- scale: scale the image to some percent value --------------------------------
             */
            if (request.getParameterMap().containsKey("scale")) {
                scaleX = Integer.parseInt(request.getParameter("scale"));
                scaleY = scaleX;
                scaleType = ImageManager.SCALE_BY_PERCENT;
                LOGGER.trace("scale image to percent:" + scaleX);
            }

            if (request.getParameterMap().containsKey("width") && request.getParameterMap().containsKey("height")) {
                scaleX = Integer.parseInt(request.getParameter("width"));
                scaleY = Integer.parseInt(request.getParameter("height"));
                scaleType = ImageManager.SCALE_TO_BOX;
            }

            /*
             * -------------------------------- width: scale image to fixed width --------------------------------
             */
            else if (request.getParameterMap().containsKey("width")) {
                scaleX = Integer.parseInt(request.getParameter("width"));
                scaleY = 0;
                scaleType = ImageManager.SCALE_BY_WIDTH;
                LOGGER.trace("scale image to width:" + scaleX);
            }

            /*
             * -------------------------------- height: scale image to fixed height --------------------------------
             */
            else if (request.getParameterMap().containsKey("height")) {
                scaleY = Integer.parseInt(request.getParameter("height"));
                scaleX = 0;
                scaleType = ImageManager.SCALE_BY_HEIGHT;
                LOGGER.trace("scale image to height:" + scaleY);
            }

            /*
             * -------------------------------- highlight --------------------------------
             */
            if (request.getParameterMap().containsKey("highlight")) {
                highlightCoordinateList = new LinkedList<String>();
                String highlight = request.getParameter("highlight");
                StrTokenizer areas = new StrTokenizer(highlight, "$");
                for (String area : areas.getTokenArray()) {
                    StrTokenizer coordinates = new StrTokenizer(area, ",");
                    highlightCoordinateList.add(coordinates.getContent());
                }
                highlightColor = config.getDefaultHighlightColor();
            }

            /*
             * -------------------------------- insert watermark, if it should be used --------------------------------
             */
            if (!request.getParameterMap().containsKey("ignoreWatermark") && config.getWatermarkUse()) {
                File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
                myWatermark = Watermark.generateWatermark(request, watermarkfile);
            }

            /*
             * -------------------------------- prepare target --------------------------------
             */
            // change to true if watermark should scale
            RenderedImage targetImage = null;
            if (config.getScaleWatermark()) {
                targetImage =
                        sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, true,
                                ImageManager.BOTTOM);
            } else {
                targetImage =
                        sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark,
                                false, ImageManager.BOTTOM);
            }
            LOGGER.trace("Creating ImageInterpreter");
            ImageFileFormat targetFormat = ImageFileFormat.getImageFileFormatFromFileExtension(targetExtension);
            ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read file
            LOGGER.trace("Image stored in " + wi.getClass().toString());
            /*
             * -------------------------------- set file name and attachment header from parameter or from configuration
             * --------------------------------
             */
            LOGGER.trace("Creating target file");
            StringBuilder targetFileName = new StringBuilder();
            if (config.getSendImageAsAttachment()) {
                targetFileName.append("attachment; ");
            }
            targetFileName.append("filename=");

            if (request.getParameter("targetFileName") != null) {
                targetFileName.append(request.getParameter("targetFileName"));
            } else {
                String filename = ContentLibUtil.getCustomizedFileName(config.getDefaultFileNameImages(), "." + targetFormat.getFileExtension());
                targetFileName.append(filename);
            }
            LOGGER.trace("Adding targetFile " + targetFileName.toString() + " to response");
            response.setHeader("Content-Disposition", targetFileName.toString());
            response.setContentType(targetFormat.getMimeType());

            /*
             * -------------------------------- resolution --------------------------------
             */
            LOGGER.trace("Setting image resolution values");
            if (request.getParameter("resolution") != null) {
                wi.setXResolution(Float.parseFloat(request.getParameter("resolution")));
                wi.setYResolution(Float.parseFloat(request.getParameter("resolution")));
            } else {
                wi.setXResolution(config.getDefaultResolution());
                wi.setYResolution(config.getDefaultResolution());
            }

            LOGGER.trace("Setting image compression");
            if (request.getParameter("compression") != null) {
                String value = request.getParameter("compression");
                try {
                    int intvalue = Integer.parseInt(value);
                    wi.setWriterCompressionValue(intvalue);
                } catch (Exception e) {
                    LOGGER.trace("value is not a number, use default value");
                }
            }

            /*
             * -------------------------------- write target image to stream --------------------------------
             */
            // cc.put(new Element(myUniqueID + "." + targetExtension, wi.getRenderedImage()));

            if (cc != null) {
                byte[] data = wi.writeToStreamAndByteArray(output);
                cc.putIfAbsent(new Element(myUniqueID + "." + targetExtension, new CacheObject(data)));
            } else {
                LOGGER.trace("writing file to servlet response");
                wi.writeToStream(null, output);
            }
            LOGGER.trace("Done writing ImageInterpreter to stream");
            wi.clear();
            LOGGER.trace("Done clearing ImageInterpreter");
        } catch (Exception e) {
            LOGGER.error("CacheException", e);
        }
        long endTime = System.currentTimeMillis();
        LOGGER.trace("Content server time to process request: " + (endTime - startTime) + " ms");
        // try {
        // CommonUtils.appendTextFile("Image request for file " + new File(sourceImageUrl).getAbsolutePath() + "; Time to process: " + (endTime -
        // startTime)
        // + " ms\n", new File(de.unigoettingen.sub.commons.contentlib.servlet.Util.getBaseFolderAsFile(), "timeConsumptionLog.txt"));
        //
        // } catch (IOException e) {
        // LOGGER.info("Unable to write time log file due to IOException " + e.toString());
        // }
    }

    /************************************************************************************
     * validate all parameters of request for image handling, throws IllegalArgumentException if one request parameter is not valid
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @throws IllegalArgumentException
     ************************************************************************************/
    @Override
    public void validateParameters(HttpServletRequest request) throws IllegalArgumentException {

        /* call super.validation for main parameters of image and pdf actions */
        super.validateParameters(request);

        /* validate path of source image */
        if (request.getParameter("sourcepath") == null && request.getAttribute("sourcepath") == null) {
            throw new IllegalArgumentException("no source path defined (sourcepath)");
        }

        /* validate rotation agle is a number */
        if (request.getParameterMap().containsKey("rotate") && !StringUtils.isNumeric(request.getParameter("rotate"))) {
            throw new IllegalArgumentException("rotation angle is not numeric");
        }

        /* validate percent scaling is a number */
        if (request.getParameterMap().containsKey("scale") && !StringUtils.isNumeric(request.getParameter("scale"))) {
            throw new IllegalArgumentException("scale is not numeric");
        }

        /* validate width scaling is a number */
        if (request.getParameterMap().containsKey("width") && !StringUtils.isNumeric(request.getParameter("width"))) {
            throw new IllegalArgumentException("width is not numeric");
        }

        /* validate height scaling is a number */
        if (request.getParameterMap().containsKey("height") && !StringUtils.isNumeric(request.getParameter("height"))) {
            throw new IllegalArgumentException("height is not numeric");
        }

        /* validate resolution */
        if (request.getParameter("resolution") != null && !StringUtils.isNumeric(request.getParameter("resolution"))) {
            throw new IllegalArgumentException("resolution is not numeric");
        }

        /* validate highlighting coordinates */
        if (request.getParameter("highlight") != null) {
            String highlight = request.getParameter("highlight");
            StrTokenizer areas = new StrTokenizer(highlight, "$");
            for (String area : areas.getTokenArray()) {
                /* currently only 4 coordinates are supported per area */
                StrTokenizer coordinates = new StrTokenizer(area, ",");
                if (coordinates.getTokenArray().length != 4) {
                    throw new IllegalArgumentException("highlight area does not have 4 coordinates: " + coordinates.getContent());
                }

                /* only numbers are permitted */
                for (String coordinate : coordinates.getTokenArray()) {
                    if (!StringUtils.isNumeric(coordinate)) {
                        throw new IllegalArgumentException("highlight coordinate is not numeric: " + coordinates.getContent());
                    }
                }

            } // end for (String area ...
        } // end if (request.getParameter("highlight") ...

    } // end method

    // /*************************************************************************************
    // * generate an ID for a pdf file, to cache it under an unique name
    // *
    // * @param request
    // * the current {@link HttpServletRequest}
    // * @param inConfig
    // * current internal {@link ContentServerConfiguration} objekt
    // ************************************************************************************/
    // @SuppressWarnings("unused")
    // private String getContentCacheIdForRequest(HttpServletRequest request, ContentServerConfiguration inConfig) {
    // String myId = "";
    // Map<String, String[]> myIdMap = request.getParameterMap();
    // for (String s : myIdMap.keySet()) {
    // if (!s.equalsIgnoreCase("ignoreCache")) {
    // myId += s;
    // String[] v = myIdMap.get(s);
    // for (String val : v) {
    // myId += val;
    // }
    // }
    // }
    //
    // try {
    // byte[] defaultBytes = myId.getBytes("UTF-8");
    // MessageDigest algorithm = MessageDigest.getInstance("MD5");
    // algorithm.reset();
    // algorithm.update(defaultBytes);
    // byte messageDigest[] = algorithm.digest();
    //
    // StringBuffer hexString = new StringBuffer();
    // for (int i = 0; i < messageDigest.length; i++) {
    // hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
    // }
    //
    // // System.out.println("sessionid " + myId + " md5 version is " +
    // // hexString.toString());
    // myId = hexString + "";
    //
    // } catch (NoSuchAlgorithmException nsae) {
    //
    // } catch (UnsupportedEncodingException e) {
    //
    // }
    //
    // return myId;
    // }

    /**
     * 
     * @param params
     * @param inConfig
     * @return
     */
    protected String getContentCacheIdForParamMap(Map<String, String[]> params, ContentServerConfiguration inConfig) {
        StringBuffer cacheId = new StringBuffer();

        String pi = null;
        String imageNo = "-";
        String width = "-";
        String height = "-";
        String rotate = "-";
        String scale = "-";
        String watermarkText = null;
        for (String s : params.keySet()) {
            String[] values = params.get(s);
            LOGGER.trace("param: " + s);
            if (s.equals("sourcepath")) {
                String[] sourcePathSplit = values[0].split("[/]");
                if (sourcePathSplit.length >= 2) {
                    pi = sourcePathSplit[sourcePathSplit.length - 2];
                    imageNo = sourcePathSplit[sourcePathSplit.length - 1];
                } else {
                    // if we are not in a subfolder
                    imageNo = sourcePathSplit[sourcePathSplit.length - 1];
                }
                if (StringUtils.isNotEmpty(imageNo)) {
                    imageNo = imageNo.substring(0, imageNo.indexOf('.'));
                }
            } else if (s.equals("width")) {
                width = values[0];
            } else if (s.equals("height")) {
                height = values[0];
            } else if (s.equals("rotate")) {
                rotate = values[0];
            } else if (s.equals("scale")) {
                scale = values[0];
            } else if (s.equals("watermarkText")) {
                watermarkText = values[0];
            }
        }

        cacheId.append(pi);
        cacheId.append("_");
        cacheId.append(imageNo);
        cacheId.append("_");
        cacheId.append(width);
        cacheId.append("_");
        cacheId.append(height);
        cacheId.append("_");
        cacheId.append(rotate);
        cacheId.append("_");
        cacheId.append(scale);
        if (watermarkText != null) {
            try {
                cacheId.append("_" + URLEncoder.encode(watermarkText, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return cacheId.toString();
    }

    public byte[] getImage(Map<String, String[]> params) throws URISyntaxException, IOException, ImageManagerException {
        return getImageHolder(params).getImage();
    }

    public ImageHolder getImageHolder(Map<String, String[]> params) throws URISyntaxException, IOException, ImageManagerException {

        /*
         * -------------------------------- get central configuration --------------------------------
         */
        URI sourceImageUrl = null;
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();
        if (!params.get("sourcepath")[0].startsWith("file:") && !params.get("sourcepath")[0].startsWith("http:")) {
            String path = params.get("sourcepath")[0].replace(" ", "+");
            sourceImageUrl = new URI(config.getRepositoryPathImages() + path);
        } else {
            String path = params.get("sourcepath")[0].replace(" ", "+");
            sourceImageUrl = new URI(path);
        }

        try {
            Cache cc = null;
            // ServletOutputStream output = response.getOutputStream();
            if (params.get("thumbnail") != null) {
                cc = ContentServer.getThumbnailCache();
            } else {
                cc = ContentServer.getContentCache();
            }
            String myUniqueID = getContentCacheIdForParamMap(params, config);
            LOGGER.trace("myUniqueId: " + myUniqueID);
            String targetExtension = params.get("format")[0];

            boolean ignoreCache = false;
            /* check if cache should be ignored */
            if (params.get("ignoreCache") != null) {
                String ignore = params.get("ignoreCache")[0].trim();
                ignoreCache = Boolean.parseBoolean(ignore);
            }
            boolean useCache = false;
            if (params.get("thumbnail") != null) {
                useCache = config.getThumbnailCacheUse();
            } else {
                useCache = config.getContentCacheUse();
            }

            if (params.get("highlight") != null) {
                useCache = false;
            }
            if (cc == null || !useCache || params.get("highlight") != null) {
                ignoreCache = true;
                cc = null;
                LOGGER.debug("cache deactivated via configuration");
            }

            // Image found in cache
            if (!ignoreCache && cc.isKeyInCache(myUniqueID + "." + targetExtension)) {
                LOGGER.debug("get file from cache: " + myUniqueID + "." + targetExtension);
                try {
                    CacheObject co = (CacheObject) cc.get(myUniqueID + "." + targetExtension).getValue();
                    return new ImageHolder(co.getData());

                } catch (NullPointerException e) {
                    LOGGER.debug("element not in cache anymore: " + myUniqueID + "." + targetExtension);
                }
                // if (!ignoreCache && cc.isKeyInCache(myUniqueID + "." + targetExtension)) {
                // LOGGER.debug("get file from cache: " + myUniqueID);
                //
                // RenderedImage ri = (RenderedImage) cc.get(myUniqueID + "." + targetExtension).getObjectValue();
                // JpegInterpreter ii = new JpegInterpreter(ri);
                // ii.writeToStream(null, baos);
                // return baos.toByteArray();
            } else if (!ignoreCache) {
                LOGGER.debug("file not found in cache: " + myUniqueID + "." + targetExtension);
            }

            /*
             * -------------------------------- retrieve source image from url --------------------------------
             */
            ImageManager sourcemanager = new ImageManager(sourceImageUrl.toURL());
            LOGGER.trace("imageManager initialized");

            /*
             * -------------------------------- set the defaults --------------------------------
             */
            int angle = 0;
            int scaleX = 100;
            int scaleY = 100;
            int scaleType = ImageManager.SCALE_BY_PERCENT;
            LinkedList<String> highlightCoordinateList = null;
            Color highlightColor = null;
            Watermark myWatermark = null;
            LOGGER.trace("Variables set");

            /*
             * -------------------------------- rotate --------------------------------
             */
            if (params.get("rotate") != null) {
                angle = Integer.parseInt(params.get("rotate")[0]);
                LOGGER.trace("rotate image:" + angle);
            }

            /*
             * -------------------------------- scale: scale the image to some percent value --------------------------------
             */
            if (params.get("scale") != null) {
                scaleX = Integer.parseInt(params.get("scale")[0]);
                scaleY = scaleX;
                scaleType = ImageManager.SCALE_BY_PERCENT;
                LOGGER.trace("scale image to percent:" + scaleX);
            }

            if (params.get("width") != null && params.get("height") != null) {
                scaleX = Integer.parseInt(params.get("width")[0]);
                scaleY = Integer.parseInt(params.get("height")[0]);
                scaleType = ImageManager.SCALE_TO_BOX;
            }

            /*
             * -------------------------------- width: scale image to fixed width --------------------------------
             */
            else if (params.get("width") != null) {
                scaleX = Integer.parseInt(params.get("width")[0]);
                scaleY = 0;
                scaleType = ImageManager.SCALE_BY_WIDTH;
                LOGGER.trace("scale image to width:" + scaleX);
            }

            /*
             * -------------------------------- height: scale image to fixed height --------------------------------
             */
            else if (params.get("height") != null) {
                scaleY = Integer.parseInt(params.get("height")[0]);
                scaleX = 0;
                scaleType = ImageManager.SCALE_BY_HEIGHT;
                LOGGER.trace("scale image to height:" + scaleY);
            }

            /*
             * -------------------------------- highlight --------------------------------
             */
            if (params.get("highlight") != null) {
                highlightCoordinateList = new LinkedList<String>();
                String highlight = params.get("highlight")[0];
                StrTokenizer areas = new StrTokenizer(highlight, "$");
                for (String area : areas.getTokenArray()) {
                    StrTokenizer coordinates = new StrTokenizer(area, ",");
                    highlightCoordinateList.add(coordinates.getContent());
                }
                highlightColor = config.getDefaultHighlightColor();
            }

            /*
             * -------------------------------- insert watermark, if it should be used --------------------------------
             */
            if (params.get("ignoreWatermark") == null) {
                if (config.getWatermarkUse()) {
                    File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
                    myWatermark = Watermark.generateWatermark(params, watermarkfile);
                }
            }

            /*
             * -------------------------------- prepare target --------------------------------
             */
            // change to true if watermark should scale
            boolean scaleWatermark = false;
            if (config.getScaleWatermark()) {
                scaleWatermark = true;
            }
            RenderedImage targetImage =
                    sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark,
                            scaleWatermark, ImageManager.BOTTOM);
            LOGGER.trace("Creating ImageInterpreter");
            ImageFileFormat targetFormat = ImageFileFormat.getImageFileFormatFromFileExtension(targetExtension);
            ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read file
            LOGGER.trace("Image stored in " + wi.getClass().getCanonicalName());

            /*
             * -------------------------------- set file name and attachment header from parameter or from configuration
             * --------------------------------
             */
            // StringBuilder targetFileName = new StringBuilder();
            // if (config.getSendImageAsAttachment()) {
            // targetFileName.append("attachment; ");
            // }
            // targetFileName.append("filename=");
            //
            // if (params.get("targetFileName") != null) {
            // targetFileName.append(params.get("targetFileName"));
            // } else {
            // String filename = ContentLibUtil.getCustomizedFileName(config.getDefaultFileNameImages(), "." +
            // targetFormat.getFileExtension());
            // targetFileName.append(filename);
            // }

            /*
             * -------------------------------- resolution --------------------------------
             */
            LOGGER.trace("Setting image resolution");
            if (params.get("resolution") != null) {
                wi.setXResolution(Float.parseFloat(params.get("resolution")[0]));
                wi.setYResolution(Float.parseFloat(params.get("resolution")[0])); // TODO Is this correct?
            } else {
                wi.setXResolution(config.getDefaultResolution());
                wi.setYResolution(config.getDefaultResolution());
            }
            LOGGER.trace("Finished setting image resolution");
            /*
             * -------------------------------- write target image to stream --------------------------------
             */

            byte[] data = wi.writeToStreamAndByteArray(new ByteArrayOutputStream());
            ImageHolder returnImage = new ImageHolder(data, wi.getWidth(), wi.getHeight());
            if (cc != null && highlightColor == null) {
                cc.putIfAbsent(new Element(myUniqueID + "." + targetExtension, new CacheObject(data)));
            }
            LOGGER.trace("Done writing image to stream");
            return returnImage;
        } catch (CacheException e) {
            LOGGER.error("CacheException", e);
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (ImageManipulatorException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (WatermarkException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }
}
