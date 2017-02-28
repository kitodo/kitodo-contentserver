/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 *         - http://gdz.sub.uni-goettingen.de 
 *         - http://www.intranda.com 
 *         - http://www.digiverso.com
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

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibPdfException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.servlet.ServletWatermark;
import de.unigoettingen.sub.commons.contentlib.servlet.Util;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/************************************************************************************
 * Error report action for errors of images and pdf..
 * 
 * @version 23.01.2009 
 * @author Steffen Hankiewicz
 * @author Igor Toker
 ************************************************************************************/
public class GetErrorReportAction extends AbstractGetAction {
    private static final Logger LOGGER = Logger.getLogger(GetErrorReportAction.class);

    /************************************************************************************
     * Create image with error-watermark an send it to output stream of the servlet, after setting correct mime type
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
        super.run(servletContext, request, response);

        /* get central configuration */
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();

        /* get error string */
        String errorString = (String) request.getAttribute("error");

        /*----------------------------------
         * check errorReport
         *----------------------------------*/

        /* jpg image */
        String jpgfile = new File(Util.getBaseFolderAsFile(), "errorfile.jpg").getAbsolutePath();
        LOGGER.debug("errorfile to use: " + jpgfile);

        /*---------------------------------
         * Generate Watermark
         *---------------------------------*/
        FileInputStream inputFileStream = new FileInputStream(jpgfile);
        RenderedImage ri = ServletWatermark.generateErrorWatermark(inputFileStream, errorString).getRenderedImage();

        /*
         * -------------------------------- prepare target and read created image. --------------------------------
         */
        ImageFileFormat targetFormat = ImageFileFormat.PNG;
        ImageInterpreter wi = targetFormat.getInterpreter(ri); // read file
        String targetFileName = "filename=error.png";
        response.setHeader("Content-Disposition", targetFileName.toString());
        response.setContentType(targetFormat.getMimeType());

        wi.setXResolution(config.getDefaultResolution());
        wi.setYResolution(config.getDefaultResolution());

        /*
         * -------------------------------- write target image to stream --------------------------------
         */
        wi.writeToStream(null, response.getOutputStream());
        inputFileStream.close();
    }

}
