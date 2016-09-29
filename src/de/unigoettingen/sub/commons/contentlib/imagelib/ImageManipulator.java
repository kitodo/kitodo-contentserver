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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;
import javax.media.jai.operator.TransposeType;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;

/************************************************************************************
 * ImageManipulator is used by ImageManager to calculate Rotation, Scaling, Highlighting etc.
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
// TODO: Add Wrapper Methods to handle the Image interface
public final class ImageManipulator {
    /**
     * types of MerginsModes for two images
     */
    public enum MergingMode {
        /** horizontal mode */
        HORIZONTALLY,
        /** vertical mode */
        VERTICALLY;
    }

    private static final Logger LOGGER = Logger.getLogger(ImageManipulator.class);

    /**************************************************************************************
     * hidden default constructor only for the singleton
     **************************************************************************************/
    private ImageManipulator() {
    }

    /************************************************************************************
     * Scale an image with the InterpolationBilinear scale algorithm
     * 
     * @param inImage the source {@link RenderedImage}
     * @param scalex Scaling for x
     * @param scaley Scaling for y
     * @return the manipulated {@link RenderedImage}
     ************************************************************************************/
    public static RenderedImage scaleInterpolationBilinear(RenderedImage inImage, float scalex, float scaley) {

        RenderedOp outImage;

        // set all parameters for scaling
        ParameterBlock params = new ParameterBlock();

        params.addSource(inImage);
        params.add(scalex);
        params.add(scaley);
        params.add(0.0F);
        params.add(0.0F);
        params.add(new InterpolationBilinear()); // interpolation method
        // for scaling
        outImage = JAI.create("scale", params);

        // get renderedImage
        return outImage.createInstance();
    }

    /************************************************************************************
     * Scale an image with the InterpolationBilinear subsamplebinarytogray algorithm
     * 
     * @param inImage the source {@link RenderedImage}
     * @param scalex Scaling for x
     * @param scaley Scaling for y
     * @return the manipulated {@link RenderedImage}
     ************************************************************************************/
    public static RenderedImage scaleSubsampleBinaryToGrey(RenderedImage inImage, float scalex, float scaley) {

        RenderedOp outImage;

        // set all parameters for scaling
        ParameterBlock params = new ParameterBlock();

        // some kind of hack...
        // -------------------------------
        if (scalex > 1) {
            scalex = 1 / scalex;
        }
        if (scaley > 1) {
            scaley = 1 / scaley;
        }
        // -------------------------------

        params.addSource(inImage);
        params.add(scalex);
        params.add(scaley);
        params.add(0.0F);
        params.add(0.0F);
        params.add(new InterpolationBilinear()); // interpolationmethod
        // for scaling
        outImage = JAI.create("subsamplebinarytogray", params);

        // get renderedImage
        return outImage.createInstance();
    }

    /************************************************************************************
     * merge two images into one image depending on given parametes for orientation
     * 
     * @param inImage1 first {@link RenderedImage}
     * @param inImage2 second {@link RenderedImage}
     * @param mode Merging mode
     * @return the merged {@link RenderedImage}
     * @throws ImageManipulatorException
     ************************************************************************************/
    public static RenderedImage mergeImages(RenderedImage inImage1, RenderedImage inImage2, MergingMode mode) throws ImageManipulatorException {
        int targetimagewidth = 0;
        int targetimageheight = 0;

        // check if width or height matches
        if (mode == MergingMode.HORIZONTALLY) {
            if (inImage1.getHeight() != inImage2.getHeight()) {
                throw new ImageManipulatorException("images have different height");
            }
            targetimagewidth = inImage1.getWidth() + inImage2.getWidth();
            targetimageheight = inImage1.getHeight();

        }

        if (mode == MergingMode.VERTICALLY) {
            if (inImage1.getWidth() != inImage2.getWidth()) {
                throw new ImageManipulatorException("images have different height");
            }
            targetimagewidth = inImage1.getWidth();
            targetimageheight = inImage1.getHeight() + inImage2.getHeight();
        }

        // create a new RenderedImage (with the targetsize)
        LOGGER.debug("Merging two images: target image is :" + targetimagewidth + " x " + targetimageheight);
        BufferedImage targetBImage = new BufferedImage(targetimagewidth, targetimageheight, BufferedImage.TYPE_INT_RGB);
        Graphics g = targetBImage.createGraphics(); // get graphics to draw on

        if (mode == MergingMode.VERTICALLY) {
            // get first image and draw at at coordinate 0/0
            BufferedImage bi1 = fromRenderedToBuffered(inImage1);
            g.drawImage(bi1, 0, 0, null);

            BufferedImage bi2 = fromRenderedToBuffered(inImage2);
            g.drawImage(bi2, 0, inImage1.getHeight(), null);
        } else if (mode == MergingMode.HORIZONTALLY) {
            // get first image and draw at at coordinate 0/0
            BufferedImage bi1 = fromRenderedToBuffered(inImage1);
            g.drawImage(bi1, 0, 0, null);

            BufferedImage bi2 = fromRenderedToBuffered(inImage2);
            g.drawImage(bi2, inImage1.getWidth(), 0, null);
        }

        return targetBImage;
    }

    /************************************************************************************
     * Scale an image with the Interpolation.INTERP_NEAREST algorithm
     * 
     * @param inImage the source {@link RenderedImage}
     * @param scalex Scaling for x
     * @param scaley Scaling for y
     * @return the manipulated {@link RenderedImage}
     ************************************************************************************/
    public static RenderedImage scaleNextNeighbor(RenderedImage inImage, float scalex, float scaley) {
        RenderedOp outImage;

        // set all parameters for scaling
        ParameterBlock params = new ParameterBlock();

        params.addSource(inImage);
        params.add(scalex);
        params.add(scaley);
        params.add(0.0F);
        params.add(0.0F);
        params.add(new InterpolationBilinear()); // interpolationmethod
        // for scaling
        params.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        outImage = JAI.create("scale", params);
        // get renderedImage
        return outImage.createInstance();
    }

    /************************************************************************************
     * draw boxes with given coordinates an color on image
     * 
     * @param inImage the incoming {@link RenderedImage}
     * @param coordinates the list of coordinates, where to draw the boxes
     * @param color the drawing {@link Color}
     * @return the {@link RenderedImage} with boxes on it
     * @throws ImageManipulatorException
     ************************************************************************************/
    public static RenderedImage drawBoxes(RenderedImage inImage, List<String> coordinates, Color color) throws ImageManipulatorException {

        // create a BufferedImage from the RenderedImage
        BufferedImage bufImage = fromRenderedToBuffered(inImage);

        // convert to RGB; this is important as we want to draw in RGB.
        // greyscale image are converted
        BufferedImage result = new BufferedImage(bufImage.getWidth(), bufImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawRenderedImage(bufImage, null);
        g.dispose();
        // darken image a bit - needs to be as dark as greyscale image
        float scaleFactor = 1f;
        RescaleOp op = new RescaleOp(scaleFactor, 0, null);
        bufImage = op.filter(result, null);
        Graphics2D g2d = bufImage.createGraphics();

        // set the color
        g2d.setColor(color);

        // Iterate over list of coordinates
        Rectangle2D.Float rectangle = new Rectangle2D.Float();
        for (String singlebox : coordinates) {
            String numbers[] = singlebox.split(","); // split between ","
            if (numbers.length != 4) {// invalid format of the string, throw
                // exception
                g2d.dispose();
                throw new ImageManipulatorException("Wrong format of coordinates, format must be x1,y1,x2,y2");
            }
            try {
                // check if all x,y values are integers
                Integer x1 = Integer.parseInt(numbers[0]);
                Integer y1 = Integer.parseInt(numbers[1]);
                Integer x2 = Integer.parseInt(numbers[2]);
                Integer y2 = Integer.parseInt(numbers[3]);

                // draw the box, 0.4f sets it to transparent
                AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
                g2d.setComposite(ac);
                rectangle.setFrame(x1, y1, (x2 - x1), (y2 - y1));
                g2d.fill(rectangle);
            } catch (Exception e) {
                // numbers have the wrong format, throw exception
                g2d.dispose();
                throw new ImageManipulatorException("Coordinates value is wrong; maybe not an integer value", e);
            }
        } // end of foreach
        g2d.dispose(); // throw it away, result is now in buffered image

        return bufImage; // buffered image implements the RenderedImage
        // interface
    }

    /**************************************************************************************
     * Rotates a RenderedImage
     * 
     * @param inImage
     * @param angle in degrees
     * @param quality interpolation method, can be set to "nearest", "bicubic" or "bilinear"
     * @return
     * @throws ImageManipulatorException
     **************************************************************************************/
    public static RenderedImage rotate(RenderedImage inImage, double angle, String quality) throws ImageManipulatorException {

        RenderedImage rotatedImage = null; // target image
        Interpolation interpolationMethod = null; // interpolation method used
        double rangle = Math.toRadians(angle); // convert degrees to radians
        double x = inImage.getWidth() / 2; // find center of image
        double y = inImage.getHeight() / 2;

        // transpose optimized rotation for right angles
        TransposeType rotOp = null;
        if (Math.abs(angle - 0) < 1e-5) {
            // 0 degree
            return inImage;
        } else if (Math.abs(angle - 90) < 1e-5) {
            // 90 degree
            rotOp = TransposeDescriptor.ROTATE_90;
        } else if (Math.abs(angle - 180) < 1e-5) {
            // 180 degree
            rotOp = TransposeDescriptor.ROTATE_180;
        } else if (Math.abs(angle - 270) < 1e-5) {
            // 270 degree
            rotOp = TransposeDescriptor.ROTATE_270;
        } else if (Math.abs(angle - 360) < 1e-5) {
            // 360 degree
            return inImage;
        }
        if (rotOp != null) {
            // transpose
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(rotOp);
            rotatedImage = JAI.create("transpose", pb);
        } else {
            // it's not a right angle, so rotate
            ParameterBlock param = new ParameterBlock();
            param.addSource(inImage);
            param.add((float) x);
            param.add((float) y);
            param.add((float) rangle);
            if ("bicubic".equals(quality)) {
                interpolationMethod = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            } else if ("bilinear".equals(quality)) {
                interpolationMethod = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            } else if ("nearest".equals(quality)) {
                interpolationMethod = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            } else {
                throw new ImageManipulatorException("Unknown interpolation method. Must either be bicubic,bilinear or nearest.");
            }
            param.add(interpolationMethod);

            rotatedImage = JAI.create("rotate", param);
        }

        if (rotatedImage == null) {
            throw new ImageManipulatorException("Can't create rotated image");
        }

        return rotatedImage;
    }

    /**************************************************************************************
     * scales a list of coordinates. The coordinates are a simple string containing the pixel coordinates of a polygone or a rectangle in the form
     * "x1,y1,x2,y2,....,xn,yn".
     * 
     * @param inCoordinates
     * @param scalex
     * @param scaley
     * @return
     * @throws ImageManipulatorException
     **************************************************************************************/
    public static List<String> scaleCoordinates(List<String> inCoordinates, float scalex, float scaley) throws ImageManipulatorException {

        LinkedList<String> result = new LinkedList<String>();

        // iterate over all coordinates
        for (String singleCoordinateset : inCoordinates) {
            String numbers[] = singleCoordinateset.split(","); // split between
            // ","
            // there must be an even number of coordinates
            if (numbers.length % 2 != 0) {
                // it's an uneven number, can't be as we always need 2
                // coordinates per pixel
                throw new ImageManipulatorException("Invalid coordinate format");
            }

            // get single integer values of the coordinates and scale them
            boolean isX = true;
            String new_coordinate = "";
            for (String x : numbers) {
                int new_x = 0;
                int new_y = 0;

                Integer x_integer = Integer.parseInt(x);
                if (isX) {
                    new_x = (int) (x_integer * scalex);
                    isX = false;
                    new_coordinate = new_coordinate + new_x + ",";
                } else {
                    new_y = (int) (x_integer * scaley);
                    isX = true;
                    new_coordinate = new_coordinate + new_y + ",";
                }
            } // end of for
            new_coordinate = new_coordinate.substring(0, new_coordinate.length() - 1); // delete
            // last
            // comma
            result.add(new_coordinate); // add it to list of of coordinates

        }
        if (result.isEmpty()) {
            throw new ImageManipulatorException("No conversion results. Error when calculating new coordinates");
        }
        return result;
    }

    /**************************************************************************************
     * converts a {@link RenderedImage} into a {@link BufferedImage}
     * 
     * @param img the {@link RenderedImage} to convert
     * @return the converted {@link BufferedImage}
     **************************************************************************************/
    public static BufferedImage fromRenderedToBuffered(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // BufferedImage ret = new RenderedImageAdapter(img).getAsBufferedImage();

        ColorModel cm = img.getColorModel();
        int w = img.getWidth();
        int h = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        String[] keys = img.getPropertyNames();

        if (keys != null) {
            for (String key : keys) {
                props.put(key, img.getProperty(key));
            }
        }
        BufferedImage ret = new BufferedImage(cm, raster, isAlphaPremultiplied, props);
        img.copyData(raster);
        return ret;
    }

    public static BufferedImage fromRenderedToBufferedNoAlpha(RenderedImage img) {

        ColorModel cm = img.getColorModel();
        int w = img.getWidth();
        int h = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        String[] keys = img.getPropertyNames();

        if (keys != null) {
            for (String key : keys) {
                // if (!keys[i].equals("ROI"))
                props.put(key, img.getProperty(key));
            }
        }

        BufferedImage ret = new BufferedImage(cm, raster, isAlphaPremultiplied, props);
        img.copyData(raster);

        BufferedImage ret2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // draw image into BufferedImage
        Graphics2D g2d = ret2.createGraphics();
        g2d.drawImage(ret, 0, 0, null);
        return ret2;
    }
}
