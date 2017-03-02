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
package de.unigoettingen.sub.commons.simplemets;

import gov.loc.mets.DivType;

/*************************************************************************************
 * This is just a simple class, for creating the Bookmark's content from the &lt;div&gt; element's metadata. This simple class makes only use of METS,
 * further extension schemas are not used.
 * 
 * @version 12.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class SimpleStructureMetadataExtractor implements StructureMetadataExtractor {

    /*
     * (non-Javadoc)
     * 
     * @see de.unigoettingen.sub.gdz.goobi.contentServlet.BookmarkMetadataExtractor #getBookmarkMetadata(gov.loc.mets.DivType)
     */
    @Override
    public String getStructureMetadata(DivType inDiv, METSParser inParser) {
        String result = null;

        result = inDiv.getLABEL();
        if (result == null) {
            result = inDiv.getTYPE();
        }
        return result;
    }

}
