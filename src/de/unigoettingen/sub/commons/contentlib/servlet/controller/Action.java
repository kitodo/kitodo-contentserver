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

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.simplemets.exceptions.MetsException;

/************************************************************************************
 * interface for all current and future actions of the image servlet currently there are only two important methods to implement: run and
 * validateParameters
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public interface Action {

    /************************************************************************************
     * exectute execute the Action method and write response to response stream
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws ServletException
     * @throws ContentLibException
     * @throws URISyntaxException
     ************************************************************************************/
    abstract void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException,
            URISyntaxException, ServletException, ContentLibException, MetsException, CacheException;

    /************************************************************************************
     * validate all parameters of request, throws exception if one request parameter is not valid
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @throws IllegalArgumentException
     ************************************************************************************/
    abstract void validateParameters(HttpServletRequest request) throws IllegalArgumentException;

}
