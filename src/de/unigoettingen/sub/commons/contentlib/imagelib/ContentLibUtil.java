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
package de.unigoettingen.sub.commons.contentlib.imagelib;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.helpers.Loader;

/************************************************************************************
 * The Class Helper for some repeating simple tasks like getExtensionFromFileName etc.
 * 
 * @version 09.01.2009 
 * @author Steffen Hankiewicz
 ***********************************************************************************/
public class ContentLibUtil {

    /************************************************************************************
     * replace variables in default file names with dynamic content (eg. date and time)
     * 
     * @param inDefaultName file name as String including variables
     * @param inExtension file extension that should be added at the end
     * @return the customized file name as string
     ************************************************************************************/
    public static String getCustomizedFileName(String inDefaultName, String inExtension) {
        String outDefaultName = inDefaultName;
        if (inDefaultName.contains("$datetime")) {
            SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            outDefaultName = inDefaultName.replaceAll("\\$datetime", sd.format(new Date()));
        }
        return outDefaultName + inExtension;
    }

    /************************************************************************************
     * get {@link List} of all PDF-sizes (currently A4, A4Box and original)
     * 
     * @return {@link List} list of pdf sizes
     ************************************************************************************/
    public static List<String> getAllPdfSizesAsList() {
        ArrayList<String> sizes = new ArrayList<String>();
        sizes.add("A4");
        sizes.add("original");
        sizes.add("A4Box");
        return sizes;
    }

    /************************************************************************************
     * get {@link File} from application root path in file system
     * 
     * @return {@link File} for root path
     ************************************************************************************/
    public static File getBaseFolderAsFile() {
        File basefolder;
        // TODO: GDZ: Do wee really need to depend on Log4J here? I don't think so...
        URL url = Loader.getResource("");
        try {
            basefolder = new File(url.toURI());
        } catch (URISyntaxException ue) {
            basefolder = new File(url.getPath());
        }
        return basefolder;
    }
}
