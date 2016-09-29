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

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/************************************************************************************
 * getAction class as basis class for Image and Pdf-Actions, defines importand validation checks
 * 
 * @version 06.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/

abstract class AbstractGetAction implements Action {

    /************************************************************************************
     * simple implementation if interface method only forwards to echo.jsp
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws ServletException
     * @throws ContentLibException
     * @throws URISyntaxException
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException,
            ContentLibException {
        validateParameters(request);
    }

    /************************************************************************************
     * validate some important parameters for image and pdf handling
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @throws IllegalArgumentException
     ************************************************************************************/
    @Override
    public void validateParameters(HttpServletRequest request) throws IllegalArgumentException {
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();

        /* validate repository */
        if (config.getRepositoryPathImages() == null) {
            throw new IllegalArgumentException("no repository url for images defined");
        }

    }

}
