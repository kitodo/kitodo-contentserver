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

import com.lowagie.text.pdf.PdfReader;

import de.unigoettingen.sub.commons.util.datasource.AbstractUrlImage;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;

public class PDFPage extends AbstractUrlImage {
    protected PdfReader pdfreader = null;

    public PDFPage() {
        super();
    }

    /**
     * Instantiates a new PDF page.
     * 
     * @param image the image
     */
    public PDFPage(UrlImage image) {
        this.pagenumber = image.getPageNumber();
        this.url = image.getURL();
    }

    /*************************************************************************************
     * Getter for pdfreader
     * 
     * @return the pdfreader
     *************************************************************************************/
    public PdfReader getPdfreader() {
        return pdfreader;
    }

    /**************************************************************************************
     * Setter for pdfreader
     * 
     * @param pdfreader the pdfreader to set
     **************************************************************************************/
    public void setPdfreader(PdfReader pdfreader) {
        this.pdfreader = pdfreader;
    }

}
