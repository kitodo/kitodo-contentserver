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
import de.unigoettingen.sub.commons.simplemets.exceptions.MetsException;

/************************************************************************************
 * Interface for metadata extractors
 * 
 * @version 12.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
// TODO: Remove underscores and "PDF"
public interface MetadataExtractor {

    /**
     * calulate the metadata for the pdf file
     * 
     * @param inDiv as {@link DivType}
     * @param metsparser as {@link METSParser}
     * @throws MetsException
     */
    public abstract void calculateMetadata(DivType inDiv, METSParser metsparser) throws MetsException;

    /**
     * calulate the metadata for the pdf file
     * 
     * @param inDivID as {@link String}
     * @param metsparser as {@link METSParser}
     * @throws MetsException
     */
    public abstract void calculatePDFMetadata(String inDivID, METSParser metsparser) throws MetsException;

    /************************************************************************************
     * @return the pdftitle
     ***********************************************************************************/
    public abstract String getPdftitle();

    /*************************************************************************************
     * @return the pdfcreator
     ************************************************************************************/
    public abstract String getPdfcreator();

    /*************************************************************************************
     * @return the pdfkeywords
     ************************************************************************************/
    public abstract String getPdfkeywords();

    /*************************************************************************************
     * @return the pdf_titlepage_line1
     ************************************************************************************/
    public abstract String getPdfTitlepageLine1();

    /*************************************************************************************
     * @return the pdf_titlepage_line2
     ************************************************************************************/
    public abstract String getPdfTitlepageLine2();

    /*************************************************************************************
     * @return the pdf_titlepage_line3
     ************************************************************************************/
    public abstract String getPdfTitlepageLine3();

    /*************************************************************************************
     * @return the pdf_titlepage_line4
     ************************************************************************************/
    public abstract String getPdfTitlepageLine4();

    /*************************************************************************************
     * @return the structtype
     ************************************************************************************/
    public String getStructType();

}