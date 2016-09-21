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

import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.OutputStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;

/************************************************************************************
 * interface for all ImageInterpreters for the different image types.
 * 
 * @version 06.01.2009 
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * ***********************************************************************************/
public interface ImageInterpreter {

    /**
     * Gets the x resolution.
     * 
     * @return the x resolution
     */
    public float getXResolution();

    /**
     * Sets the x resolution.
     * 
     * @param resolution the new x resolution
     */
    public void setXResolution(float resolution);

    /**
     * Gets the y resolution.
     * 
     * @return the y resolution
     */
    public float getYResolution();

    /**
     * Sets the y resolution.
     * 
     * @param resolution the new y resolution
     */
    public void setYResolution(float resolution);

    /**
     * Gets the height.
     * 
     * @return the height
     */
    public int getHeight();

    /**
     * Sets the height.
     * 
     * @param height the new height
     */
    public void setHeight(int height);

    /**
     * Gets the width.
     * 
     * @return the width
     */
    public int getWidth();

    /**
     * Sets the width.
     * 
     * @param width the new width
     */
    public void setWidth(int width);

    /**
     * Gets the colordepth.
     * 
     * @return the colordepth
     */
    public int getColordepth();

    /**
     * Sets the colordepth.
     * 
     * @param colordepth the new colordepth
     */
    public void setColordepth(int colordepth);

    /**
     * Gets the samplesperpixel.
     * 
     * @return the samplesperpixel
     */
    public int getSamplesperpixel();

    /**
     * Sets the samplesperpixel.
     * 
     * @param samplesperpixel the new samplesperpixel
     */
    public void setSamplesperpixel(int samplesperpixel);

    /**
     * Gets the rendered image.
     * 
     * @return the rendered image
     */
    public RenderedImage getRenderedImage();

    /**
     * Pdf bytestream embeddable.
     * 
     * @return true, if successful
     */
    public boolean pdfBytestreamEmbeddable();

    /**
     * Gets the image byte stream.
     * 
     * @return the image byte stream
     */
    public byte[] getImageByteStream();

    /**
     * write the content of the RenderedImage to a stream
     * 
     * @param outStream the out stream
     */
    public void writeToStream(FileOutputStream fos, OutputStream outStream);

    public byte[] writeToStreamAndByteArray(OutputStream outStream);

    /**
     * Gets the writer compression type.
     * 
     * @return the writer compression type
     * @throws ParameterNotSupportedException the parameter not supported exception
     */
    public int getWriterCompressionType() throws ParameterNotSupportedException;

    /**
     * Sets the writer compression type.
     * 
     * @param type the new writer compression type
     * @throws ParameterNotSupportedException the parameter not supported exception
     */
    public void setWriterCompressionType(int type) throws ParameterNotSupportedException;

    /**
     * Gets the writer compression value.
     * 
     * @return the writer compression value
     * @throws ParameterNotSupportedException the parameter not supported exception
     */
    public int getWriterCompressionValue() throws ParameterNotSupportedException;

    /**
     * Sets the writer compression value.
     * 
     * @param value the new writer compression value
     * @throws ParameterNotSupportedException the parameter not supported exception
     */
    public void setWriterCompressionValue(int value) throws ParameterNotSupportedException;

    public void clear();

}
