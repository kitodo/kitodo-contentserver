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
package de.unigoettingen.sub.commons.contentlib.exceptions;

/************************************************************************************
 * general Exception thrown if the given parameter for the requests are not valide.
 * 
 * @version 02.05.2009 
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * *********************************************************************************/
public class ParameterNotSupportedException extends ContentLibImageException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 12008110101L;

    /**
     * Instantiates a new parameter not supported exception.
     */
    public ParameterNotSupportedException() {
        super();
    }

    /**
     * Instantiates a new parameter not supported exception.
     * 
     * @param inMessage the in message
     */
    public ParameterNotSupportedException(String inMessage) {
        super(inMessage);
    }

    /**
     * Instantiates a new parameter not supported exception.
     * 
     * @param incause the incause
     */
    public ParameterNotSupportedException(Throwable incause) {
        super(incause);
    }

    /**
     * Instantiates a new parameter not supported exception.
     * 
     * @param inMessage the in message
     * @param incause the incause
     */
    public ParameterNotSupportedException(String inMessage, Throwable incause) {
        super(inMessage, incause);
    }
}
