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

/************************************************************************************
 * PDFTitlePageLine for handling of text on the pdf front page
 * 
 * @version 06.01.2009 
 * @author Markus Enders
 ************************************************************************************/
public class PDFTitlePageLine {

    String content = null;
    int linewrap = 50;
    int shortentextlength = 100;
    int fontsize = 12;
    String fonttype = "Helvetica";
    int linetype = 0;

    /************************************************************************************
     * public constructor
     ************************************************************************************/
    public PDFTitlePageLine() {
        // may be used for subclassing
    }

    /************************************************************************************
     * public constructor
     * 
     * @param inContent String of content
     ************************************************************************************/
    public PDFTitlePageLine(String inContent) {
        content = inContent;
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
     * Getter for linewrap
     * 
     * @return the linewrap
     *************************************************************************************/
    public int getLinewrap() {
        return linewrap;
    }

    /**************************************************************************************
     * Setter for linewrap
     * 
     * @param linewrap the linewrap to set
     **************************************************************************************/
    public void setLinewrap(int linewrap) {
        this.linewrap = linewrap;
    }

    /*************************************************************************************
     * Getter for shortentextlength
     * 
     * @return the shortentextlength
     *************************************************************************************/
    public int getShortentextlength() {
        return shortentextlength;
    }

    /**************************************************************************************
     * Setter for shortentextlength
     * 
     * @param shortentextlength the shortentextlength to set
     **************************************************************************************/
    public void setShortentextlength(int shortentextlength) {
        this.shortentextlength = shortentextlength;
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

    /*************************************************************************************
     * Getter for linetype
     * 
     * @return the linetype
     *************************************************************************************/
    public int getLinetype() {
        return linetype;
    }

    /**************************************************************************************
     * Setter for linetype
     * 
     * @param linetype the linetype to set
     **************************************************************************************/
    public void setLinetype(int linetype) {
        this.linetype = linetype;
    }

}
