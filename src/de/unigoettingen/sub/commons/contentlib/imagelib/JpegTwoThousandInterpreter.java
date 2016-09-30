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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.ByteArraySeekableStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;

/************************************************************************************
 * JpegZwoThousandInterpreter handles Jpeg-2000-Images (jp2)
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class JpegTwoThousandInterpreter extends AbstractImageInterpreter implements ImageInterpreter {
    private static final Logger LOGGER = Logger.getLogger(JpegTwoThousandInterpreter.class);

    public static final Integer LOSSY = 0;
    public static final Integer LOSSLESS = 1;

    int myWriterCompressionType = 0;
    int myWriterCompressionValue = 80;

    /************************************************************************************
     * Constructor for {@link JpegTwoThousandInterpreter} to read an jp2 image from given {@link InputStream}
     * 
     * @param inStream {@link InputStream}
     * @throws ImageInterpreterException
     ************************************************************************************/
    public JpegTwoThousandInterpreter(InputStream inStream) throws ImageInterpreterException {
        ImageReader imagereader = null; // ImageReader to read the class
        ImageInputStream iis = null;
        InputStream inputStream = null;

        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("jpeg2000");
        if (it.hasNext()) {
            imagereader = it.next();
        } else {
            // ERROR - no ImageReader was found
            LOGGER.error("Imagereader for Jpeg2000 couldn't be found");
            throw new ImageInterpreterException("Imagereader for Jpeg2000 format couldn't be found!");
        }

        // read the stream and store it in a byte array
        // TODO: This reads the Image into the memory, try to get away with avoiding this, the underlying implementation does it anyway.
        this.readImageStream(inStream);
        byte imagebytes[] = this.getImageByteStream();

        try {
            inputStream = new ByteArraySeekableStream(imagebytes);
        } catch (IOException e1) {
            LOGGER.error("Can't transform the image's byte array to stream");
            ImageInterpreterException iie = new ImageInterpreterException("Can't transform the image's byte array to stream");
            throw iie;
        }

        try {

            // read the stream
            iis = ImageIO.createImageInputStream(inputStream);

            imagereader.setInput(iis, true); // set the ImageInputStream as

            ImageReadParam readParam = imagereader.getDefaultReadParam();
            this.renderedimage = imagereader.readAsRenderedImage(0, readParam); // get
            // the
            // rendered
            // image

        } catch (IOException ioe) {
            LOGGER.error("Can't read JPGS2000 image", ioe);
            throw new ImageInterpreterException("Can't read the input stream", ioe);
        } catch (Exception e) {
            LOGGER.error("something went wrong during reading of JPEG2000 image", e);
            throw new ImageInterpreterException("Something went wrong while reading the JPEG 2000 from input stream", e);
        }

        // get all metadata of the image, color depth etc...
        //
        IIOMetadata imageMetadata = null;
        try {
            imageMetadata = imagereader.getImageMetadata(0);
            // String
            // nativeFormatName=imageMetadata.getNativeMetadataFormatName();
            Node domNode = imageMetadata.getAsTree("javax_imageio_1.0");

            // parse the XML tree

            // get new metadata - this is not very sophisticated parsing the DOM
            // tree - needs to be replaced by
            // XPATH expressions

            // pixel sizes are in mm
            try {
                float hps = Float.parseFloat(this.getHorizontalPixelSize(domNode));
                float vps = Float.parseFloat(this.getVerticalPixelSize(domNode));
                this.setXResolution((int) (25.4 / hps));
                this.setYResolution((int) (25.4 / vps));
            } catch (Exception e) {
                // can't read resolution information,
                // set some default values
                this.setXResolution(100f);
                this.setYResolution(100f);
            }

            // getting colordepths and bitspersample
            //
            String colordepth_str = this.getNumChannels(domNode);
            if (colordepth_str != null) {
                this.colorDepth = Integer.parseInt(colordepth_str);
            }

            String samplesperpixel_str = this.getBitsPerSample(domNode);
            if (samplesperpixel_str != null) {
                // usually it contains information for
                // single channels - e.g.
                // "1 1 1" for bitonal or "8 8 8" for color
                String[] singlechannels = samplesperpixel_str.split(" ");
                if ("1 1 1".equals(singlechannels)) {
                    // it's bitonal
                    this.setSamplesperpixel(1);
                    this.setColordepth(1); // overwrite earlier information
                } else {
                    // it's not bitonal; get the largest number
                    int maxint = 0;
                    for (String singlechannel : singlechannels) {
                        try {
                            int s = Integer.parseInt(singlechannel);
                            if (s > maxint) {
                                maxint = s;
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.trace("Failed to convert string to integer");
                            // nothing happens, just need to catch the exception
                            // inc ase the stinrg value cannot be converted to
                            // an int
                        }
                    } // maxint should contain
                    this.setSamplesperpixel(maxint);
                }
            }

            this.setHeight(this.renderedimage.getHeight()); // set height
            this.setWidth(this.renderedimage.getWidth()); // set width
            //
        } catch (IOException e) {
            LOGGER.error("IOException:" + e);
            LOGGER.error(e);
        }
    }

    /************************************************************************************
     * Constructor for jp2 image from given {@link RenderedImage}
     * 
     * @param inImage the given {@link RenderedImage}
     ************************************************************************************/
    public JpegTwoThousandInterpreter(RenderedImage inImage) {
        // will not set any metadata for this image
        // needs to be done separatly
        this.renderedimage = inImage;
    }

    /**
     * Bytestream from JPEG 2000 file can be embedded into PDF directly without recompression
     */
    @Override
    public boolean pdfBytestreamEmbeddable() {
        return true;
    }

    /************************************************************************************
     * Write the renderedimage to an {@link OutputStream}
     * 
     * @param outStream the {@link OutputStream} to write to
     ************************************************************************************/
    @Override
    public void writeToStream(FileOutputStream fos, OutputStream outStream) {

        if (this.renderedimage == null) { // no image available
            return;
        }
        try {
            ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);

            Iterator<ImageWriter> writerIter = ImageIO.getImageWritersByFormatName("jpeg2000");
            ImageWriter writer = writerIter.next(); // get writer from ImageIO

            // create metadata by creating an XML tree
            //
            BufferedImage image = ImageManipulator.fromRenderedToBuffered(renderedimage);
            J2KImageWriteParam writerParam = (J2KImageWriteParam) writer.getDefaultWriteParam();
            // check compression type
            if (myWriterCompressionType == LOSSLESS) {
                writerParam.setLossless(true);
            } else {
                writerParam.setLossless(false);
                float comprRate = myWriterCompressionValue / 100f;
                writerParam.setEncodingRate(comprRate);
            }
            // ImageWriteParam writerParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel());

            IIOMetadata iomd = writer.getDefaultImageMetadata(its, writerParam);

            // create new XML tree and merge with old
            setMetadata(iomd);

            // set output
            writer.setOutput(imageOutStream);

            IIOImage iioImage = new IIOImage(image, null, iomd);

            writer.write(null, iioImage, writerParam);

            // writer.endWriteSequence();
            imageOutStream.flush();
            if (fos != null) {
                ImageOutputStream imageToFile = ImageIO.createImageOutputStream(fos);
                writer.setOutput(imageToFile);
                writer.write(null, iioImage, writerParam);
                imageToFile.flush();
                imageToFile.close();
                fos.flush();
                fos.close();
            }

            writer.dispose();
            imageOutStream.close();

        } catch (IOException e) {
            LOGGER.error("IOException occured", e);
        }
    }

    @Override
    public void setWriterCompressionType(int type) throws ParameterNotSupportedException {
        if ((type < 0) || (type > 1)) {
            ParameterNotSupportedException pnse = new ParameterNotSupportedException("compression type not supported; unsupported value!");
            throw pnse;
        }
        myWriterCompressionType = type;
    }

    @Override
    public int getWriterCompressionType() throws ParameterNotSupportedException {
        return myWriterCompressionType;
    }

    @Override
    public int getWriterCompressionValue() throws ParameterNotSupportedException {
        return myWriterCompressionValue;
    }

    @Override
    public void setWriterCompressionValue(int value) throws ParameterNotSupportedException {
        if ((value < 0) || (value > 100)) {
            ParameterNotSupportedException pnse = new ParameterNotSupportedException("compression value out of range (10 - 100)!");
            throw pnse;
        }
        myWriterCompressionValue = value;
    }

    /**
     * sets the metadata for the "javax_imageio_1.0" xml tree. A new tree is created an later mergered with the already existing one (the default
     * tree)
     * 
     * @param iomd
     */
    private void setMetadata(IIOMetadata iomd) {
        LOGGER.info("setting metadata for JPEG2000 format");

        // Node topnode = iomd.getAsTree("javax_imageio_1.0");

        // create new tree and merge it with the old one
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = f.newDocumentBuilder();
            Document xmlDocument = db.newDocument();

            Element topElement = xmlDocument.createElement("javax_imageio_1.0");
            Element dimElement = xmlDocument.createElement("Dimension");
            Element hpsElement = xmlDocument.createElement("HorizontalPixelSize");
            Element vpsElement = xmlDocument.createElement("VerticalPixelSize");

            // the size of the pixel is in mm, we have to convert the
            // dpi in pixel sizes
            float xres = (float) 25.4 / this.getXResolution();
            float yres = (float) 25.4 / this.getYResolution();

            LOGGER.info("set X pixel size:" + Float.toString(xres));
            LOGGER.info("set Y pixel size:" + Float.toString(yres));

            // store the resolution

            hpsElement.setAttribute("value", Float.toString(xres));
            vpsElement.setAttribute("value", Float.toString(yres));

            topElement.appendChild(dimElement);
            dimElement.appendChild(hpsElement);
            dimElement.appendChild(vpsElement);

            iomd.mergeTree("javax_imageio_1.0", topElement);
        } catch (Exception e) {
            LOGGER.error("Exception occured", e);
        }
    }

    /*********************************************************
     * 
     * just methods for extracting the metadata from the XML tree
     * 
     *********************************************************/

    /**
     * get the number of channels
     * 
     * @param domNode
     * @return
     */
    private String getNumChannels(Node domNode) {
        String result = null;

        Node chromanode = getFirstElementByName(domNode, "Chroma");
        if (chromanode == null) {
            return null; // markerSequence element not available
        }

        Node numChannelsNods = getFirstElementByName(chromanode, "NumChannels");
        if (numChannelsNods == null) {
            return null; // NumChannels element not available
        }

        Node attribute = getAttributeByName(numChannelsNods, "value");
        if (attribute == null) {
            return null; // attribute not available
        }

        result = attribute.getNodeValue();

        return result;
    }

    /**
     * get the number of channels
     * 
     * @param domNode
     * @return returns a string giving the number of samples for each channel (e.h. "1 1 1" if it's bitonal).
     */
    private String getBitsPerSample(Node domNode) {
        String result = null;

        Node datanode = getFirstElementByName(domNode, "Data");
        if (datanode == null) {
            return null; // markerSequence element not available
        }

        Node bitsperSampleNode = getFirstElementByName(datanode, "BitsPerSample");
        if (bitsperSampleNode == null) {
            return null; // NumChannels element not available
        }

        Node attribute = getAttributeByName(bitsperSampleNode, "value");
        if (attribute == null) {
            return null; // attribute not available
        }

        result = attribute.getNodeValue();

        return result;
    }

    /**
     * get the number of channels
     * 
     * @param domNode
     * @return returns a string giving the number of samples for each channel (e.h. "1 1 1" if it's bitonal).
     */
    private String getHorizontalPixelSize(Node domNode) {
        String result = null;

        Node dimensionnode = getFirstElementByName(domNode, "Dimension");
        if (dimensionnode == null) {
            return null; // markerSequence element not available
        }

        Node horizontalpixelNode = getFirstElementByName(dimensionnode, "HorizontalPixelSize");
        if (horizontalpixelNode == null) {
            return null; // NumChannels element not available
        }

        Node attribute = getAttributeByName(horizontalpixelNode, "value");
        if (attribute == null) {
            return null; // attribute not available
        }

        result = attribute.getNodeValue();

        return result;
    }

    /**
     * get the number of channels
     * 
     * @param domNode
     * @return returns a string giving the number of samples for each channel (e.h. "1 1 1" if it's bitonal).
     */
    private String getVerticalPixelSize(Node domNode) {
        String result = null;

        Node dimensionnode = getFirstElementByName(domNode, "Dimension");
        if (dimensionnode == null) {
            return null; // markerSequence element not available
        }

        Node verticalpixelnode = getFirstElementByName(dimensionnode, "VerticalPixelSize");
        if (verticalpixelnode == null) {
            return null; // NumChannels element not available
        }

        Node attribute = getAttributeByName(verticalpixelnode, "value");
        if (attribute == null) {
            return null; // attribute not available
        }

        result = attribute.getNodeValue();

        return result;
    }

    /************************************************************************************
     * get first node with given name from list of child nodes
     * 
     * @param inNode the parent {@link Node}
     * @param elementName the name we are looking for
     ************************************************************************************/
    private Node getFirstElementByName(Node inNode, String elementName) {
        NodeList list = inNode.getChildNodes();
        int i = 0;
        while (i < list.getLength()) {
            Node n = list.item(i);

            if ((n.getNodeType() == Node.ELEMENT_NODE) && (n.getNodeName().equals(elementName))) {
                return n;
            }
            i++;
        }
        return null;
    }

    /************************************************************************************
     * get attribute with given name from {@link Node}
     * 
     * @param inNode the parent {@link Node}
     * @param attributeName the name we are looking for
     ************************************************************************************/
    private Node getAttributeByName(Node inNode, String attributeName) {
        Node result = null;

        NamedNodeMap nnm = inNode.getAttributes();
        result = nnm.getNamedItem(attributeName);

        return result;
    }

    @Override
    public byte[] writeToStreamAndByteArray(OutputStream outStream) {
        return new byte[0];
    }

    @Override
    public void createByteStreamFromRenderedImage() {
        // TODO Auto-generated method stub

    }
}
