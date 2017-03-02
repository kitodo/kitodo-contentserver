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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;

/************************************************************************************
 * Implements a filled box. Default color is white.
 * 
 * @version 20.11.2010
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * @author Igor Toker
 ************************************************************************************/
public class WatermarkBox extends WatermarkComponent {
    private static final Logger LOGGER = Logger.getLogger(WatermarkBox.class);

    String origin = "left"; // origin of the coordinate system

    Integer width;
    Integer height;
    Color color = new Color(0f, 0f, 0f);

    /*************************************************************************************
     * empty Constructor
     ************************************************************************************/
    public WatermarkBox(int id) {
        super(id);
    }

    /*************************************************************************************
     * Constructor for box with given height and width
     ************************************************************************************/
    public WatermarkBox(int id, Integer w, Integer h) {
        super(id);
        this.width = w;
        this.height = h;
    }

    /*************************************************************************************
     * Constructor for box with given size and color
     ************************************************************************************/
    public WatermarkBox(int id, Integer w, Integer h, Color inColor) {
        super(id);
        this.width = w;
        this.height = h;
        color = inColor;
    }

    public WatermarkBox(Node configNode) throws WatermarkException {
        super(configNode);
        NamedNodeMap nnm = configNode.getAttributes();
        if (nnm != null) {
            Node heightnode = nnm.getNamedItem("height");
            Node widthnode = nnm.getNamedItem("width");
            Node colornode = nnm.getNamedItem("color");

            Node xnode = nnm.getNamedItem("x");
            Node ynode = nnm.getNamedItem("y");

            Node originnode = nnm.getNamedItem("origin");

            // read x and y
            // x coordinate
            if (xnode != null) {
                String value = xnode.getNodeValue();
                try {
                    this.x = Integer.parseInt(value);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for x-coordinate for Watermark Text");
                    throw new WatermarkException("Invalid value for x-coordinate for Watermark Text", e);
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
                    LOGGER.error("Invalid value for y-coordinate for Watermark Text");
                    throw new WatermarkException("Invalid value for y-coordinate for Watermark Text", e);
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

            // width and height
            //

            if (widthnode != null) {
                String value = widthnode.getNodeValue();
                try {
                    this.width = Integer.parseInt(value);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for width for Watermark Text");
                    throw new WatermarkException("Invalid value for width for Watermark Text", e);
                }
            } else {
                this.width = 0;
            }

            if (heightnode != null) {
                String value = heightnode.getNodeValue();
                try {
                    this.height = Integer.parseInt(value);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for height for Watermark Text");
                    throw new WatermarkException("Invalid value for height for Watermark Text", e);
                }
            } else {
                this.width = 0;
            }

            // color
            //

            if (colornode != null) {
                String colorstring = colornode.getNodeValue();
                // split string into compontents
                String r = colorstring.substring(0, 2);
                String g = colorstring.substring(2, 4);
                String b = colorstring.substring(4, 6);

                // convert hexadecimals into decimals

                try {
                    Integer r_int = Integer.parseInt(r, 16);
                    Integer g_int = Integer.parseInt(g, 16);
                    Integer b_int = Integer.parseInt(b, 16);

                    color = new Color(r_int, g_int, b_int);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for fontsize for Watermark Text");
                    throw new WatermarkException("Invalid value for fontsize for Watermark Text", e);
                }
            }
        } // end of parsing NamedNodeMap
    }

    /*************************************************************************************
     * Getter for width
     * 
     * @return the width
     *************************************************************************************/
    public Integer getWidth() {
        return width;
    }

    /**************************************************************************************
     * Setter for width
     * 
     * @param width the width to set
     **************************************************************************************/
    public void setWidth(Integer width) {
        this.width = width;
    }

    /*************************************************************************************
     * Getter for height
     * 
     * @return the height
     *************************************************************************************/
    public Integer getHeight() {
        return height;
    }

    /**************************************************************************************
     * Setter for height
     * 
     * @param height the height to set
     **************************************************************************************/
    public void setHeight(Integer height) {
        this.height = height;
    }

    /*************************************************************************************
     * Getter for color
     * 
     * @return the color
     *************************************************************************************/
    public Color getColor() {
        return color;
    }

    /**************************************************************************************
     * Setter for color
     * 
     * @param color the color to set
     **************************************************************************************/
    public void setColor(Color color) {
        this.color = color;
    }

    /*************************************************************************************
     * render the watermark
     ************************************************************************************/

    public void render() {

        Integer actual_x = 0;
        Integer actual_y = 0;

        if (this.getParent_watermark() == null) {
            LOGGER.warn("WatermarkBox is not included in any Watermark; can't render WatermarkBox");
            return;
        }

        BufferedImage bImage = this.getParent_watermark().getWatermarkImage(); // gets
                                                                               // the
                                                                               // image
                                                                               // of
                                                                               // the
                                                                               // watermark

        // calculate the actual coordinates
        if (origin.equalsIgnoreCase("LEFT")) {
            actual_x = this.getX();
            actual_y = this.getY();
        } else if (origin.equalsIgnoreCase("RIGHT")) {
            actual_y = this.getY();
            actual_x = this.getParent_watermark().getWidth() - this.getWidth() - this.getX();
        } else if (origin.equalsIgnoreCase("CENTER")) {
            actual_y = this.getY();
            actual_x = (this.getParent_watermark().getWidth() - this.getWidth()) / 2;
        }

        // create graphics to render the text
        Graphics2D g = bImage.createGraphics();

        g.setColor(this.color);
        g.setPaint(this.color);
        g.fillRect(actual_x, actual_y, this.width, this.height);
    }

}
