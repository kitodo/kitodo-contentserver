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
package de.unigoettingen.sub.commons.simplemets.exceptions;

/************************************************************************************
 * A MetsParserException is thrown, whenever an error occurs in the METSParser class which is NOT related to the content of the METS file itself. E.g.
 * if a file or an http stream cannot be opened or other internal problems occure
 * 
 * @version 12.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * ***********************************************************************************/
public class MetsParserException extends MetsException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 4426591261011049594L;

    /**
     * Instantiates a new mets parser exception.
     */
    public MetsParserException() {
        super();
    }

    /**
     * Instantiates a new mets parser exception.
     * 
     * @param inMessage the in message
     */
    public MetsParserException(String inMessage) {
        super(inMessage);
    }

    /**
     * Instantiates a new mets parser exception.
     * 
     * @param incause the incause
     */
    public MetsParserException(Throwable incause) {
        super(incause);
    }

    /**
     * Instantiates a new mets parser exception.
     * 
     * @param inMessage the in message
     * @param incause the incause
     */
    public MetsParserException(String inMessage, Throwable incause) {
        super(inMessage, incause);
    }

}
