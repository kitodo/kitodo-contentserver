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

import de.unigoettingen.sub.commons.contentlib.pdflib.PDFManager.PdfPageSize;

//TODO: GDZ: Merge with configuration for other PDF classes
public class PDFConfiguration {

    private boolean pdfDefaultAlwaysCompressToJPEG = true;
    private boolean pdfDefaultAlwaysUseRenderedImage = true;
    private boolean writeAsPdfA = true;
    private String iccinputfilename = null;
    private PdfPageSize pagesize = PdfPageSize.ORIGINAL;
    private PDFTitlePage pdftitlepage = null;

    /**************************************************************************************
     * simple public constructor
     **************************************************************************************/
    public PDFConfiguration() {
        // may be used to instantiate subclasses
    }

    /*************************************************************************************
     * Getter for pdfDefaultAlwaysCompressToJPEG
     * 
     * @return the pdfDefaultAlwaysCompressToJPEG
     *************************************************************************************/
    public boolean isPdfDefaultAlwaysCompressToJPEG() {
        return pdfDefaultAlwaysCompressToJPEG;
    }

    /**************************************************************************************
     * Setter for pdfDefaultAlwaysCompressToJPEG
     * 
     * @param pdfDefaultAlwaysCompressToJPEG the pdfDefaultAlwaysCompressToJPEG to set
     **************************************************************************************/
    public void setPdfDefaultAlwaysCompressToJPEG(boolean pdfDefaultAlwaysCompressToJPEG) {
        this.pdfDefaultAlwaysCompressToJPEG = pdfDefaultAlwaysCompressToJPEG;
    }

    /*************************************************************************************
     * Getter for pdfDefaultAlwaysUseRenderedImage
     * 
     * @return the pdfDefaultAlwaysUseRenderedImage
     *************************************************************************************/
    public boolean isPdfDefaultAlwaysUseRenderedImage() {
        return pdfDefaultAlwaysUseRenderedImage;
    }

    /**************************************************************************************
     * Setter for pdfDefaultAlwaysUseRenderedImage
     * 
     * @param pdfDefaultAlwaysUseRenderedImage the pdfDefaultAlwaysUseRenderedImage to set
     **************************************************************************************/
    public void setPdfDefaultAlwaysUseRenderedImage(boolean pdfDefaultAlwaysUseRenderedImage) {
        this.pdfDefaultAlwaysUseRenderedImage = pdfDefaultAlwaysUseRenderedImage;
    }

    /*************************************************************************************
     * Getter for writeAsPdfA
     * 
     * @return the writeAsPdfA
     *************************************************************************************/
    public boolean isWriteAsPdfA() {
        return writeAsPdfA;
    }

    /**************************************************************************************
     * Setter for writeAsPdfA
     * 
     * @param writeAsPdfA the writeAsPdfA to set
     **************************************************************************************/
    public void setWriteAsPdfA(boolean writeAsPdfA) {
        this.writeAsPdfA = writeAsPdfA;
    }

    /*************************************************************************************
     * Getter for iccinputfilename
     * 
     * @return the iccinputfilename
     *************************************************************************************/
    public String getIccinputfilename() {
        return iccinputfilename;
    }

    /**************************************************************************************
     * Setter for iccinputfilename
     * 
     * @param iccinputfilename the iccinputfilename to set
     **************************************************************************************/
    public void setIccinputfilename(String iccinputfilename) {
        this.iccinputfilename = iccinputfilename;
    }

    /*************************************************************************************
     * Getter for pdftitlepage
     * 
     * @return the pdftitlepage
     *************************************************************************************/
    public PDFTitlePage getPdftitlepage() {
        return pdftitlepage;
    }

    /**************************************************************************************
     * Setter for pdftitlepage
     * 
     * @param pdftitlepage the pdftitlepage to set
     **************************************************************************************/
    public void setPdftitlepage(PDFTitlePage pdftitlepage) {
        this.pdftitlepage = pdftitlepage;
    }

    public void setPagesize(PdfPageSize pagesize) {
        this.pagesize = pagesize;
    }

    public PdfPageSize getPagesize() {
        return pagesize;
    }

}
