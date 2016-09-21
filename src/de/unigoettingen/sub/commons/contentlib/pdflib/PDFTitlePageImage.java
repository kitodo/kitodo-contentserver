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
 * PDFTitlePageImage for defining the pdf front page image
 * 
 * @version 06.01.2009 
 * @author Markus Enders
 ************************************************************************************/
public class PDFTitlePageImage {
    float xCoordinate = 0f;
    float yCoordinate = 0f;

    String filename = null;
    int scalefactor = 1;

    /************************************************************************************
     * public constructor
     ************************************************************************************/
    public PDFTitlePageImage() {
    }

    /************************************************************************************
     * public constructor with file name
     * 
     * @param in String with FileName
     ************************************************************************************/
    public PDFTitlePageImage(String in) {
        this.filename = in;
    }

    /**
     * @return the xCoordinate
     */
    public float getXCoordinate() {
        return this.xCoordinate;
    }

    /**
     * @param coordinate the xCoordinate to set
     */
    public void setXCoordinate(float coordinate) {
        this.xCoordinate = coordinate;
    }

    /**
     * @return the yCoordinate
     */
    public float getYCoordinate() {
        return this.yCoordinate;
    }

    /**
     * @param coordinate the yCoordinate to set
     */
    public void setYCoordinate(float coordinate) {
        this.yCoordinate = coordinate;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the scalefactor
     */
    public int getScalefactor() {
        return this.scalefactor;
    }

    /**
     * @param scalefactor the scalefactor to set
     */
    public void setScalefactor(int scalefactor) {
        this.scalefactor = scalefactor;
    }

}
