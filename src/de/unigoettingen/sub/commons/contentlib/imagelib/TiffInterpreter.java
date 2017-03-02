/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 *         - http://gdz.sub.uni-goettingen.de 
 *         - http://www.intranda.com 
 *         - http://www.digiverso.com
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
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;

import org.apache.log4j.Logger;

import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.TIFFEncodeParam;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;

/************************************************************************************
 * TiffInterpreter handles Tiff-Images
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class TiffInterpreter extends AbstractImageInterpreter implements ImageInterpreter {
    private static final Logger LOGGER = Logger.getLogger(TiffInterpreter.class);

    public static final int COMPRESSION_NONE = 1;
    public static final int COMPRESSION_CCITTRLE = 2;
    public static final int COMPRESSION_PACKBITS = 32773;
    public static final int COMPRESSION_CCITTFAX3 = 3;
    public static final int COMPRESSION_CCITTFAX4 = 4;
    public static final int COMPRESSION_LZW = 5;
    // private static final int COMPRESSION_OJPEG = 6;
    public static final int COMPRESSION_JPEG = 7;
    // private static final int COMPRESSION_ADOBE_DEFLATE = 8;

    // private static final int COMPRESSION_JP2000 = 34712;
    // private static final int COMPRESSION_DEFLATE = 32946;
    // private static final int COMPRESSION_CCITTRLEW = 32771;

    int writerCompressionType = 0;
    int compressionType = 0;
    ByteArraySeekableStream inputStream = null;

    /**
     * an empty constructor, just used for the extending this class
     */
    public TiffInterpreter() {
        // may be empty
    }

    /************************************************************************************
     * Constructor for {@link TiffInterpreter} to read an tiff image from given {@link InputStream}
     * 
     * @param inStream {@link InputStream}
     * @throws ImageInterpreterException
     ************************************************************************************/
    public TiffInterpreter(InputStream inStream) throws ImageInterpreterException {
        this.read(inStream);
    }

    /************************************************************************************
     * read an tiff image from given {@link InputStream}
     * 
     * @param inStream {@link InputStream}
     * @throws ImageInterpreterException
     ************************************************************************************/
    protected final void read(InputStream inStream) throws ImageInterpreterException {
        ImageReader imagereader = null; // ImageReader to read the class
        // TIFFImageMetadata tim = null; // contains all metadata tags for the TIFF
        ImageInputStream iis = null; // specialized input stream for image
        TIFFDirectory tiffDirectory = null;

        // read the stream and store it in a byte array
        this.readImageStream(inStream);
        byte imagebytes[] = this.getImageByteStream();

        try {
            inputStream = new ByteArraySeekableStream(imagebytes);
        } catch (IOException e1) {
            LOGGER.error("Can't transform the image's byte array to stream");
            throw new ImageInterpreterException("Can't transform the image's byte array to stream");
        }

        // inputStream = SeekableStream.wrapInputStream(inStream, true);

        // get the ImageReader first, before we can read the image
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("tiff");
        if (it.hasNext()) {
            imagereader = it.next();
        } else {
            // ERROR - no ImageReader was found
            LOGGER.error("Imagereader for TIFF couldn't be found");
            throw new ImageInterpreterException("Imagereader for TIFF format couldn't be found!");
        }

        try {
            // read the stream
            iis = ImageIO.createImageInputStream(inputStream);
            imagereader.setInput(iis, true); // set the ImageInputStream as
            tiffDirectory = TIFFDirectory.createFromMetadata(imagereader.getImageMetadata(0));
            // Input for the ImageReader
            // tim = (TIFFImageMetadata) imagereader.getImageMetadata(0);
        } catch (IOException ioe) {
            LOGGER.error("Can't read tiff image", ioe);
            throw new ImageInterpreterException("Can't read the input stream", ioe);
        } catch (Exception e) {
            LOGGER.error("something went wrong during reading of image", e);
            throw new ImageInterpreterException("Something went wrong while reading the TIFF from input stream", e);
        }

        // resolution
        TIFFField tiffield = tiffDirectory.getTIFFField(282); // resolution
        // TIFFField tiffield = tim.getTIFFField(282); // resolution
        xResolution = tiffield.getAsFloat(0);
        yResolution = tiffield.getAsFloat(0);

        // check the resolution unit, if it is in centimeter
        // convert the value to dpi
        tiffield = tiffDirectory.getTIFFField(282);
        // tiffield = tim.getTIFFField(282);
        int res_unit = tiffield.getAsInt(0);
        if (res_unit == 3) { // centimeter; calculate resolution in inch
            // then
            xResolution = (float) (xResolution / 2.54);
            yResolution = (float) (yResolution / 2.54);
        }

        // width
        tiffield = tiffDirectory.getTIFFField(256);
        // tiffield = tim.getTIFFField(256);
        width = tiffield.getAsInt(0);

        // height
        tiffield = tiffDirectory.getTIFFField(257);
        // tiffield = tim.getTIFFField(257);
        height = tiffield.getAsInt(0);

        try {
            // colordepth = bits per sample
            tiffield = tiffDirectory.getTIFFField(258);
            // tiffield = tim.getTIFFField(258);
            colorDepth = tiffield.getAsInt(0);
        } catch (Exception e) {
            // no colordepth information!
            // this means we colordepth and sampler per pixel
            // is 1 = bitonal image
            colorDepth = 1;
        }
        try {
            // samples per pixel
            tiffield = tiffDirectory.getTIFFField(277);
            // tiffield = tim.getTIFFField(277);
            samplesPerPixel = tiffield.getAsInt(0);
        } catch (Exception e) {
            // no samples per pixel information available
            // the default value is 1
            samplesPerPixel = 1;
        }

        // get compression mode
        //
        try {
            tiffield = tiffDirectory.getTIFFField(259);
            // tiffield = tim.getTIFFField(259);
            compressionType = tiffield.getAsInt(0);
        } catch (Exception e) {
            LOGGER.error("Can't read compression type of TIFF", e);
            throw new ImageInterpreterException("Can't read compression type of TIFF", e);
        } finally {
            try {
                iis.close();
                inStream.close();
            } catch (IOException e) {
                LOGGER.error("Error closing input streams: " + e.toString());
            }
        }
    }

    /************************************************************************************
     * Constructor for tiff image from given {@link RenderedImage}
     * 
     * @param inImage the given {@link RenderedImage}
     ************************************************************************************/
    public TiffInterpreter(RenderedImage inImage) {
        // will not set any metadata for this image
        // needs to be done separatley
        this.renderedimage = inImage;
    }

    /**
     * Retrieves the RenderedImage from the input stream used in the constructor.
     * 
     * @return
     */
    @Override
    public RenderedImage getRenderedImage() {

        if ((this.renderedimage == null) && (this.inputStream != null)) {
            // create the renderedimage from stream, if not already done
            this.renderedimage = JAI.create("Stream", this.inputStream);
            try {
                inputStream.close();
            } catch (Exception e) {
                LOGGER.error("safaf");
            }
        }
        return this.renderedimage;
    }

    @Override
    public void createByteStreamFromRenderedImage() {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TIFFEncodeParam encodeParam = new TIFFEncodeParam();
        encodeParam.setCompression(TIFFEncodeParam.COMPRESSION_NONE);
        ParameterBlock block = new ParameterBlock();
        block.addSource(this.renderedimage);
        block.add(bytes);

        block.add("tiff");
        block.add(encodeParam);
        JAI.create("encode", block);
        this.rawbytes = bytes.toByteArray();
    }

    /**
     * Write the renderedimage to an OutputStream
     * 
     * @param outStream
     * @throws ParameterNotSupportedException
     */
    @Override
    public void writeToStream(FileOutputStream fos, OutputStream outStream) {
        if (this.renderedimage == null) { // no image available
            return;
        }

        try {
            isCompressionApplicable(writerCompressionType);
        } catch (ParameterNotSupportedException e1) {
            LOGGER.warn("Can't write Image with compression", e1);
            this.writerCompressionType = COMPRESSION_NONE;
        }

        try {
            ImageWriter iwriter = getWriter();

            // gets a copy of the default writer
            ImageWriteParam wparam = iwriter.getDefaultWriteParam();
            wparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            // String[] compressions=wparam.getCompressionTypes();
            // for (int i=0;i<compressions.length;i++){
            // LOGGER.debug("compressions:"+compressions[i]);
            // }

            if (writerCompressionType == COMPRESSION_NONE) {
                wparam.setCompressionType(null);
            } else if (writerCompressionType == COMPRESSION_LZW) {
                wparam.setCompressionType("LZW");
            } else if (writerCompressionType == COMPRESSION_CCITTFAX4) {
                wparam.setCompressionType("CCITT T.4");
            } else if (writerCompressionType == COMPRESSION_PACKBITS) {
                wparam.setCompressionType("PackBits");
            } else if (writerCompressionType == COMPRESSION_JPEG) {
                wparam.setCompressionType("JPEG");
            } else if (writerCompressionType == COMPRESSION_CCITTRLE) {
                wparam.setCompressionType("CCITT RLE");
            } else {
                LOGGER.warn("Unsupported compression for writing TIFFs");
                wparam.setCompressionType(null);
            }

            // wparam.setCompressionQuality(0.5f); // only used for JPEG

            BufferedImage bi = ImageManipulator.fromRenderedToBuffered(this.renderedimage);

            ImageOutputStream ios = ImageIO.createImageOutputStream(outStream);
            iwriter.setOutput(ios);
            iwriter.write(null, new IIOImage(bi, null, null), wparam);
            ios.flush();
            iwriter.dispose();
            ios.close();
            if (fos != null) {
                ImageOutputStream imageToFile = ImageIO.createImageOutputStream(fos);
                iwriter.setOutput(imageToFile);
                iwriter.write(null, new IIOImage(bi, null, null), wparam);
                imageToFile.flush();
                imageToFile.close();
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            LOGGER.error("IOException occured", e);
        }
    }

    /**
     * Indicates wether the image's bytestream is directly embeddable.
     * 
     * @return
     */
    @Override
    public boolean pdfBytestreamEmbeddable() {
        if ((this.compressionType == COMPRESSION_NONE) || (this.compressionType == COMPRESSION_LZW)
                || (this.compressionType == COMPRESSION_CCITTFAX4)) {
            return true;
        }
        return false;
    }

    @Override
    public void setWriterCompressionType(int inWriterCompressionType) throws ParameterNotSupportedException {
        isCompressionApplicable(inWriterCompressionType); // throws exception if
        // not applicable
        writerCompressionType = inWriterCompressionType;
    }

    private void isCompressionApplicable(int inWriterCompressionType) throws ParameterNotSupportedException {
        if ((inWriterCompressionType != COMPRESSION_NONE) && (inWriterCompressionType != COMPRESSION_LZW)
                && (inWriterCompressionType != COMPRESSION_CCITTFAX4) && (inWriterCompressionType != COMPRESSION_PACKBITS)
                && (inWriterCompressionType != COMPRESSION_JPEG) && (inWriterCompressionType != COMPRESSION_CCITTRLE)) {
            throw new ParameterNotSupportedException("Compression type is not supported");
        }

        // only applicable for black and white
        if ((inWriterCompressionType == COMPRESSION_CCITTFAX4) && ((this.getSamplesperpixel() != 1) || (this.getColordepth() != 1))) {
            throw new ParameterNotSupportedException("Compression type is not supported for this image; only for bitonal images");
        }

        // only applicable for black and white
        if ((inWriterCompressionType == COMPRESSION_CCITTRLE) && ((this.getSamplesperpixel() != 1) || (this.getColordepth() != 1))) {
            throw new ParameterNotSupportedException("Compression type is not supported for this image; only for bitonal images");
        }

    }

    protected ImageWriter getWriter() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tif");
        ImageWriter writer = writers.next();
        return writer;
    }

    @Override
    public byte[] writeToStreamAndByteArray(OutputStream outStream) {
        return new byte[0];
    }

}
