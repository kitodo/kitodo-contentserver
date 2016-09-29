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
package de.unigoettingen.sub.commons.contentlib.imagelib;

import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;

/*******************************************************************************
 * abstract class for all ImageInterpreters for different image types
 * 
 * @version 06.01.2009 
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ******************************************************************************/
public abstract class AbstractImageInterpreter {
    private static final Logger LOGGER = Logger.getLogger(AbstractImageInterpreter.class);
    RenderedImage renderedimage = null; // rendered image version of the image

    float xResolution = 0f;
    float yResolution = 0f;

    int height = 0;
    int width = 0;

    int colorDepth = 0;
    int samplesPerPixel = 0;

    byte rawbytes[];

    /***************************************************************************
     * Getter for xResolution
     * 
     * @return the xResolution
     **************************************************************************/
    public float getXResolution() {
        if (xResolution <= 1f) {
            return 72;
        }
        return xResolution;
    }

    /***************************************************************************
     * Setter for xResolution
     * 
     * @param resolution the xResolution to set
     **************************************************************************/
    public void setXResolution(float resolution) {
        xResolution = resolution;
    }

    /**
     * @return the yResolution
     */
    public float getYResolution() {
        if (yResolution <= 1f) {
            return 72;
        }
        return yResolution;
    }

    /**
     * @param resolution the yResolution to set
     */
    public void setYResolution(float resolution) {
        yResolution = resolution;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the colordepth
     */
    public int getColordepth() {
        return colorDepth;
    }

    /**
     * @param colordepth the colordepth to set
     */
    public void setColordepth(int colordepth) {
        this.colorDepth = colordepth;
    }

    /**
     * @return the samplesperpixel
     */
    public int getSamplesperpixel() {
        return samplesPerPixel;
    }

    /**
     * read {@link InputStream} for image
     * 
     * @param is inputstream
     */
    public void readImageStream(InputStream is) {
        try {
            rawbytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            LOGGER.error("IO-Error occured", e);
        }
    }

    /**
     * Method creates an image format specific byte stream Needs to be overloaded by the specific ImageInterpreter class
     */
    abstract public void createByteStreamFromRenderedImage();

    /**
     * writes the rawbytes into an output stream. If they are not available they are created by calling the interpreter specific
     * createBytestreamFromRenderedImage method
     * 
     * @param outStream
     */

    public void writeToStream(FileOutputStream fos, OutputStream outStream) {
        if (rawbytes == null) {
            // create stream
            createByteStreamFromRenderedImage();
        }

        try {
            for (byte rawbyte : rawbytes) {
                outStream.write(rawbyte);
            }
        } catch (IOException e) {
            LOGGER.error("IOException occured", e);
        }
    }

    /**
     * retrieve a byte array of the data
     * 
     * @return
     */

    public byte[] getImageByteStream() {
        return rawbytes;
    }

    /**
     * @param samplesperpixel the samplesperpixel to set
     */
    public void setSamplesperpixel(int samplesperpixel) {
        this.samplesPerPixel = samplesperpixel;
    }

    /**
     * @return
     */
    public RenderedImage getRenderedImage() {
        return this.renderedimage;
    }

    /**
     * Indicates wether the image's bytestream is directly embeddable.
     * 
     * @return true if bytestream is embeddable
     */
    public boolean pdfBytestreamEmbeddable() {
        return false;
    }

    class ByteBuffer {
        byte buffer[];

        public ByteBuffer(int size) {
            buffer = new byte[size];
        }

        public byte[] getBufferContents() {
            return buffer;
        }

        public void setBufferContents(byte in[]) {
            buffer = in;
        }
    }

    /**
     * @return the writerCompressionType
     * @throws ParameterNotSupportedException
     */
    public int getWriterCompressionType() throws ParameterNotSupportedException {
        ParameterNotSupportedException pnse = new ParameterNotSupportedException();
        throw pnse;
    }

    /**
     * @param writerCompressionType the writerCompressionType to set
     * @throws ParameterNotSupportedException
     */
    public void setWriterCompressionType(int writerCompressionType) throws ParameterNotSupportedException {
        ParameterNotSupportedException pnse = new ParameterNotSupportedException();
        throw pnse;
    }

    /**
     * @return the writerCompressionValue
     * @throws ParameterNotSupportedException
     */
    public int getWriterCompressionValue() throws ParameterNotSupportedException {
        ParameterNotSupportedException pnse = new ParameterNotSupportedException();
        throw pnse;
    }

    /**
     * @param writerCompressionValue the writerCompressionValue to set
     * @throws ParameterNotSupportedException
     */
    public void setWriterCompressionValue(int writerCompressionValue) throws ParameterNotSupportedException {
        ParameterNotSupportedException pnse = new ParameterNotSupportedException();
        throw pnse;
    }

    public void clear() {
        LOGGER.debug("Cleanung up ImageInterpreter");
        LOGGER.debug("Free memory before operation: " + Runtime.getRuntime().freeMemory());
        rawbytes = null;
        // Runtime.getRuntime().runFinalization();
        // Runtime.getRuntime().gc();
        LOGGER.debug("Free memory after operation: " + Runtime.getRuntime().freeMemory());
    }

    /**
     * Prints a node with all subnodes; just for debugging
     * 
     * @param domNode
     * @return
     */
    // Removed see /intranda_ContentServer/WebContent/WEB-INF/src/de/unigoettingen/commons/util/xml/XMLDumper.java

}
