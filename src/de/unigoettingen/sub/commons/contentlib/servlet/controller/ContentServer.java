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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.log4j.Logger;
import org.goobi.presentation.contentservlet.controller.ContentCache;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.servlet.Util;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/************************************************************************************
 * simple contentserver class for requesting images
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class ContentServer extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ContentServer.class);
    protected Map<String, Class<? extends Action>> actions = null;
    // private static ContentCache cc;
    // private static ContentCache thumbnailcache;
    private static ContentCache pdfCache;
    private static CacheManager cacheManager;
    private static final long serialVersionUID = 1L;

    /************************************************************************************
     * default constructor for initialization
     ************************************************************************************/
    @Override
    public void init() throws ServletException {
        super.init();
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();

        try {
            /* initialize ContentCache only, if set in configuration */
            // if (config.getContentCacheUse()) {
            // cc = new ContentCache(config.getContentCachePath(), config.getContentCacheSize());
            // // Cache cc = new Cache("testCache", 5000, false, false, 5, 2);
            //
            //
            // }
            // if (config.getThumbnailCacheUse()) {
            // thumbnailcache = new ContentCache(config.getThumbnailCachePath(), config.getThumbnailCacheSize());
            // }
            if (config.getPdfCacheUse()) {
                pdfCache = new ContentCache(config.getPdfCachePath(), config.getPdfCacheSize());
            }
        } catch (CacheException e) {
            throw new ServletException("ContentCache for GoobiContentServer can not be initialized", e);
        }
        actions = new HashMap<String, Class<? extends Action>>();
        actions.put("image", GetImageAction.class);
        actions.put("pdf", GetPdfAction.class);
        actions.put("cachecheck", CacheCheckAction.class);
    }

    @Override
    public void destroy() {
        super.destroy();

        try {
            getContentCache().flush();
            getThumbnailCache().flush();
        } catch (IllegalStateException e) {
            LOGGER.error(e);
        } catch (net.sf.ehcache.CacheException e) {
            LOGGER.error(e);
        } catch (CacheException e) {
            LOGGER.error(e);
        }
        getManager().shutdown();
    }

    /************************************************************************************
     * get method for executing contentserver requests
     ************************************************************************************/
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.debug("Contentserver start");

        /*
         * -------------------------------- check action-Parameter if empty execute echo-action --------------------------------
         */
        String actionString = request.getParameter("action");
        if (actionString == null || actionString.equals("")) {
            actionString = "echo";
        } else {
            actionString = actionString.toLowerCase().trim();
        }
        LOGGER.debug("actionString is:" + actionString);

        /*-------------------------------- 
         * prepare appropriate action method
         * --------------------------------*/

        Action action = null;
        LOGGER.debug("Implementation class for action " + actionString + " is " + this.actions.get(actionString).getName());
        try {
            action = this.actions.get(actionString).newInstance();
        } catch (InstantiationException e1) {
            LOGGER.error("Can't intantiate Action class for " + actionString, e1);
        } catch (IllegalAccessException e1) {
            LOGGER.error("Illegal Access to Action class for " + actionString, e1);
        }
        /*
         * if (actionString.equals("image")) { action = new GetImageAction(); } else if (actionString.equals("pdf")) { action = new GetPdfAction(); }
         * else { action = new JspOnlyAction(actionString); }
         */
        if (action == null) {
            action = new JspOnlyAction(actionString);
        }
        LOGGER.debug("action is:" + action.getClass().getName());

        /*-------------------------------- 
         * execute action method
         * --------------------------------*/
        try {
            /* run the action */
            action.run(getServletContext(), request, response);
        } catch (Exception e) {
            /* if an error occurs log stacktrace and forward error message */
            LOGGER.error("An error occured", e);
            /*
             * depending on error reporting parameter show jsp oder image for errors
             */
            Action errorAction = new JspOnlyAction("echo");
            if (request.getParameterMap().containsKey("errorReport") && request.getParameter("errorReport").equals("image")) {
                errorAction = new GetErrorReportAction();
            }
            request.setAttribute("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            try {
                /* execute error report action */
                errorAction.run(getServletContext(), request, response);
            } catch (Exception e2) {
                LOGGER.error("An error occured", e2);
            }
        }
        LOGGER.debug("Contentserver end");
    }

    /************************************************************************************
     * post-method for contentserver requests, simply forwards request to get method
     ************************************************************************************/
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    // /*************************************************************************************
    // * Getter for ContentCache
    // *
    // * @return the cc
    // * @throws CacheException
    // *************************************************************************************/
    // public static ContentCache getContentCache() throws CacheException {
    // if (cc == null && ContentServerConfiguration.getInstance().getContentCacheUse()) {
    // cc = new ContentCache(ContentServerConfiguration.getInstance().getContentCachePath(), ContentServerConfiguration.getInstance()
    // .getContentCacheSize());
    // }
    // return cc;
    // }
    //
    // /**
    // * @return the thumbnailcache
    // * @throws CacheException
    // */
    // public static ContentCache getThumbnailCache() throws CacheException {
    // if (thumbnailcache == null && ContentServerConfiguration.getInstance().getThumbnailCacheUse()) {
    // thumbnailcache = new ContentCache(ContentServerConfiguration.getInstance().getThumbnailCachePath(), ContentServerConfiguration
    // .getInstance().getThumbnailCacheSize());
    // }
    // return thumbnailcache;
    // }
    //
    /**
     * 
     * @return
     * @throws CacheException
     */
    public static ContentCache getPdfCache() throws CacheException {
        if (pdfCache == null && ContentServerConfiguration.getInstance().getPdfCacheUse()) {
            pdfCache =
                    new ContentCache(ContentServerConfiguration.getInstance().getPdfCachePath(), ContentServerConfiguration.getInstance()
                            .getPdfCacheSize());
        }
        return pdfCache;
    }

    /*************************************************************************************
     * Getter for ContentCache
     * 
     * @return the cc
     * @throws CacheException
     *************************************************************************************/
    public static Cache getContentCache() throws CacheException {
        return getManager().getCache("content");
    }

    /**
     * @return the thumbnailcache
     * @throws CacheException
     */
    public static Cache getThumbnailCache() throws CacheException {
        return getManager().getCache("thumbnails");
    }

    private static CacheManager getManager() {
        if (cacheManager == null) {
            File file = new File(Util.getBaseFolderAsFile(), "ehcache.xml");
            // cacheManager = new CacheManager(file.getAbsolutePath());
            cacheManager = CacheManager.create(file.getAbsolutePath());
        }
        return cacheManager;
    }

    // /**
    // *
    // * @return
    // * @throws CacheException
    // */
    // public static Cache getPdfCache() throws CacheException {
    // return cacheManager.getCache("pdf");
    // }

}
