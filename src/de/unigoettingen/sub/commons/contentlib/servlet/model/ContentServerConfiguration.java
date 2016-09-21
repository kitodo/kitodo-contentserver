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
package de.unigoettingen.sub.commons.contentlib.servlet.model;

import java.awt.Color;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.servlet.Util;

/************************************************************************************
 * central Configuration class for Contentserver servlet
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public final class ContentServerConfiguration {
    private static final Logger LOGGER = Logger.getLogger(ContentServerConfiguration.class);

    XMLConfiguration config;
    private static ContentServerConfiguration instance;

    /************************************************************************************
     * private default constructor to forbid instantiation
     ************************************************************************************/
    private ContentServerConfiguration() {

    }

    /************************************************************************************
     * get singleton object
     ************************************************************************************/
    synchronized public static ContentServerConfiguration getInstance() {
        if (instance == null) {
            instance = new ContentServerConfiguration();
            try {
                File file = new File(Util.getBaseFolderAsFile(), "contentServerConfig.xml");
                instance.config = new XMLConfiguration(file);
                instance.config.setReloadingStrategy(new FileChangedReloadingStrategy());
            } catch (ConfigurationException e) {
                LOGGER.error("ConfigurationException occured", e);
                instance.config = new XMLConfiguration();
            }
        }
        return instance;
    }

    /***************************************************************************************************************
     * get maximal file length for input images
     * 
     * @return int
     ***************************************************************************************************************/
    public Integer getMaxFileLength() {
        return config.getInt("maxFileLength[@value]", 0) * 1024 * 1024;
    }

    public String getErrorFile() {
        return config.getString("maxFileLength[@file]");
    }

    /************************************************************************************
     * get path of repository from configuration as URL-String
     * 
     * @return path to repository as {@link String}
     ************************************************************************************/
    public String getRepositoryPathImages() {
        return config.getString("defaultRepositoryPathImages[@value]");
    }

    public String getRepositoryPathPdf() {
        return config.getString("defaultRepositoryPathPdf[@value]", "");
    }

    public Boolean getUsePdf() {
        return config.getBoolean("defaultRepositoryPathPdf[@usage]", false);
    }

    /************************************************************************************
     * get path of repository from configuration
     * 
     * @return path to repository as {@link String}
     ************************************************************************************/
    public String getRepositoryPathMets() {
        return config.getString("defaultRepositoryPathMets[@value]");
    }

    /************************************************************************************
     * get path of content cache from configuration
     * 
     * @return path to content cache as {@link String}
     ************************************************************************************/
    public String getContentCachePath() {
        return config.getString("contentCache[@path]");
    }

    /************************************************************************************
     * get maximum size of content cache from configuration
     * 
     * @return maximum size to content cache as {@link Long}
     ************************************************************************************/
    public Long getContentCacheSize() {
        return config.getLong("contentCache[@size]", 100);
    }

    /************************************************************************************
     * get boolean if content cache should be used or not
     * 
     * @return value if cache should be used as Boolean
     ************************************************************************************/
    public Boolean getContentCacheUse() {
        return config.getBoolean("contentCache[@useCache]");
    }

    /************************************************************************************
     * get boolean if content cache should use short file names
     * 
     * @return value if cache should use short file names as Boolean
     ************************************************************************************/
    public Boolean getContentCacheUseShortFileNames() {
        return config.getBoolean("contentCache[@useShortFileNames]");
    }

    public String getThumbnailCachePath() {
        return config.getString("thumbnailCache[@path]");
    }

    /************************************************************************************
     * get maximum size of content cache from configuration
     * 
     * @return maximum size to content cache as {@link Long}
     ************************************************************************************/
    public Long getThumbnailCacheSize() {
        return config.getLong("thumbnailCache[@size]", 100);
    }

    /************************************************************************************
     * get boolean if content cache should be used or not
     * 
     * @return value if cache should be used as Boolean
     ************************************************************************************/
    public Boolean getThumbnailCacheUse() {
        return config.getBoolean("thumbnailCache[@useCache]");
    }

    /************************************************************************************
     * get boolean if content cache should use short file names
     * 
     * @return value if cache should use short file names as Boolean
     ************************************************************************************/
    public Boolean getThumbnailCacheUseShortFileNames() {
        return config.getBoolean("thumbnailCache[@useShortFileNames]");
    }

    /************************************************************************************
     * get boolean if pdf cache should be used or not
     * 
     * @return value if cache should be used as Boolean
     ************************************************************************************/
    public Boolean getPdfCacheUse() {
        return config.getBoolean("pdfCache[@useCache]");
    }

    /************************************************************************************
     * get boolean if pdf cache should use short file names
     * 
     * @return value if pdf should use short file names as Boolean
     ************************************************************************************/
    public Boolean getPdfCacheUseShortFileNames() {
        return config.getBoolean("pdfCache[@useShortFileNames]");
    }

    /************************************************************************************
     * get maximum size of pdf cache from configuration
     * 
     * @return maximum size to pdf cache as {@link Long}
     ************************************************************************************/
    public Long getPdfCacheSize() {
        return config.getLong("pdfCache[@size]", 100);
    }

    /************************************************************************************
     * get path of pdf cache from configuration
     * 
     * @return path to pdf cache as {@link String}
     ************************************************************************************/
    public String getPdfCachePath() {
        return config.getString("pdfCache[@path]");
    }

    /************************************************************************************
     * get default resolution from configuration
     * 
     * @return default resolution as {@link Integer}
     ************************************************************************************/
    public Integer getDefaultResolution() {
        return config.getInt("defaultResolution[@value]");
    }

    /************************************************************************************
     * get default highlight color from configuration
     * 
     * @return color as {@link Color}
     ************************************************************************************/
    public Color getDefaultHighlightColor() {
        int red = config.getInt("defaultHighlightColor[@valueRed]");
        int green = config.getInt("defaultHighlightColor[@valueGreen]");
        int blue = config.getInt("defaultHighlightColor[@valueBlue]");
        int alpha = config.getInt("defaultHighlightColor[@valueAlpha]");
        return new Color(red, green, blue, alpha);
    }

    /************************************************************************************
     * get default image file name
     * 
     * @return file name as String
     ************************************************************************************/
    public String getDefaultFileNameImages() {
        return config.getString("defaultFileNames.image[@value]");
    }

    /************************************************************************************
     * get default value if sending of image should contain header "attachment"
     * 
     * @return if sending as attachment
     ************************************************************************************/
    public Boolean getSendImageAsAttachment() {
        return config.getBoolean("defaultFileNames.image[@sendAsAttachment]");
    }

    /************************************************************************************
     * get default pdf file name
     * 
     * @return file name as String
     ************************************************************************************/
    public String getDefaultFileNamePdf() {
        return config.getString("defaultFileNames.pdf[@value]");
    }

    /************************************************************************************
     * get default value if sending of pdf should contain header "attachment"
     * 
     * @return if sending as attachment
     ************************************************************************************/
    public Boolean getSendPdfAsAttachment() {
        return config.getBoolean("defaultFileNames.pdf[@sendAsAttachment]");
    }

    /************************************************************************************
     * get error title font size
     * 
     * @return error title as {@link String}
     ************************************************************************************/
    public String getErrorTitle() {
        return config.getString("errorWaterMark[@title]");
    }

    /************************************************************************************
     * get error title font size
     * 
     * @return error title font size as {@link Integer}
     ************************************************************************************/
    public Integer getErrorTitleFontSize() {
        return config.getInt("errorWaterMark[@titleFontSize]");
    }

    /************************************************************************************
     * get message title font size
     * 
     * @return error message font size as {@link Integer}
     ************************************************************************************/
    public Integer getErrorMessageFontSize() {
        return config.getInt("errorWaterMark[@messageFontSize]");
    }

    /************************************************************************************
     * get maximum message line length
     * 
     * @return maximum message line length as {@link Integer}
     ************************************************************************************/
    public Integer getErrorMessageMaxLineLength() {
        return config.getInt("errorWaterMark[@messageMaxLineLength]");
    }

    /************************************************************************************
     * get default value for pdf generation: convert all images always to rendered image before sending it to iText
     * 
     * @return alwaysUseRenderedImage as {@link Boolean}
     ************************************************************************************/
    public Boolean getPdfDefaultAlwaysUseRenderedImage() {
        return config.getBoolean("defaultPdfConfig[@alwaysUseRenderedImage]", false);
    }

    /************************************************************************************
     * get default value for pdf generation: compress all images always to jpeg image before sending it to iText
     * 
     * @return alwaysUseRenderedImage as {@link Boolean}
     ************************************************************************************/
    public Boolean getPdfDefaultAlwaysCompressToJPEG() {
        return config.getBoolean("defaultPdfConfig[@alwaysCompressToJPEG]", false);
    }

    /************************************************************************************
     * write pdf files as PDF/A files
     * 
     * @return true if pdf should be written as PDF/A
     ************************************************************************************/
    public Boolean getPdfDefaultWritePdfA() {
        return config.getBoolean("defaultPdfConfig[@writeAsPdfA]", true);
    }

    /************************************************************************************
     * get default page size for pdf generation
     * 
     * @return String of pagesize
     ************************************************************************************/
    public String getPdfDefaultPageSize() {
        return config.getString("defaultPdfConfig[@pagesize]", "A4");
    }

    /************************************************************************************
     * check if pdf title page should be used
     * 
     * @return true if pdf title page should be generated
     ************************************************************************************/
    public Boolean getPdfTitlePageUse() {
        return config.getBoolean("pdfTitlePage[@use]");
    }

    /************************************************************************************
     * get path pdf title page configuration
     * 
     * @return path to pdf title page configuration as {@link String}
     * @throws URISyntaxException
     ************************************************************************************/
    public URI getPdfTitlePageConfigFile() throws URISyntaxException {
        return new URI(config.getString("pdfTitlePage[@configFile]"));
    }

    /************************************************************************************
     * get boolean value if watermarks should be used for image processing
     * 
     * @return watermark[@use] as {@link Boolean}
     ************************************************************************************/
    public Boolean getWatermarkUse() {
        return config.getBoolean("watermark[@use]", false);
    }

    /************************************************************************************
     * get full path of watermark config file as URL-String
     * 
     * @return path of watermark[@configFile] as {@link String}
     ************************************************************************************/
    public String getWatermarkConfigFilePath() {
        return config.getString("watermark[@configFile]");
    }

    /************************************************************************************
     * get default mets filegroup for pdf generation
     * 
     * @return String of filegroup name
     ************************************************************************************/
    public String getDefaultMetsFileGroup() {
        return config.getString("defaultPdfConfig[@metsFileGroup]", "DEFAULT");
    }

    /************************************************************************************
     * get boolean if image footers (watermark) should scale with the image
     * 
     * @return value if cache should use short file names as Boolean
     ************************************************************************************/
    public Boolean getScaleWatermark() {
        return config.getBoolean("watermark[@scale]", false);
    }

}
