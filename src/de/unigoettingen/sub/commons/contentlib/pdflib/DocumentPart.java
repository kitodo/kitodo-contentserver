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
package de.unigoettingen.sub.commons.contentlib.pdflib;

import java.net.URL;

public class DocumentPart {

    public enum DocumentPartType {
        PDF, METS;
    }

    private URL url;
    private String divid;
    private PDFTitlePage titlepage;
    private DocumentPartType type;
    private String metsFileGroup;

    /**
     * Constructor
     */
    @SuppressWarnings("unused")
    private DocumentPart() {
        // never call this one from outside
    }

    /**************************************************************************************
     * Constructor incl. divid
     * 
     * @param inURL the url to set
     * @param inType the {@link DocumentPartType} to set
     **************************************************************************************/
    public DocumentPart(URL inURL, DocumentPartType inType) {
        url = inURL;
        type = inType;
    }

    /**************************************************************************************
     * Constructor incl. divid
     * 
     * @param inURL the url to set
     * @param inDivid the divid to set
     * @param inType the {@link DocumentPartType} to set
     **************************************************************************************/
    public DocumentPart(URL inURL, String inDivid, DocumentPartType inType) {
        url = inURL;
        type = inType;
        this.divid = inDivid;
    }

    /*************************************************************************************
     * Getter for url
     * 
     * @return the url
     *************************************************************************************/
    public URL getUrl() {
        return url;
    }

    /*************************************************************************************
     * Getter for type
     * 
     * @return the type
     *************************************************************************************/
    public DocumentPartType getType() {
        return type;
    }

    /*************************************************************************************
     * Getter for divid
     * 
     * @return the divid
     *************************************************************************************/
    public String getDivid() {
        return divid;
    }

    /**************************************************************************************
     * Setter for divid
     * 
     * @param divid the divid to set
     **************************************************************************************/
    public void setDivid(String divid) {
        this.divid = divid;
    }

    /*************************************************************************************
     * Getter for titlepage
     * 
     * @return the titlepage
     *************************************************************************************/
    public PDFTitlePage getTitlepage() {
        return titlepage;
    }

    /**************************************************************************************
     * Setter for titlepage
     * 
     * @param titlepage the titlepage to set
     **************************************************************************************/
    public void setTitlepage(PDFTitlePage titlepage) {
        this.titlepage = titlepage;
    }

    /*************************************************************************************
     * Getter for metsFileGroup
     * 
     * @return the metsFileGroup
     *************************************************************************************/
    public String getMetsFileGroup() {
        return metsFileGroup;
    }

    /**************************************************************************************
     * Setter for metsFileGroup
     * 
     * @param metsFileGroup the metsFileGroup to set
     **************************************************************************************/
    public void setMetsFileGroup(String metsFileGroup) {
        this.metsFileGroup = metsFileGroup;
    }

}
