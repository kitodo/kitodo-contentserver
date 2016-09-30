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

import java.util.LinkedList;
import java.util.List;

import com.lowagie.text.pdf.PdfOutline;

import de.unigoettingen.sub.commons.util.datasource.AbstractStructure;
import de.unigoettingen.sub.commons.util.datasource.Structure;

/************************************************************************************
 * Bookmark for handling of metadata for pdf files
 * 
 * @version 06.01.2009 
 * @author Markus Enders
 ************************************************************************************/
public class PDFBookmark extends AbstractStructure<PDFBookmark> {
    protected PdfOutline pdfOutline;

    public PDFBookmark(Structure struct) {
        super(struct);
        // this(struct.getImageNumber(), struct.getContent());
        if (struct instanceof PDFBookmark) {
            pdfOutline = getPdfOutline();
        }

        if (!struct.getChildren().isEmpty()) {
            for (Structure s : struct.getChildren()) {
                addChildBookmark(new PDFBookmark(s));
            }
        }

    }

    public PDFBookmark() {
        super();
    }

    /**************************************************************************************
     * Constructor which create a new bookmark with pagename and content
     * 
     * @param pagename as Integer
     * @param content as String
     **************************************************************************************/
    public PDFBookmark(Integer pagename, String content) {
        super(pagename, content);
    }

    /*************************************************************************************
     * Getter for pdfOutline
     * 
     * @return the pdfOutline
     *************************************************************************************/
    public PdfOutline getPdfOutline() {
        return pdfOutline;
    }

    /**************************************************************************************
     * Setter for pdfOutline
     * 
     * @param pdfOutline the pdfOutline to set
     **************************************************************************************/
    public void setPdfOutline(PdfOutline pdfOutline) {
        this.pdfOutline = pdfOutline;
    }

    /**
     * Convert list.
     * 
     * @param structList the struct list
     * 
     * @return the list< pdf bookmark>
     */
    public static List<PDFBookmark> convertList(final List<? extends Structure> structList) {
        List<PDFBookmark> returnList = new LinkedList<PDFBookmark>();
        for (Structure struct : structList) {
            returnList.add(new PDFBookmark(struct));
        }
        return returnList;
    }
}
