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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
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
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import magick.MagickException;
import magick.MagickImage;

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.sun.imageio.plugins.jpeg.JPEGImageWriter;
import com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi;
import com.sun.media.jai.codec.ByteArraySeekableStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;

/************************************************************************************
 * JpegInterpreter handles Jpeg-Images
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class JpegInterpreter extends AbstractImageInterpreter implements ImageInterpreter {
    private static final Logger LOGGER = Logger.getLogger(JpegInterpreter.class);

    int defaultXResolution = 100;
    int defaultYResolution = 100;
    int writerCompressionValue = 80;

    /************************************************************************************
     * Constructor for {@link JpegInterpreter} to read an jpeg image from given {@link InputStream}
     * 
     * @param inStream {@link InputStream}
     * @throws ImageInterpreterException
     ************************************************************************************/
    public JpegInterpreter(InputStream inStream) throws ImageInterpreterException {
        InputStream inputStream = null;
        byte imagebytes[] = null;

        // read the stream and store it in a byte array
        try {
            this.readImageStream(inStream);
            imagebytes = this.getImageByteStream();
            if (inStream != null) {
                inStream.close();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to close input stream", e);
        }
        //

        // Read image bytes into new stream
        try {
            inputStream = new ByteArraySeekableStream(imagebytes);
        } catch (IOException e1) {
            LOGGER.error("Can't transform the image's byte array to stream");
            ImageInterpreterException iie = new ImageInterpreterException("Can't transform the image's byte array to stream");
            throw iie;
        }
        //

        // read the stream
        Node domNode = null;
        try {
            IIOImage image = createImage(inputStream, 0);
            if ((image == null) || (image.getRenderedImage() == null)) {
                LOGGER.error("Failed to read image from input stream. Aborting!");
                ImageInterpreterException iie = new ImageInterpreterException("Failed to read image from input stream. Aborting!");
                throw iie;
            }
            this.renderedimage = image.getRenderedImage();
            IIOMetadata metadata = image.getMetadata();
            if (metadata != null) {
                String formatName = metadata.getNativeMetadataFormatName();
                domNode = metadata.getAsTree(formatName);
                if ((domNode == null) || (domNode.getChildNodes() == null)) {
                    metadata = null;
                }
            }
            if (metadata == null) {
                LOGGER.error("Failed to read metadata from input stream. Using default values");
                xResolution = defaultXResolution;
                yResolution = defaultYResolution;
                width = this.renderedimage.getWidth();
                height = this.renderedimage.getHeight();
                samplesPerPixel = 1;
                return;
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("Failed to close input stream");
                }
            }
        }
        //

        // get new metadata - this is not very sophisticated parsing the DOM
        // tree - needs to be replaced by
        // XPATH expressions - see above
        String height_str = this.getNumLines(domNode);
        if (height_str != null) {
            this.height = Integer.parseInt(height_str);
        }

        String width_str = this.getSamplesPerLine(domNode);
        if (width_str != null) {
            this.width = Integer.parseInt(width_str);
        }

        String resunits = this.getResUnits(domNode);
        int resunits_int = 1; // default is DPI
        // if resunits==1 than it is dpi,
        // if resunits==2 than it is dpcm
        if (resunits != null) {
            resunits_int = Integer.parseInt(resunits);
        }

        String xres_str = this.getXdensity(domNode);
        if (xres_str != null) {
            this.xResolution = Integer.parseInt(xres_str);
            if (resunits_int == 2) {
                this.xResolution = this.xResolution * 2.54f; // d/inch = d/cm * cm/inch
                // this.xResolution = this.xResolution / 2.54f;
            }
        }

        String yres_str = this.getYdensity(domNode);
        if (yres_str != null) {
            this.yResolution = Integer.parseInt(yres_str);
            if (resunits_int == 2) {
                this.yResolution = this.yResolution * 2.54f; // d/inch = d/cm * cm/inch
                // this.yResolution = this.yResolution / 2.54f;
            }
        }

        if ((resunits == null) || (resunits_int == 0) || (xResolution <= 1.0) || (yResolution <= 1.0)) {
            xResolution = defaultXResolution;
            yResolution = defaultYResolution;
        }

        String colordepth_str = this.getSamplePrecision(domNode);
        if (colordepth_str != null) {
            this.colorDepth = Integer.parseInt(colordepth_str);
        }

        String samplesperpixel_str = this.getNumFrames(domNode);
        if (samplesperpixel_str != null) {
            this.samplesPerPixel = Integer.parseInt(samplesperpixel_str);
        }
    }

    @SuppressWarnings("unused")
    private void getImageMetadataFromIM(MagickImage image) throws MagickException {

        // MagickImage image = new MagickImage(info);

        LOGGER.trace("Getting image resolution");
        int units = image.getUnits();
        LOGGER.debug("Image resolutionUnits = " + units);
        this.xResolution = (float) image.getXResolution();
        this.yResolution = (float) image.getYResolution();
        if ((units == 0) || (xResolution <= 1.0) || (yResolution <= 1.0)) {
            xResolution = defaultXResolution;
            yResolution = defaultYResolution;
        }
        LOGGER.debug("Image resolution = " + xResolution + "/" + yResolution);

        LOGGER.trace("Getting image dimensions");
        Dimension dim = image.getDimension();
        this.width = dim.height;
        this.height = dim.width;
        LOGGER.debug("Image size = " + width + "x" + height);

        LOGGER.trace("Getting image colordepth");
        this.colorDepth = image.getDepth();
        LOGGER.debug("Image colordepth = " + colorDepth);

        LOGGER.trace("Getting number of frames");
        this.samplesPerPixel = image.getNumFrames();
        LOGGER.debug("Number of Frames / Samples per Pixel = " + samplesPerPixel);
    }

    /************************************************************************************
     * Constructor for jpeg image from given {@link RenderedImage}
     * 
     * @param inImage the given {@link RenderedImage}
     ************************************************************************************/
    public JpegInterpreter(RenderedImage inImage) {
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
            // create a buffered Image, which has no Alpha channel
            // as JPEG does not support Alpha Channels and the
            // ImageIO doesn't care - but will create a corrupt JPEG
            BufferedImage noAlphaBi = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedimage);
            ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);

            // Iterator<ImageWriter> writerIter = ImageIO
            // .getImageWritersByFormatName("jpg");
            // ImageWriter writer = writerIter.next(); // get writer from ImageIO
            ImageWriter writer = new JPEGImageWriter(new JPEGImageWriterSpi());

            // create metadata by creating an XML tree
            ImageWriteParam writerParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(noAlphaBi);

            // ImageTypeSpecifier its = new
            // ImageTypeSpecifier(image.getColorModel(),
            // image.getSampleModel());

            // IIOMetadata iomd = writer.getDefaultImageMetadata(new
            // ImageTypeSpecifier(image), writerParam);
            // Element tree =
            // (Element)iomd.getAsTree("javax_imageio_jpeg_image_1.0");
            // Element tree = (Element)iomd.getAsTree("javax_imageio_1.0");
            //
            IIOMetadata iomd = writer.getDefaultImageMetadata(its, writerParam);

            // create the XML tree and modify the appropriate DOM elements
            // to set the metadata
            setMetadata(iomd);

            // set compression
            writerParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            float comprvalue = ((float) writerCompressionValue) / 100;
            writerParam.setCompressionQuality(comprvalue);

            // set output
            writer.setOutput(imageOutStream);
            writer.prepareWriteSequence(null);

            // create new image parameters to set the compression
            // Locale locale = new Locale("en");
            // JPEGImageWriteParam jpegWriteParam = new
            // JPEGImageWriteParam(locale);

            // IIOImage iioImage = new IIOImage(renderedimage, null, iomd);

            IIOImage iioImage = new IIOImage(noAlphaBi, null, iomd);
            writer.write(null, iioImage, writerParam);
            writer.endWriteSequence();
            imageOutStream.flush();

            if (fos != null) {
                ImageOutputStream imageToFile = ImageIO.createImageOutputStream(fos);
                writer.setOutput(imageToFile);
                writer.prepareWriteSequence(null);
                writer.write(null, iioImage, writerParam);
                writer.endWriteSequence();
                imageToFile.flush();
                imageToFile.close();
                fos.flush();
                fos.close();
            }

            // ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // byte [] data = wi.getImageAsByteArray();

            writer.dispose();
            imageOutStream.close();

        } catch (IOException e) {
            LOGGER.error("IOException occured", e);
        }
    }

    @Override
    public byte[] writeToStreamAndByteArray(OutputStream outStream) {
        byte[] data = null;
        if (this.renderedimage == null) { // no image available
            return data;
        }
        try {
            // create a buffered Image, which has no Alpha channel
            // as JPEG does not support Alpha Channels and the
            // ImageIO doesn't care - but will create a corrupt JPEG
            BufferedImage noAlphaBi = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedimage);
            ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);

            // Iterator<ImageWriter> writerIter = ImageIO
            // .getImageWritersByFormatName("jpg");
            // ImageWriter writer = writerIter.next(); // get writer from ImageIO
            ImageWriter writer = new JPEGImageWriter(new JPEGImageWriterSpi());

            // create metadata by creating an XML tree
            ImageWriteParam writerParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(noAlphaBi);

            // ImageTypeSpecifier its = new
            // ImageTypeSpecifier(image.getColorModel(),
            // image.getSampleModel());

            // IIOMetadata iomd = writer.getDefaultImageMetadata(new
            // ImageTypeSpecifier(image), writerParam);
            // Element tree =
            // (Element)iomd.getAsTree("javax_imageio_jpeg_image_1.0");
            // Element tree = (Element)iomd.getAsTree("javax_imageio_1.0");
            //
            IIOMetadata iomd = writer.getDefaultImageMetadata(its, writerParam);

            // create the XML tree and modify the appropriate DOM elements
            // to set the metadata
            setMetadata(iomd);

            // set compression
            writerParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            float comprvalue = ((float) writerCompressionValue) / 100;
            writerParam.setCompressionQuality(comprvalue);

            // set output
            writer.setOutput(imageOutStream);
            writer.prepareWriteSequence(null);

            // create new image parameters to set the compression
            // Locale locale = new Locale("en");
            // JPEGImageWriteParam jpegWriteParam = new
            // JPEGImageWriteParam(locale);

            // IIOImage iioImage = new IIOImage(renderedimage, null, iomd);

            IIOImage iioImage = new IIOImage(noAlphaBi, null, iomd);
            writer.write(null, iioImage, writerParam);
            writer.endWriteSequence();
            imageOutStream.flush();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageOutputStream imageToFile = ImageIO.createImageOutputStream(baos);
            writer.setOutput(imageToFile);
            writer.prepareWriteSequence(null);
            writer.write(null, iioImage, writerParam);
            writer.endWriteSequence();
            imageToFile.flush();
            imageToFile.close();
            baos.flush();
            data = baos.toByteArray();
            baos.close();
            writer.dispose();
            imageOutStream.close();
        } catch (IOException e) {
            LOGGER.error("IOException occured", e);
        }
        return data;
    }

    /************************************************************************************
     * set metadata to image
     * 
     * @param iomd the given {@link IIOMetadata} to set
     ************************************************************************************/
    private void setMetadata(IIOMetadata iomd) {

        Node node = iomd.getAsTree("javax_imageio_jpeg_image_1.0");

        // set dimensions
        setSamplesPerLine(node, this.getWidth());
        setNumLines(node, this.getHeight());

        // what are child nodes?
        NodeList nl = node.getChildNodes();
        for (int j = 0; j < nl.getLength(); j++) {
            Node n = nl.item(j);

            if (n.getNodeName().equals("JPEGvariety")) {
                NodeList childNodes = n.getChildNodes();

                for (int k = 0; k < childNodes.getLength(); k++) {
                    if (childNodes.item(k).getNodeName().equals("app0JFIF")) {

                        // get the attributes resUnits, Xdensity, and Ydensity
                        Node resUnitsNode = getAttributeByName(childNodes.item(k), "resUnits");
                        Node XdensityNode = getAttributeByName(childNodes.item(k), "Xdensity");
                        Node YdensityNode = getAttributeByName(childNodes.item(k), "Ydensity");
                        // overwrite values for that node
                        resUnitsNode.setNodeValue("1"); // it's dpi

                        int xres = (int) this.getXResolution();
                        int yres = (int) this.getYResolution();
                        if (xres == 0) {
                            xres = defaultXResolution;
                        }
                        if (yres == 0) {
                            yres = defaultYResolution;
                        }
                        XdensityNode.setNodeValue(String.valueOf(xres));
                        YdensityNode.setNodeValue(String.valueOf(yres));

                    } // endif
                } // end id
                break; // don't need to change the other children
            } // end id

        } // end for

        // set the XML tree for the IIOMetadata object
        try {
            iomd.setFromTree("javax_imageio_jpeg_image_1.0", node);
        } catch (IIOInvalidTreeException e) {
            LOGGER.error(e); // To change body of catch statement
        }

    }

    /**
     * Indicates wether the image's bytestream is directly embeddable. jpegs are always embeddable
     * 
     * @return true if pdf bytes are embeddable
     */
    @Override
    public boolean pdfBytestreamEmbeddable() {
        return true;
    }

    /************************************************************************************
     * get numLines from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getNumLines(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "numLines");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    private void setNumLines(Node domNode, int lines) {
        Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
        if (markerSequenceNode == null) {
            markerSequenceNode = new IIOMetadataNode("markerSequence");
            domNode.appendChild(markerSequenceNode);
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
        if (sofNode == null) {
            sofNode = new IIOMetadataNode("sof");
            markerSequenceNode.appendChild(sofNode);
        }

        Node attribute = getAttributeByName(sofNode, "numLines");
        if (attribute == null) {
            attribute = new IIOMetadataNode("numLines");
            sofNode.appendChild(attribute);
        }
        attribute.setNodeValue(Integer.toString(lines));
    }

    /************************************************************************************
     * get samplesPerLine from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getSamplesPerLine(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "samplesPerLine");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    private void setSamplesPerLine(Node domNode, int samples) {
        Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
        if (markerSequenceNode == null) {
            markerSequenceNode = new IIOMetadataNode("markerSequence");
            domNode.appendChild(markerSequenceNode);
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
        if (sofNode == null) {
            sofNode = new IIOMetadataNode("sof");
            markerSequenceNode.appendChild(sofNode);
        }

        Node attribute = getAttributeByName(sofNode, "samplesPerLine");
        if (attribute == null) {
            attribute = new IIOMetadataNode("samplesPerLine");
            sofNode.appendChild(attribute);
        }
        attribute.setNodeValue(Integer.toString(samples));
    }

    /************************************************************************************
     * get Xdensity from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getXdensity(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "JPEGvariety");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "app0JFIF");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "Xdensity");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    /************************************************************************************
     * get Ydensity from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getYdensity(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "JPEGvariety");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "app0JFIF");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "Ydensity");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    /************************************************************************************
     * get resUnits from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getResUnits(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "JPEGvariety");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "app0JFIF");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "resUnits");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    /************************************************************************************
     * get sample precision from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getSamplePrecision(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "samplePrecision");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    /************************************************************************************
     * get number of frame components from {@link Node}
     * 
     * @param domNode given {@link Node}
     ************************************************************************************/
    private String getNumFrames(Node domNode) {
        Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
        if (markerSequenceNode == null) {
            return null; // markerSequence element not available
        }

        Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
        if (sofNode == null) {
            return null; // sof element not available
        }

        Node attribute = getAttributeByName(sofNode, "numFrameComponents");
        if (attribute == null) {
            return null; // attribute not available
        }

        return attribute.getNodeValue();
    }

    /************************************************************************************
     * get Dom {@link Node} from parent {@link Node} with given name
     * 
     * @param inNode the parent {@link Node}
     * @param elementName the name of the Node to look for
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
    public void setWriterCompressionValue(int inWriterCompressionValue) throws ParameterNotSupportedException {
        if ((inWriterCompressionValue < 0) || (inWriterCompressionValue > 100)) {
            ParameterNotSupportedException pnse = new ParameterNotSupportedException("Value for JPEG compression must be between 0 and 100");
            throw pnse;
        }
        writerCompressionValue = inWriterCompressionValue;
    }

    /** Patches a JPEG file that is missing a JFIF marker **/
    private static class PatchInputStream extends FilterInputStream {
        private static final int[] JFIF = { 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x02, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
                0x00 };
        int position = 0;

        public PatchInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int result;
            if (position < 2) {
                result = in.read();
            } else if (position < (2 + JFIF.length)) {
                result = JFIF[position - 2];
            } else {
                result = in.read();
            }
            position++;
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int max = off + len;
            int bytesread = 0;
            for (int i = off; i < max; i++) {
                final int bi = read();
                if (bi == -1) {
                    if (bytesread == 0) {
                        bytesread = -1;
                    }
                    break;
                } else {
                    b[i] = (byte) bi;
                    bytesread++;
                }
            }
            return bytesread;
        }
    }

    /** Remove the bytes between the file init and the JFIF marker **/
    private static class RemoveHeaderInputStream extends FilterInputStream {

        // Header with the correct bytes (no more than needed)
        private static final int[] JFIF = { 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46 };

        int position = 0;
        int positionJFIF = 0;

        public RemoveHeaderInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int result;
            if (position < 2) {
                result = in.read();

                // Seek for the header until the end of file
            } else if (positionJFIF < JFIF.length) {

                // Advance positions until JFIF is found
                while ((result = in.read()) != JFIF[positionJFIF]) {
                    position++;
                }

                // Una vez
                positionJFIF++;

            } else {
                result = in.read();
            }
            position++;
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int max = off + len;
            int bytesread = 0;
            for (int i = off; i < max; i++) {
                final int bi = read();
                if (bi == -1) {
                    if (bytesread == 0) {
                        bytesread = -1;
                    }
                    break;
                } else {
                    b[i] = (byte) bi;
                    bytesread++;
                }
            }
            return bytesread;
        }
    }

    private IIOImage createImage(InputStream istr, int attempt) throws ImageInterpreterException {
        ImageInputStream iis = null;
        Iterator<ImageReader> ri = null;
        ImageReadParam param = null;
        ImageReader ir = null;
        BufferedImage bi = null;
        // Raster raster = null;
        // int[] bufferTypes = new int[] { DataBuffer.TYPE_BYTE, DataBuffer.TYPE_INT, DataBuffer.TYPE_FLOAT, DataBuffer.TYPE_DOUBLE,
        // DataBuffer.TYPE_SHORT, DataBuffer.TYPE_USHORT, DataBuffer.TYPE_UNDEFINED };

        // Create raster from image reader
        try {
            iis = ImageIO.createImageInputStream(istr);
            ri = ImageIO.getImageReaders(iis);
            if (!ri.hasNext()) {
                // List<ImageReader> list = new ArrayList<ImageReader>();
                // list.add(new JPEGImageReader(new JPEGImageReaderSpi()));
                ri = IteratorUtils.getIterator(new JPEGImageReader(new JPEGImageReaderSpi()));
            }
        } catch (IOException e) {
            throw new ImageInterpreterException("Error reading input stream: " + e.toString());
        }
        while (ri.hasNext()) {
            ir = ri.next();
            try {
                ir.setInput(iis);
                param = ir.getDefaultReadParam();
                bi = ir.read(0, param);
                // raster = ir.readRaster(0, param);
                if (bi != null) {
                    break;
                }
            } catch (Error e) {
                LOGGER.error("Failed to render image with ImageReader: " + e.toString());
                continue;
            } catch (Exception e) {
                LOGGER.error("Failed to render image with ImageReader: " + e.toString());
                continue;
            }
        }

        // store metadata
        IIOMetadata md = null;
        try {
            md = getImageMetadata(ir);
        } catch (ImageInterpreterException e) {
            LOGGER.error("Failed to extract metadata from image: " + e.getMessage());
            InputStream patchedInputStream = null;
            if (attempt <= 1) {
                patchedInputStream = attempt == 0 ? new PatchInputStream(istr) : new RemoveHeaderInputStream(istr);
                if (istr != null) {
                    try {
                        istr.close();
                    } catch (IOException e1) {
                        LOGGER.error("Failed to close input stream");
                    }
                }
                return createImage(patchedInputStream, attempt + 1);
            } else {
                LOGGER.error("Unable to read image metadata.");
                md = null;
            }
        } finally {
            if (iis != null) {
                try {
                    iis.close();
                } catch (IOException e) {
                    LOGGER.error("Failed to close image stream", e);
                }
            }
        }

        // // create buffered image from raster
        // int count = 0;
        // while (count < bufferTypes.length) {
        // int bufferType = bufferTypes[count++];
        // try {
        // SampleModel sm = RasterFactory.createPixelInterleavedSampleModel(bufferType, raster.getWidth(), raster.getHeight(),
        // raster.getNumBands());
        // ColorModel cm = PlanarImage.createColorModel(sm);
        // // cm = bi.getColorModel();
        // ColorSpace sourceColorSpace = cm.getColorSpace();
        // ColorSpace destColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        // ColorConvertOp op = new ColorConvertOp(sourceColorSpace, destColorSpace, null);
        // // WritableRaster wraster = raster.createInterleavedRaster(bufferTypes[count-1], raster.getWidth(), raster.getHeight(),
        // raster.getNumBands(), null);
        // // WritableRaster wraster = op.createCompatibleDestRaster(raster);
        // WritableRaster wraster = op.filter(raster, null);
        // // Raster raster2 = bi.getRaster();
        // // cm = new ComponentColorModel(cs, false, false, ColorModel.OPAQUE, bufferType);
        // // sm = bi.getSampleModel();
        // // cm = bi.getColorModel();
        // // cm = ColorModel.getRGBdefault();
        // bi = new BufferedImage(cm, wraster, false, null);
        // cm.finalize();
        // } catch (Error e) {
        // LOGGER.debug("Failed to render image with BufferType " + bufferType);
        // continue;
        // } catch (Exception e) {
        // LOGGER.debug("Failed to render image with BufferType " + bufferType);
        // continue;
        // }
        // break;
        // }

        if (bi == null) {
            throw new ImageInterpreterException("Failed to extract buffered image from image reader");
        }
        IIOImage image = new IIOImage(bi, null, md);
        return image;
    }

    private IIOMetadata getImageMetadata(ImageReader ir) throws ImageInterpreterException {
        IIOMetadata md = null;
        Node treeNode = null;
        try {
            md = ir.getImageMetadata(0);
            String formatName = md.getNativeMetadataFormatName();
            treeNode = md.getAsTree(formatName);
        } catch (Error e) {
            throw new ImageInterpreterException("Error when attempting to parse image metadata: " + e.toString());
        } catch (Exception e) {
            throw new ImageInterpreterException("Error when attempting to parse image metadata: " + e.toString());
        }
        if ((treeNode == null) || (treeNode.getChildNodes() == null)) {
            throw new ImageInterpreterException("Image metadata Node is null or empty");
        }
        return md;
    }

    @Override
    public void createByteStreamFromRenderedImage() {
        // TODO Auto-generated method stub

    }
}
