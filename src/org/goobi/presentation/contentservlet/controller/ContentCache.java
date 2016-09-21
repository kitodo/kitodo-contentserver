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
package org.goobi.presentation.contentservlet.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;

/************************************************************************************
 * the class ContentCache manages the cache for the generated pdf files, which are requested more than one time, until its size exeeds the configured
 * maximum size
 * 
 * @version 13.01.2009
 * @author Steffen Hankiewicz
 * @author Igor Toker
 ************************************************************************************/
public class ContentCache {
    private File cacheFolder;
    private long maxSizeInMB;

    private static final Logger LOGGER = Logger.getLogger(ContentCache.class);

    /************************************************************************************
     * Constructor for ContentCache
     * 
     * @param inCacheFolder the folder where to save the cached files
     * @param inMaxSizeInMB the maximum size in MB for the whole contentCache-Folder
     * @throws CacheException is thrown, if folder can not be created
     ***********************************************************************************/
    public ContentCache(String inCacheFolder, long inMaxSizeInMB) throws CacheException {
        cacheFolder = new File(inCacheFolder);
        maxSizeInMB = inMaxSizeInMB;

        /* create folder if not exists */
        if (!cacheFolder.exists() && !cacheFolder.mkdir()) {
            throw new CacheException("Can't create cache folder: " + cacheFolder.getAbsolutePath());
        }

        /* check if cache ist directory */
        if (!cacheFolder.isDirectory()) {
            throw new CacheException("Cache-Folder is not a directory: " + cacheFolder.getAbsolutePath());
        }

        /* check if folder is writeable */
        if (!cacheFolder.canWrite()) {
            throw new CacheException("Cache folder not writeable: " + cacheFolder.getAbsolutePath());
        }

        /* check cache folder size */
        if (isCacheSizeExceeded()) {
            // long currentSize = FileUtils.sizeOfDirectory(cacheFolder);
            // long maxSize = maxSizeInMB * 1024 * 1024;
            cleanCache();
            // throw new CacheException("Given maximum size of cache (" + maxSize + " byte) already exeeded ("
            // + currentSize + " byte)");
        }
    }

    @SuppressWarnings("unchecked")
    public void cleanCache() {
        long limit = (long) ((maxSizeInMB * 1024 * 1024) * 0.75);
        File[] files = cacheFolder.listFiles();
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        for (File file : files) {
            FileUtils.deleteQuietly(file);
            if (FileUtils.sizeOfDirectory(cacheFolder) < limit) {
                return;
            }
        }
    }

    /*************************************************************************************
     * check if file with given id exists in cache
     * 
     * @param inId ID as String (no file name, no file extension)
     * @return - true, if file exists
     ************************************************************************************/
    public boolean cacheContains(String inId, String suffix) {
        File file = getFileForId(inId, suffix);
        return file.exists() && file.length() > 0;
    }

    /*************************************************************************************
     * write file from cache with given id to given {@link OutputStream}
     * 
     * @param out OutputStream where to write the cached file
     * @param inId ID as String (no file name, no file extension)
     * @throws CacheException
     ************************************************************************************/
    public void writeToStream(OutputStream out, String inId, String suffix) throws CacheException {
        if (!cacheContains(inId, suffix)) {
            throw new CacheException("File already exists in cache.");
        }
        /*
         * -------------------------------- File exists and can be read? --------------------------------
         */
        File file = getFileForId(inId, suffix);
        if (!file.exists() || !file.canRead()) {
            throw new CacheException("File with given ID (" + inId + ") can not be read. (" + file.getAbsolutePath() + ")");
        }
        // Update Timestamp to be able to find old cache items
        file.setLastModified(System.currentTimeMillis());

        /*
         * -------------------------------- write File to OutputStream --------------------------------
         */
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
        } catch (FileNotFoundException e) {
            throw new CacheException("File " + file.getAbsolutePath() + " does not exist.", e);
        } catch (IOException e) {
            throw new CacheException("IO-Error while writing file to stream", e);
        }
    }

    /*************************************************************************************
     * remove file with given id from cache
     * 
     * @param inId
     * @throws CacheException
     ************************************************************************************/
    public void delete(String inId, String suffix) throws CacheException {
        File file = getFileForId(inId, suffix);
        if (cacheContains(inId, suffix)) {
            if (!file.delete()) {
                throw new CacheException("File " + file.getAbsolutePath() + " can not be deleted.");
            }
        }
    }

    /*************************************************************************************
     * check if maximum size of cache already exceeded
     * 
     * @return true, if size of cache is already bigger than configured maximum
     * @throws CacheException
     ************************************************************************************/
    public boolean isCacheSizeExceeded() throws CacheException {
        LOGGER.debug("Checking cache size");
        long currentSize = FileUtils.sizeOfDirectory(cacheFolder);
        long maxSize = maxSizeInMB * 1024 * 1024;
        LOGGER.debug("Current cache size = " + currentSize);
        if (currentSize >= maxSize) {
            LOGGER.debug("Cleaning cache");
            cleanCache();
            currentSize = FileUtils.sizeOfDirectory(cacheFolder);
            LOGGER.debug("Done cleaning cache");
        }
        return (currentSize >= maxSize);
    }

    /*************************************************************************************
     * get File for given cacheID
     * 
     * @param inId
     * @return File for given ID from cache
     ************************************************************************************/
    public File getFileForId(String inId, String suffix) {
        return new File(cacheFolder, inId + "." + suffix);
    }
}
