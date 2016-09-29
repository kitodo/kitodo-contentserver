package de.unigoettingen.sub.commons.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.ContentServer;

/**
 * @author Andrey Kozhushkov
 */
public class CacheUtils {

    private static final Logger LOGGER = Logger.getLogger(CacheUtils.class);

    /**
     * Removes Ehcache elements whose key starts with any of the given identifiers.
     * 
     * @param identifiers
     * @param fromContentCache
     * @param fromThumbnailCache
     * @return
     */
    public static int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache) {
        int count = 0;

        if (identifiers != null) {
            for (String identifier : identifiers) {
                count += deleteFromCache(identifier, fromContentCache, fromThumbnailCache);
            }
        }

        return count;
    }

    /**
     * Removes Ehcache elements whose key starts with the given identifier.
     * 
     * @param identifier
     * @param fromContentCache If true, cache elements will be removed from the content cache.
     * @param fromThumbnailCache If true, cache elements will be removed from the thumbnail cache.
     * @return Total number of deleted Ehcache elements.
     */
    @SuppressWarnings("unchecked")
    public static int deleteFromCache(String identifier, boolean fromContentCache, boolean fromThumbnailCache) {
        int countContent = 0;
        int countThumbs = 0;

        if (StringUtils.isNotBlank(identifier)) {
            if (fromContentCache) {
                try {
                    Cache cc = ContentServer.getContentCache();
                    List<String> keys = cc.getKeys();
                    Set<String> keysToRemove = new HashSet<String>();
                    for (String key : keys) {
                        if (key.startsWith(identifier + "_")) {
                            keysToRemove.add(key);
                            ++countContent;
                        }
                    }
                    cc.removeAll(keysToRemove);
                } catch (CacheException e) {
                    LOGGER.error(e.getMessage(), e);
                    countContent = 0;
                }
            }
            if (fromThumbnailCache) {
                try {
                    Cache cc = ContentServer.getThumbnailCache();
                    List<String> keys = cc.getKeys();
                    Set<String> keysToRemove = new HashSet<String>();
                    for (String key : keys) {
                        if (key.startsWith(identifier + "_")) {
                            keysToRemove.add(key);
                            ++countThumbs;
                        }
                    }
                    cc.removeAll(keysToRemove);
                } catch (CacheException e) {
                    LOGGER.error(e.getMessage(), e);
                    countThumbs = 0;
                }
            }
        }

        return countContent + countThumbs;
    }

    /**
     * Removes all elements from Ehcache.
     * 
     * @param contentCache If true, all elements will be deleted from the content cache.
     * @param thumbnailCache If true, all elements will be deleted from the thumbnail cache.
     */
    public static void emptyCache(boolean contentCache, boolean thumbnailCache) {
        if (contentCache) {
            try {
                ContentServer.getContentCache().removeAll();
            } catch (CacheException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        if (contentCache) {
            try {
                ContentServer.getThumbnailCache().removeAll();
            } catch (CacheException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
