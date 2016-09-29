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
import java.util.Arrays;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;

/************************************************************************************
 * JspOnly action for all requests of jsp files
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class JspOnlyAction implements Action {
    private static final Logger LOGGER = Logger.getLogger(JspOnlyAction.class);
    private String url;

    /************************************************************************************
     * Default Constructor sets the jsp url to echo
     ************************************************************************************/
    public JspOnlyAction() {
        this.url = "/" + "echo.jsp";
    }

    /************************************************************************************
     * Constructor for setting the action string directly
     * 
     * @param actionString {@link String} Name of jsp file (without file extension and path)
     ************************************************************************************/
    public JspOnlyAction(String actionString) {
        this.url = "/" + actionString + ".jsp";
    }

    /************************************************************************************
     * Setter for the url of the jsp file
     * 
     * @param url {@link String} Name of jsp file (without file extension and path)
     ************************************************************************************/
    public void setUrl(String url) {
        this.url = url;
    }

    /************************************************************************************
     * Action to forward response to requested jsp file
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws ServletException
     * @throws ContentLibException
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        LOGGER.debug("jspOnlyAction: " + url);
        // forward request to jsp
        RequestDispatcher dispatcher = servletContext.getRequestDispatcher(url);
        dispatcher.forward(request, response);
    }

    /************************************************************************************
     * validate correct url for jsp file
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @throws IllegalArgumentException
     ************************************************************************************/
    @Override
    public void validateParameters(HttpServletRequest request) throws IllegalArgumentException {
        /* validate if url is valid */
        String[] jspfiles = new String[] { "echo", "help", "about" };
        if (!Arrays.asList(jspfiles).contains(url)) {
            throw new IllegalArgumentException("requested file url unknown");
        }
    }
}
