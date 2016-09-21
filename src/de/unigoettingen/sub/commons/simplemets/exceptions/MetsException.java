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
 * A MetsException is thrown whenever a problem occurs, which is related to the contents of the METS file being parsed. This may be related to invalid
 * XML structures, unkown values etc.
 * 
 * @version 12.01.2009�
 * @author Steffen Hankiewicz
 * @author Markus Enders ********************************************************************************
 */
public class MetsException extends Exception {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 5097874287008154951L;

    /**
     * Instantiates a new mets exception.
     */
    public MetsException() {
        super();
    }

    /**
     * Instantiates a new mets exception.
     * 
     * @param inMessage the in message
     */
    public MetsException(String inMessage) {
        super(inMessage);
    }

    /**
     * Instantiates a new mets exception.
     * 
     * @param incause the incause
     */
    public MetsException(Throwable incause) {
        super(incause);
    }

    /**
     * Instantiates a new mets exception.
     * 
     * @param inMessage the in message
     * @param incause the incause
     */
    public MetsException(String inMessage, Throwable incause) {
        super(inMessage, incause);
    }

}
