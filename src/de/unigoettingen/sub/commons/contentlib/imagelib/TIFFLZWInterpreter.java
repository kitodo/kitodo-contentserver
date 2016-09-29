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
package de.unigoettingen.sub.commons.contentlib.imagelib;

//TODO: Rename this to TiffLzwImageInterpreter
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;

/************************************************************************************
 * TIFFLZWInterpreter handles Tiff-LZW-Images
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class TIFFLZWInterpreter extends TiffInterpreter implements ImageInterpreter {

    private static final Logger LOGGER = Logger.getLogger(TiffInterpreter.class);

    /************************************************************************************
     * Constructor for {@link TIFFLZWInterpreter} to read an tiff image from given {@link InputStream}
     * 
     * @param inStream {@link InputStream}
     * @throws ImageInterpreterException
     ************************************************************************************/
    public TIFFLZWInterpreter(InputStream inStream) throws ImageInterpreterException {
        this.read(inStream);
    }

    /**
     * bespoke writeToStream method which sets the compression parameter to LZW.
     */
    public void writeToStream(OutputStream outStream) {
        if (this.renderedimage == null) { // no image available
            return;
        }

        try {
            ImageWriter iwriter = getWriter();

            // gets a copy of the default writer
            ImageWriteParam wparam = iwriter.getDefaultWriteParam();
            wparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            wparam.setCompressionType("LZW");
            // wparam.setCompressionQuality(0.5f); // only used for JPEG

            BufferedImage bi = ImageManipulator.fromRenderedToBuffered(this.renderedimage);

            iwriter.setOutput(outStream);
            iwriter.write(null, new IIOImage(bi, null, null), wparam);

        } catch (IOException e) {
            LOGGER.error("IOException occured", e);
        }
    }
}
