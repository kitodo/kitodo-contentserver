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
package de.unigoettingen.sub.commons.contentlib.pdflib;

/************************************************************************************
 * PDFTitlePageParagraph for handling of paragraph on pdf front page
 * 
 * @version 06.01.2009 
 * @author Markus Enders
 ************************************************************************************/
public class PDFTitlePageParagraph {
    String content = "";
    int fontsize = 12;
    String fonttype = "Helvetica";

    /************************************************************************************
     * public constructor
     ************************************************************************************/
    public PDFTitlePageParagraph() {
        // may be used for subclassing
    }

    /*************************************************************************************
     * Getter for content
     * 
     * @return the content
     *************************************************************************************/
    public String getContent() {
        return content;
    }

    /**************************************************************************************
     * Setter for content
     * 
     * @param content the content to set
     **************************************************************************************/
    public void setContent(String content) {
        this.content = content;
    }

    /*************************************************************************************
     * Getter for fontsize
     * 
     * @return the fontsize
     *************************************************************************************/
    public int getFontsize() {
        return fontsize;
    }

    /**************************************************************************************
     * Setter for fontsize
     * 
     * @param fontsize the fontsize to set
     **************************************************************************************/
    public void setFontsize(int fontsize) {
        this.fontsize = fontsize;
    }

    /*************************************************************************************
     * Getter for fonttype
     * 
     * @return the fonttype
     *************************************************************************************/
    public String getFonttype() {
        return fonttype;
    }

    /**************************************************************************************
     * Setter for fonttype
     * 
     * @param fonttype the fonttype to set
     **************************************************************************************/
    public void setFonttype(String fonttype) {
        this.fonttype = fonttype;
    }

}
