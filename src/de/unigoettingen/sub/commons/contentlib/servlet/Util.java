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
package de.unigoettingen.sub.commons.contentlib.servlet;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.helpers.Loader;

public class Util {

    /************************************************************************************
     * get {@link File} from application root path in file system
     * 
     * @return {@link File} for root path
     ************************************************************************************/
    public static File getBaseFolderAsFile() {
        File basefolder;
        // TODO: GDZ: Do we really need to depend on Log4J here? I don't think so...
        URL url = Loader.getResource("");

        if (!url.getProtocol().startsWith("file")) {
            return new File(".");
        }

        try {
            basefolder = new File(url.toURI());
        } catch (URISyntaxException ue) {
            basefolder = new File(url.getPath());
        }
        return basefolder;
    }

}
