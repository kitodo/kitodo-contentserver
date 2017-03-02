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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URL;

import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;

/************************************************************************************
 * WatermarkImage class
 * 
 * @version 20.11.2010
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * @author Igor Toker
 ************************************************************************************/
public class WatermarkImage extends WatermarkComponent {
    private static final Logger LOGGER = Logger.getLogger(WatermarkImage.class);

    RenderedImage wImage; // actual Image which should be added to the watermark
    String origin = "left"; // origin of the coordinate system, left or right
                            // upper corner

    public WatermarkImage(Node configNode) throws WatermarkException {
        super(configNode);
        NamedNodeMap nnm = configNode.getAttributes();
        if (nnm != null) {
            Node urlnode = nnm.getNamedItem("url");
            Node xnode = nnm.getNamedItem("x");
            Node ynode = nnm.getNamedItem("y");
            Node originnode = nnm.getNamedItem("origin");

            // x coordinate
            if (xnode != null) {
                String value = xnode.getNodeValue();
                try {
                    this.x = Integer.parseInt(value);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for x-coordinate for Watermark Image");
                    throw new WatermarkException("Invalid value for x-coordinate for Watermark Image", e);
                }
            } else {
                this.x = 0;
            }

            // y coordinate
            if (ynode != null) {
                String value = ynode.getNodeValue();
                try {
                    this.y = Integer.parseInt(value);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for y-coordinate for Watermark Image");
                    throw new WatermarkException("Invalid value for y-coordinate for Watermark Image", e);
                }
            } else {
                this.y = 0;
            }

            if (originnode != null) {
                String value = originnode.getNodeValue();
                if ((!value.equalsIgnoreCase("left")) && (!value.equalsIgnoreCase("right")) && (!value.equalsIgnoreCase("center"))) {
                    LOGGER.error("origin node has invalid value for Watermark Image");
                    throw new WatermarkException("origin node has invalid value for Watermark Image");
                } else {
                    origin = value;
                }
            }

            // image URL, load the actual image
            if (urlnode != null) {
                loadImageFromUrl(urlnode.getNodeValue());
            } else {
                LOGGER.error("No URL for Watermark Image found");
                throw new WatermarkException("No URL for Watermark Image found");
            }
        }
    }

    protected final void loadImageFromUrl(String value) throws WatermarkException {
        try {
            ImageManager im = new ImageManager(new URL(value));
            ImageInterpreter myInterpreter = im.getMyInterpreter();
            wImage = myInterpreter.getRenderedImage();
        } catch (Exception e) {
            throw new WatermarkException("URL for watermark image " + value + " is invalid URL", e);
        }

    }

    /*************************************************************************************
     * empty Constructor with inImage as RenderedImage
     ************************************************************************************/
    public WatermarkImage(int id, RenderedImage inImage) {
        super(id);
        wImage = inImage;
    }

    /*************************************************************************************
     * Setter for this.wImage
     * 
     * @param image the wImage to set
     ************************************************************************************/
    protected void setWImage(RenderedImage image) {
        this.wImage = image;
    }

    /*************************************************************************************
     * This methods renders the WatermarkImage into the Watermark at the given coordinates.
     * 
     ************************************************************************************/
    public void render() {
        int actual_x = 0;
        int actual_y = 0;

        if (this.getParent_watermark() == null) {
            LOGGER.warn("WatermarkImage is not included in any Watermark; can't render WatermarkImage");
            return;
        }

        Graphics g = this.parentWatermark.getWatermarkImage().createGraphics(); // get
                                                                                // graphics
                                                                                // to
                                                                                // draw
                                                                                // on

        // PlanarImage img=PlanarImage.wrapRenderedImage(this.getTargetImage());
        // // get a PlanarImage of the target image
        BufferedImage bi = ImageManipulator.fromRenderedToBuffered(wImage);

        // calculate the actual coordinates
        if (origin.equalsIgnoreCase("LEFT")) {
            actual_x = this.getX();
            actual_y = this.getY();
        } else if (origin.equalsIgnoreCase("RIGHT")) {
            actual_y = this.getY();
            actual_x = this.getParent_watermark().getWidth() - wImage.getWidth() - this.getX();
        } else if (origin.equalsIgnoreCase("CENTER")) {
            actual_y = this.getY();
            actual_x = (this.getParent_watermark().getWidth() - wImage.getWidth()) / 2;
        }

        g.drawImage(bi, actual_x, actual_y, null);

    }

}
