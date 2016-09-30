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
package de.unigoettingen.sub.commons.contentlib.imagelib.magick;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;

import org.apache.log4j.Logger;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator.MergingMode;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;

/************************************************************************************
 * Image Manager derivative using ImageMagick for all kinds of image handlings, wraps all functionalities of the {@link ImageManipulator}
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class ImageManager {
    private static final Logger LOGGER = Logger.getLogger(ImageManager.class);

    OutputStream outputStream = null;
    MagickImage mImage = null;
    ImageInfo imageInfo = null;

    int defaultXResolution = 100;
    int defaultYResolution = 100;
    int writerCompressionValue = 80;

    public static final Integer SCALE_BY_WIDTH = 1;
    public static final Integer SCALE_BY_HEIGHT = 2;
    public static final Integer SCALE_BY_PERCENT = 3;
    public static final Integer SCALE_BY_RESOLUTION = 4;
    public static final Integer SCALE_TO_BOX = 5;

    public static final Integer DPI_DEFAULT = 100;
    public static final Integer DPI_DISPLAY = 72;
    public static final Integer DPI_PRINT = 600;

    public static final Integer IMAGE_ROTATION_0 = 0;
    public static final Integer IMAGE_ROTATION_90 = 90;
    public static final Integer IMAGE_ROTATION_180 = 180;
    public static final Integer IMAGE_ROTATION_270 = 270;

    Map<Integer, Integer> parameters = null;

    // possible values for scaling parameters
    public static final Integer SUBSAMPLETOGREY = 1;
    public static final Integer BILINEAR = 2;
    public static final Integer NEXTNEIGHBOR = 3;

    // possible parameters "BITONALSCALEMETHOD", "GREYSCALESCALEMETHOD",
    // "COLORSCALEMETHOD"
    public static final Integer BITONALSCALEMETHOD = 1;
    public static final Integer GREYSCALESCALEMETHOD = 2;
    public static final Integer COLORSCALEMETHOD = 3;

    public static final Integer TOP = 1;
    public static final Integer BOTTOM = 2;
    public static final Integer RIGHT = 3;
    public static final Integer LEFT = 4;

    private String httpproxyhost = null;
    private String httpproxyport = null;
    private String httpproxyuser = null;
    private String httpproxypassword = null;

    /************************************************************************************
     * simple Constructor for given {@link URL}
     * 
     * @param url the URL to use
     ************************************************************************************/
    public ImageManager(URL url) throws ImageManagerException {
        System.setProperty("jmagick.systemclassloader", "false");
        init(url, this.httpproxyhost, this.httpproxyport, this.httpproxyuser, this.httpproxypassword);
    }

    /**
     * @param url
     * @param httpproxyhost
     * @param httpproxyport
     * @param httpproxyuser
     * @param httpproxypassword
     * @throws ImageManagerException
     */
    public ImageManager(URL url, String httpproxyhost, String httpproxyport, String httpproxyuser, String httpproxypassword)
            throws ImageManagerException {
        init(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
    }

    /**
     * @param url
     * @param httpproxyhost
     * @param httpproxyport
     * @param httpproxyuser
     * @param httpproxypassword
     * @throws ImageManagerException
     */
    protected final void init(URL url, String httpproxyhost, String httpproxyport, String httpproxyuser, String httpproxypassword)
            throws ImageManagerException {
        try {
            LOGGER.trace("image request url = " + url);
            File imageFile = new File(url.toURI());
            if (!imageFile.isFile()) {
                throw new IOException();
            }
            imageInfo = new ImageInfo(imageFile.getAbsolutePath());
            mImage = new MagickImage(imageInfo);
        } catch (MagickException e) {
            LOGGER.error("Error while getting MagickImage", e);
            throw new ImageManagerException(e);
        } catch (URISyntaxException e) {
            LOGGER.error("Error reading url", e);
            throw new ImageManagerException(e);
        } catch (IOException e) {
            LOGGER.error("Error loading image file", e);
            throw new ImageManagerException(e);
        }

        this.parameters = new HashMap<Integer, Integer>();
        this.parameters.put(BITONALSCALEMETHOD, SUBSAMPLETOGREY);
        this.parameters.put(GREYSCALESCALEMETHOD, BILINEAR);
        this.parameters.put(COLORSCALEMETHOD, NEXTNEIGHBOR);
    }

    /*************************************************************************************
     * @return the outputStream
     ************************************************************************************/
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    /************************************************************************************
     * @param outputStream the outputStream to set
     ***********************************************************************************/
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Scales and rotates an image. Rotation is optional. If no rotation is needed, the angle must be set to 0 <br/>
     * <br/>
     * 
     * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param scaleby defines the scaling method
     * @param angle angle for rotation; value will be betwetween 0 and 360
     * 
     * @return the generated {@link RenderedImage}
     * @throws ImageManipulatorException
     */
    public RenderedImage scaleImageByPixel(int pixelx, int pixely, int scaleby, int angle) throws ImageManipulatorException {
        return scaleImageByPixel(pixelx, pixely, scaleby, angle, null, null);
    }

    /**
     * Scales an image. Several different modes are possible. It may also draw small little boxes for highlighting words into the image. <br/>
     * <br/>
     * If no boxes should be drawn, the coordinates must be set to null. An empty List ist NOT sufficient and will cause errors. <br/>
     * 
     * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param scaleby defines the scaling method
     * @param coordinates a LinkedList containing String. Those strings are representing coordinates: x1,y1,x2,y2 for boxes
     * @param inColor color for drawing those boxes
     * 
     * @return the generated {@link RenderedImage}
     * @throws ImageManipulatorException
     */
    public RenderedImage scaleImageByPixel(int pixelx, int pixely, int scaleby, List<String> coordinates, Color inColor)
            throws ImageManipulatorException {
        return scaleImageByPixel(pixelx, pixely, scaleby, 0, coordinates, inColor);
    }

    /**
     * Scales and rotates an image. Several different modes are possible. It may also draw small little boxes for highlighting words into the image. <br/>
     * Rotation is optional. If no rotation is needed, the angle must be set to 0 <br/>
     * If no boxes should be drawn, the coordinates must be set to null. An empty List ist NOT sufficient and will cause errors. <br/>
     * 
     * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param scaleby defines the scaling method
     * @param angle angle for rotation; value will be betwetween 0 and 360
     * @param coordinates a LinkedList containing String. Those strings are representing coordinates: x1,y1,x2,y2 for boxes
     * @param inColor color for drawing those boxes
     * 
     * @return the generated {@link RenderedImage}
     * @throws ImageManipulatorException
     */
    public RenderedImage scaleImageByPixel(int pixelx, int pixely, int scaleby, int angle, List<String> coordinates, Color inColor)
            throws ImageManipulatorException {
        LOGGER.debug("Calling scaleImageByPixel from ImageManager.scaleImageByPixel");
        return scaleImageByPixel(pixelx, pixely, scaleby, angle, coordinates, inColor, null, false, 0);
    }

    /**
     * Scales and rotates an image. Several different modes are possible. It may also draw small little boxes for highlighting words into the image.
     * If a watermark is given, the watermark can also be added. <br/>
     * Rotation is optional. If no rotation is needed, the angle must be set to 0 <br/>
     * If no boxes should be drawn, the coordinates must be set to null. An empty List ist NOT sufficient and will cause errors. <br/>
     * If a watermark is set, all watermark related parameters must be set as well. However setting the watermark is optional. If the watermark is set
     * to null, no watermark will be added to the image.
     * 
     * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
     * @param externalscalemethod defines the scaling method
     * @param angle angle for rotation; value will be betwetween 0 and 360
     * @param coordinates a LinkedList containing String. Those strings are representing coordinates: x1,y1,x2,y2 for boxes
     * @param inColor color for drawing those boxes
     * @param inWatermark Watermark to be added to the scaled and rotated image
     * @param watermarkscale method for adjusting the size of the watermark
     * @param watermarkposition tells if watermark should be added to TOP,BOTTOM,RIGHT or LEFT of the image
     * @return the generated {@link RenderedImage}
     * @throws ImageManipulatorException
     */
    public RenderedImage scaleImageByPixel(int pixelx, int pixely, int externalscalemethod, int angle, List<String> coordinates, Color inColor,
            Watermark inWatermark, boolean watermarkscale, int watermarkposition) throws ImageManipulatorException {
        MagickImage inImage = null;
        MagickImage outImage = null;
        List<String> draw_coordinates = null;
        float internalScaling_y = 0;
        float internalScaling_x = 0;
        int scalemethod; // method for scaling: next neighbor, subsample
        // check parameters
        if ((angle < 0) || (angle > 360)) {
            throw new ImageManipulatorException("Invalid value for angle - must be between 0 and 360 degrees");
        }

        // if ((angle > 0) && (coordinates != null) && (coordinates.size() > 0)) {
        // throw new ImageManipulatorException("Can't rotate angle, when box cordinates are used!");
        // }

        // get image
        if (this.mImage != null) {
            inImage = this.mImage;
        }
        if (inImage == null) {
            throw new ImageManipulatorException("Can't get image from ImageMagick");
        }

        // get image dimensions
        float width, height, xResolution, yResolution;
        int samplesPerPixel, colorDepth;
        try {
            width = inImage.getDimension().width;
            height = inImage.getDimension().height;
            xResolution = (float) inImage.getXResolution();
            yResolution = (float) inImage.getYResolution();
            samplesPerPixel = inImage.getNumFrames();
            colorDepth = inImage.getDepth();
            if (width <= 0 || height <= 0) {
                throw new ImageManipulatorException("Can't get image dimensions");
            }
            if (yResolution <= 0 || xResolution <= 0) {
                LOGGER.warn("Cannot read image reslution. Using default resolution");
                xResolution = defaultXResolution;
                yResolution = defaultYResolution;
            }
        } catch (MagickException e) {
            throw new ImageManipulatorException("Cannot get image properties");
        }

        // ----------------------------------------------------------------------------------------------------
        if (externalscalemethod == SCALE_TO_BOX) {
            // pixely = neue höhe
            // pixelx = neue breite
            internalScaling_x = pixelx / width;
            internalScaling_y = pixely / height;

            LOGGER.debug("x: " + internalScaling_x + " y: " + internalScaling_y);
            if (internalScaling_x > internalScaling_y) {

                internalScaling_x = internalScaling_y;
            } else {
                internalScaling_y = internalScaling_x;
            }
            LOGGER.debug("new values: " + internalScaling_x);
            LOGGER.debug("new x " + width * internalScaling_x);
            LOGGER.debug("new y " + height * internalScaling_y);

        }

        // ----------------------------------------------------------------------------------------------------
        // calculate internal scaling factor
        else if (externalscalemethod == SCALE_BY_WIDTH) {
            internalScaling_x = pixelx / width;
            if (pixely == 0) {
                // scale proportionally
                internalScaling_y = internalScaling_x;
            } else {
                internalScaling_y = pixely / height;
            }
            // ----------------------------------------------------------------------------------------------------
        } else if (externalscalemethod == SCALE_BY_HEIGHT) {
            internalScaling_y = pixely / height;
            if (pixelx == 0) {
                // scale proportionally
                internalScaling_x = internalScaling_y;
            } else {
                internalScaling_x = pixelx / width;
            }
            // ----------------------------------------------------------------------------------------------------
        } else if (externalscalemethod == SCALE_BY_PERCENT) {
            float xres = xResolution; // get x resolution
            float yres = yResolution; // get y resolution

            float intpercentx = ((float) pixelx / 100);
            float intpercenty = ((float) pixely / 100);

            internalScaling_x = (xResolution / xres);
            // internalScaling_x = (DPI_DEFAULT / xres);
            internalScaling_x = internalScaling_x * intpercentx;
            internalScaling_y = (xResolution / yres);
            // internalScaling_y = (DPI_DEFAULT / yres);
            internalScaling_y = internalScaling_y * intpercenty;

        } else {
            // no known scale mechanism
            throw new ImageManipulatorException("no known scale mechanism");
        }

        // ----------------------------------------------------------------------------------------------------
        // calculate the new coordinates
        // ----------------------------------------------------------------------------------------------------
        if (coordinates != null) {
            draw_coordinates = ImageManipulator.scaleCoordinates(coordinates, internalScaling_x, internalScaling_y);
        }

        // ----------------------------------------------------------------------------------------------------
        // scale image depending on mechanism
        // mechanism depends on color depth
        // ----------------------------------------------------------------------------------------------------
        if ((samplesPerPixel == 1) && (colorDepth == 1)) {
            // it's bitonal
            scalemethod = this.parameters.get(BITONALSCALEMETHOD);
        } else if ((samplesPerPixel == 1) && (colorDepth > 1)) {
            // it's greyscale
            scalemethod = this.parameters.get(GREYSCALESCALEMETHOD);
        } else {
            // it's color
            scalemethod = this.parameters.get(COLORSCALEMETHOD);
        }
        // check value of scale method and scale
        if (scalemethod == SUBSAMPLETOGREY) {
            outImage = MagickImageManipulator.scaleSubsampleBinaryToGrey(inImage, internalScaling_x, internalScaling_y);
        } else if (scalemethod == BILINEAR) {
            outImage = MagickImageManipulator.scaleInterpolationBilinear(inImage, internalScaling_x, internalScaling_y);
        } else if (scalemethod == NEXTNEIGHBOR) {
            outImage = MagickImageManipulator.scaleNextNeighbor(inImage, internalScaling_x, internalScaling_y);
        } else {
            throw new ImageManipulatorException("wrong scalemethod");
        }

        // ----------------------------------------------------------------------------------------------------
        // draw boxes, but only if we don't rotate the image
        // ----------------------------------------------------------------------------------------------------
        if ((coordinates != null)) {
            if (inColor == null) {
                inColor = Color.GREEN;
            }
            // TODO: Currently doesn't do anything
            outImage = MagickImageManipulator.drawBoxes(outImage, draw_coordinates, inColor);
        }

        // ----------------------------------------------------------------------------------------------------
        // rotate image
        // ----------------------------------------------------------------------------------------------------
        if (angle > 0) {
            outImage = MagickImageManipulator.rotate(outImage, angle);
        }
        // ----------------------------------------------------------------------------------------------------
        // deal with the watermarks
        RenderedImage watermarkRi = null;
        if (inWatermark != null) {
            try {
                int outWidth = outImage.getDimension().width;
                int outHeight = outImage.getDimension().height;
                // we have to scale watermark
                // ------------------------------------------------------------------------------------------------
                if (watermarkscale) {
                    // watermark will be rendered and then scaled
                    watermarkRi = inWatermark.getRenderedImage();
                    // calculate internal scaling factor
                    if ((watermarkposition == ImageManager.TOP) || (watermarkposition == ImageManager.BOTTOM)) {
                        internalScaling_x = (float) outWidth / (float) watermarkRi.getWidth();
                        internalScaling_y = internalScaling_x;
                    } else {
                        internalScaling_y = (float) outHeight / (float) watermarkRi.getHeight();
                        internalScaling_x = internalScaling_y;
                    }
                    LOGGER.debug("Scaling watermark: image size:" + outWidth + " / " + outHeight + "   watermark size:" + watermarkRi.getWidth()
                            + " / " + watermarkRi.getHeight() + "\n scalefactor:" + internalScaling_x);
                    // scale watermark
                    watermarkRi = ImageManipulator.scaleNextNeighbor(watermarkRi, internalScaling_x, internalScaling_y);
                }
                // we don't have to scale watermark
                // ------------------------------------------------------------------------------------------------
                else {
                    if ((watermarkposition == ImageManager.TOP) || (watermarkposition == ImageManager.BOTTOM)) {
                        inWatermark.overrideWidth(outWidth);
                    } else {
                        inWatermark.overrideHeight(outHeight);
                    }
                    watermarkRi = inWatermark.getRenderedImage();
                }
                LOGGER.debug("Watermark size is: " + watermarkRi.getWidth() + " / " + watermarkRi.getHeight());

                // ------------------------------------------------------------------------------------------------
                // add renderedImage of Watermark to outImage
                // ------------------------------------------------------------------------------------------------
                MagickImage watermarkImage = new MagickImage(); // TODO: Create magickImage of watermarkImage
                if (watermarkposition == ImageManager.RIGHT) {
                    outImage = MagickImageManipulator.mergeImages(outImage, watermarkImage, MergingMode.HORIZONTALLY);
                } else if (watermarkposition == ImageManager.LEFT) {
                    outImage = MagickImageManipulator.mergeImages(watermarkImage, outImage, MergingMode.HORIZONTALLY);
                } else if (watermarkposition == ImageManager.TOP) {
                    outImage = MagickImageManipulator.mergeImages(watermarkImage, outImage, MergingMode.VERTICALLY);
                } else if (watermarkposition == ImageManager.BOTTOM) {
                    outImage = MagickImageManipulator.mergeImages(outImage, watermarkImage, MergingMode.VERTICALLY);
                }
            } catch (MagickException e) {
                throw new ImageManipulatorException("Error creating watermark");
            }
        }

        if (outImage == null) {
            throw new ImageManipulatorException("Can't create a RenderedImage object, outImage is null");
        }
        inImage = null;

        // Create RenderedImage from MagickImage
        try {
            byte[] imageBlob = outImage.imageToBlob(imageInfo);
            InputStream iStream = new ByteInputStream(imageBlob, imageBlob.length);
            RenderedImage riImage = ImageIO.read(iStream);
            return riImage;
        } catch (IOException e) {
            throw new ImageManipulatorException("Error creating rendered image");
        }
    }

    /**
     * @return the httpproxyhost
     */
    public String getHttpproxyhost() {
        return this.httpproxyhost;
    }

    /**
     * @param httpproxyhost the httpproxyhost to set
     */
    public void setHttpproxyhost(String httpproxyhost) {
        this.httpproxyhost = httpproxyhost;
    }

    /**
     * @return the httpproxyport
     */
    public String getHttpproxyport() {
        return this.httpproxyport;
    }

    /**
     * @param httpproxyport the httpproxyport to set
     */
    public void setHttpproxyport(String httpproxyport) {
        this.httpproxyport = httpproxyport;
    }

    /**
     * @return the httpproxyuser
     */
    public String getHttpproxyuser() {
        return this.httpproxyuser;
    }

    /**
     * @param httpproxyuser the httpproxyuser to set
     */
    public void setHttpproxyuser(String httpproxyuser) {
        this.httpproxyuser = httpproxyuser;
    }

    /**
     * @return the httpproxypassword
     */
    public String getHttpproxypassword() {
        return this.httpproxypassword;
    }

    /**
     * @param httpproxypassword the httpproxypassword to set
     */
    public void setHttpproxypassword(String httpproxypassword) {
        this.httpproxypassword = httpproxypassword;
    }

}
