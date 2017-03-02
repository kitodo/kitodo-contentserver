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
package org.goobi.presentation.contentservlet.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.Action;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetImageAction;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.simplemets.METSParser;
import de.unigoettingen.sub.commons.simplemets.exceptions.MetsException;

/************************************************************************************
 * mets image action for all kinds of image handlings for some image file from a mets file with given id and given filegroup, first of all validate
 * all request parameters, and than interprete all request parameters for correct image handling
 * 
 * @version 20.03.2009
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class GetMetsImageAction implements Action {
    private static final Logger LOGGER = Logger.getLogger(GetMetsImageAction.class);

    /************************************************************************************
     * exectute mets handling and requesting image with given id from mets file for further image processing
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws URISyntaxException
     * @throws ContentLibException
     * @throws ServletException
     * @throws MetsException 
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws URISyntaxException, IOException,
            ServletException, ContentLibException, MetsException {
        /* first of all validation */
        validateParameters(request);

        /*
         * -------------------------------- get central configuration and retrieve source image from url --------------------------------
         */
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();

        /*
         * -------------------------------- parse mets file and get url from requested image div id --------------------------------
         */
        String metsFile = request.getParameter("metsFile");
        if (!metsFile.endsWith(".xml")) {
            metsFile += ".xml";
        }
        String pageid = request.getParameter("divID");
        URL fullMetsPath = new URL(config.getRepositoryPathMets() + metsFile);
        LOGGER.debug("mets file to parse: " + fullMetsPath);

        /*
         * -------------------------------- open METS file and check if filegroup is defined in request parameter, else take it from config file
         * --------------------------------
         */
        METSParser metsparser = new METSParser(fullMetsPath, true);
        String strMetsFileGroup = request.getParameter("metsFileGroup");
        if (strMetsFileGroup == null) {
            metsparser.setFilegroupsuseattributevalue(config.getDefaultMetsFileGroup());
        } else {
            metsparser.setFilegroupsuseattributevalue(strMetsFileGroup);
        }

        /*
         * -------------------------------- get url for image --------------------------------
         */
        URL fileUrl = metsparser.getURLForSingleDiv(pageid);
        LOGGER.debug("requested image url " + fileUrl.toString());

        /*
         * -------------------------------- forward request to image handling action --------------------------------
         */
        request.setAttribute("sourcepath", fileUrl.toString());
        GetImageAction imageAction = new GetImageAction();
        imageAction.run(servletContext, request, response);
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

        /* divID has to be not blank */
        if (StringUtils.isBlank(request.getParameter("divID"))) {
            throw new IllegalArgumentException("parameter divID can not be null or empty");
        }

        /* metsFile has to be not blank */
        if (StringUtils.isBlank(request.getParameter("metsFile"))) {
            throw new IllegalArgumentException("parameter metsFile can not be null or empty");
        }
    }
}
