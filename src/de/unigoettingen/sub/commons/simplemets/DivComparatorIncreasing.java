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
package de.unigoettingen.sub.commons.simplemets;

import gov.loc.mets.DivType;

import java.math.BigInteger;
import java.util.Comparator;

/************************************************************************************
 * Comparator class for {@link DivType} increasing
 * 
 * @version 12.01.2009�
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class DivComparatorIncreasing implements Comparator<DivType> {

    @Override
    public int compare(DivType arg0, DivType arg1) {

        BigInteger x = arg0.getORDER();
        BigInteger y = arg1.getORDER();

        if ((x == null) && (y == null)) {
            return 0;
        }
        if (x == null) {
            return 1;
        }
        if (y == null) {
            return -1;
        }

        if (x.compareTo(y) == -1) {
            // x < y
            return -1;
        } else if (x.compareTo(y) == 1) {
            // x > y
            return 1;
        } else {
            return 0;
        }

    }
}
