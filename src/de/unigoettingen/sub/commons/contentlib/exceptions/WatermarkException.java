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

import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;

/************************************************************************************
 * Exception for watermark handling by {@link Watermark}.
 * 
 * @version 02.05.2009 
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * *********************************************************************************/
public class WatermarkException extends ContentLibPdfException {

    private static final long serialVersionUID = 20081101L;

    /**
     * Instantiates a new watermark exception.
     */
    public WatermarkException() {
        super();
    }

    /**
     * Instantiates a new watermark exception.
     * 
     * @param inMessage the in message
     */
    public WatermarkException(final String inMessage) {
        super(inMessage);
    }

    /**
     * Instantiates a new watermark exception.
     * 
     * @param incause the incause
     */
    public WatermarkException(final Throwable incause) {
        super(incause);
    }

    /**
     * Instantiates a new watermark exception.
     * 
     * @param inMessage the in message
     * @param incause the incause
     */
    public WatermarkException(final String inMessage, final Throwable incause) {
        super(inMessage, incause);
    }

}
