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
import java.io.ByteArrayOutputStream;
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
import javax.imageio.ImageWriteParam;
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

import com.sun.imageio.plugins.png.PNGImageReader;
import com.sun.media.jai.codec.ByteArraySeekableStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;

/************************************************************************************
 * PngInterpreter handles Png-Images
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class PngInterpreter extends AbstractImageInterpreter implements ImageInterpreter {
    private static final Logger LOGGER = Logger.getLogger(PngInterpreter.class);

    /************************************************************************************
     * Constructor for {@link PngInterpreter} to read an png image from given {@link InputStream}
     * 
     * @param inStream {@link InputStream}
     * @throws ImageInterpreterException
     ************************************************************************************/
    public PngInterpreter(InputStream inStream) throws ImageInterpreterException {
        PNGImageReader imagereader = null; // ImageReader to read the class
        ImageInputStream iis = null;
        InputStream inputStream = null;

        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("png");
        if (it.hasNext()) {
            imagereader = (PNGImageReader) it.next();
        } else {
            // ERROR - no ImageReader was found
            LOGGER.error("Imagereader for PNG couldn't be found");
            throw new ImageInterpreterException("Imagereader for PNG format couldn't be found!");
        }

        // read the stream and store it in a byte array
        this.readImageStream(inStream);
        byte imagebytes[] = this.getImageByteStream();

        try {
            inputStream = new ByteArraySeekableStream(imagebytes);
        } catch (IOException e1) {
            LOGGER.error("Can't transform the image's byte array to stream");
            ImageInterpreterException iie = new ImageInterpreterException("Can't transform the image's byte array to stream");
            throw iie;
        }

        // inputStream=inStream;

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
            LOGGER.error("Can't read png image", ioe);
            throw new ImageInterpreterException("Can't read the input stream", ioe);
        } catch (Exception e) {
            LOGGER.error("something went wrong during reading of png image", e);
            throw new ImageInterpreterException("Something went wrong while reading the PNG from input stream", e);
        }

        // get all metadata of the image, color depth etc...
        //
        IIOMetadata imageMetadata = null;
        try {
            imageMetadata = imagereader.getImageMetadata(0);
        } catch (IOException e) {
            LOGGER.error("IOException:" + e);
            LOGGER.error(e);
        }
        Node domNode = imageMetadata.getAsTree("javax_imageio_1.0");

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
            if (singlechannels.equals("1 1 1")) {
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
                    } catch (Exception e) {
                        // nothing happens, just need to catch the exception
                        // inc ase the stinrg value cannot be converted to
                        // an int
                    }
                } // maxint should contain
                this.setSamplesperpixel(maxint);
            }
        }

        this.setHeight(this.renderedimage.getHeight()); // set height
        this.setWidth(this.renderedimage.getWidth()); // set width }
    }

    /************************************************************************************
     * Constructor for png image from given {@link RenderedImage}
     * 
     * @param inImage the given {@link RenderedImage}
     ************************************************************************************/
    public PngInterpreter(RenderedImage inImage) {
        // will not set any metadata for this image
        // needs to be done separatly
        this.renderedimage = inImage;
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

            Iterator<ImageWriter> writerIter = ImageIO.getImageWritersByFormatName("png");
            ImageWriter writer = writerIter.next(); // get writer from ImageIO

            // create metadata by creating an XML tree
            BufferedImage image = ImageManipulator.fromRenderedToBuffered(renderedimage);
            ImageWriteParam writerParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel());

            IIOMetadata iomd = writer.getDefaultImageMetadata(its, writerParam);

            // create new XML tree and merge with old
            setMetadata(iomd);

            // set output
            writer.setOutput(imageOutStream);

            IIOImage iioImage = new IIOImage(image, null, iomd);

            writer.write(iioImage);

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

            // ImageIO.write(renderedimage, "JPEG ", outStream);
        } catch (IOException e) {
            LOGGER.error("IOException:" + e);
        }
    }

    /************************************************************************************
     * sets the metadata for the "javax_imageio_1.0" xml tree. A new tree is created an later mergered with the already existing one (the default
     * tree)
     * 
     * @param iomd the given {@link IIOMetadata} to write
     ************************************************************************************/
    private void setMetadata(IIOMetadata iomd) {
        LOGGER.debug("setting metadata for PNG format");
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

            LOGGER.debug("set X pixel size:" + Float.toString(xres));
            LOGGER.debug("set Y pixel size:" + Float.toString(yres));

            // in order to store the right value we must not store the
            // size of the pixel, but 1 divided by the size of the
            // pixel the value attributes. Only then we the right values
            // of xres and yres are stored
            //
            // I wonder if this is a bug in ImageIO's PNG library

            hpsElement.setAttribute("value", Float.toString(1 / xres));
            vpsElement.setAttribute("value", Float.toString(1 / yres));

            topElement.appendChild(dimElement);
            dimElement.appendChild(hpsElement);
            dimElement.appendChild(vpsElement);

            iomd.mergeTree("javax_imageio_1.0", topElement);
        } catch (Exception e) {
            LOGGER.error("Exception occured", e);
        }
    }

    /**
     * @return the height
     */
    @Override
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the colordepth
     */
    @Override
    public int getColordepth() {
        return this.colorDepth;
    }

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

    /**
     * helper methods to parse the DOM tree getting a element and an attribute node
     */
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
     * get Dom {@link Node} of attribute from parent {@link Node} with given name
     * 
     * @param inNode the parent {@link Node}
     * @param attributeName the name of the attribute to look for
     ************************************************************************************/
    private Node getAttributeByName(Node inNode, String attributeName) {
        NamedNodeMap nnm = inNode.getAttributes();
        return nnm.getNamedItem(attributeName);
    }

    @Override
    public byte[] writeToStreamAndByteArray(OutputStream outStream) {
        if (this.renderedimage == null) { // no image available
            return null;
        }

        // byte[] data = null;
        try {
            ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);

            Iterator<ImageWriter> writerIter = ImageIO.getImageWritersByFormatName("png");
            ImageWriter writer = writerIter.next(); // get writer from ImageIO

            // create metadata by creating an XML tree
            BufferedImage image = ImageManipulator.fromRenderedToBuffered(renderedimage);
            ImageWriteParam writerParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel());

            IIOMetadata iomd = writer.getDefaultImageMetadata(its, writerParam);

            // create new XML tree and merge with old
            setMetadata(iomd);

            // set output
            writer.setOutput(imageOutStream);

            IIOImage iioImage = new IIOImage(image, null, iomd);

            writer.write(iioImage);

            // writer.endWriteSequence();
            imageOutStream.flush();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (baos != null) {
                ImageOutputStream imageToBytes = ImageIO.createImageOutputStream(baos);
                writer.setOutput(imageToBytes);
                writer.write(null, iioImage, writerParam);
                imageToBytes.flush();
                imageToBytes.close();
                baos.flush();
                baos.close();
            }
            writer.dispose();
            imageOutStream.close();

            return baos.toByteArray();

            // ImageIO.write(renderedimage, "JPEG ", outStream);
        } catch (IOException e) {
            LOGGER.error("IOException:" + e);
        }
        return null;
    }

    @Override
    public void createByteStreamFromRenderedImage() {
        // TODO Auto-generated method stub

    }

}
