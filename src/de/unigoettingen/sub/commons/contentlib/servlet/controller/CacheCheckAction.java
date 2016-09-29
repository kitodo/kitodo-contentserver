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

import net.sf.ehcache.Cache;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/************************************************************************************
 * Image action for all kinds of image handlings first of all validate all request parameters, and than interprete all request parameters for correct
 * image handling
 * 
 * @version 17.03.2012
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class CacheCheckAction extends GetImageAction {
    private static final Logger LOGGER = Logger.getLogger(CacheCheckAction.class);

    /************************************************************************************
     * check if given image is in cache
     * 
     * @param request {@link HttpServletRequest} of ServletRequest
     * @param response {@link HttpServletResponse} for writing to response output stream
     * @throws IOException
     * @throws ServletException
     * @throws ContentLibException
     * @throws URISyntaxException
     ************************************************************************************/
    @Override
    public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException {

        boolean isInCache = false;

        ContentServerConfiguration config = ContentServerConfiguration.getInstance();
        try {

            Cache cc = null;
            response.getOutputStream();
            if (request.getParameter("thumbnail") != null) {

                cc = ContentServer.getThumbnailCache();

            } else {
                cc = ContentServer.getContentCache();
            }

            String myUniqueID = getContentCacheIdForParamMap(request.getParameterMap(), config);
            String targetExtension = request.getParameter("format");

            if (cc.isKeyInCache(myUniqueID + "." + targetExtension)) {
                LOGGER.debug("get file from cache: " + myUniqueID + "." + targetExtension);
                try {
                    cc.get(myUniqueID + "." + targetExtension).getObjectValue();
                    isInCache = true;
                } catch (NullPointerException e) {
                    LOGGER.debug("element not in cache anymore: " + myUniqueID + "." + targetExtension);
                }
            }
        } catch (CacheException e) {
            LOGGER.error("Cache error", e);
        }
        LOGGER.debug("image in cache: " + isInCache);
        if (!isInCache) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "not in cache");
        }
    }

}
