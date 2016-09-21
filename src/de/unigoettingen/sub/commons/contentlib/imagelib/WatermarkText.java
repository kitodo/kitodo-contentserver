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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;

/************************************************************************************
 * WatermarkText class
 * 
 * @version 20.11.2010
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * @author Igor Toker
 ************************************************************************************/
public class WatermarkText extends WatermarkComponent {
    private static final Logger LOGGER = Logger.getLogger(WatermarkText.class);

    String content = null;
    String font = null;
    int fontstyle = Font.PLAIN;
    Color fontcolor = new Color(1f, 1f, 1f);
    int fontsize = 12;
    String origin = "left"; // origin of the coordinate system

    /*************************************************************************************
     * Constructor with inContent as {@link String} for content
     ************************************************************************************/
    public WatermarkText(int id, String inContent) {
        super(id);
        content = inContent;
    }

    public WatermarkText(Node configNode) throws WatermarkException {
        super(configNode);
        NamedNodeMap nnm = configNode.getAttributes();
        if (nnm != null) {
            Node fontnamenode = nnm.getNamedItem("fontname");
            Node fontsizenode = nnm.getNamedItem("fontsize");
            // Node fontstylenode = nnm.getNamedItem("fontstyle");
            Node colornode = nnm.getNamedItem("fontcolor");

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

            // read font information
            if (fontnamenode != null) {
                font = fontnamenode.getNodeValue();
                if (!isFontKnown(font)) {
                    LOGGER.error("Unknown font for text (" + font + ")");
                    throw new WatermarkException("Unknown font for text (" + font + ")");
                }
            } else {
                font = "Serif";
            }

            // read font size
            if (fontsizenode != null) {
                String fontsize_str = fontsizenode.getNodeValue();
                try {
                    fontsize = Integer.parseInt(fontsize_str);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for fontsize for Watermark Text");
                    throw new WatermarkException("Invalid value for fontsize for Watermark Text", e);
                }
            }

            // read the color
            if (colornode != null) {
                String colorstring = colornode.getNodeValue();
                // split string into compontents
                String r = colorstring.substring(0, 2);
                String g = colorstring.substring(2, 4);
                String b = colorstring.substring(4, 6);

                // convert hexadecimals into decimals

                try {
                    int r_int = Integer.parseInt(r, 16);
                    int g_int = Integer.parseInt(g, 16);
                    int b_int = Integer.parseInt(b, 16);

                    fontcolor = new Color(r_int, g_int, b_int);
                } catch (Exception e) {
                    LOGGER.error("Invalid value for fontsize for Watermark Text");
                    throw new WatermarkException("Invalid value for fontsize for Watermark Text", e);
                }

            }
        }
        // read the content

        NodeList nl = configNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                this.content = n.getNodeValue();
                break; // get out of loop, we found the content
            }
        }

        if (content == null) {
            LOGGER.error("No content for Watermark Text");
            throw new WatermarkException("No content for Watermark Text");
        }
    }

    /**
     * find if font is known and available
     * 
     * @TODO needs to be rewritten using the getAllAvailableFontFamilyNames method
     * @param font
     * @return true if font is known
     */
    private boolean isFontKnown(String font) {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String fontNames[] = ge.getAvailableFontFamilyNames();
        for (String fontName : fontNames) {
            if (fontName.equals(font)) {
                return true;
            }
        }

        return false;
    }

    /**************************************************************************************
     * Renders the text in the bufferedImage of the Watermark object.
     *************************************************************************************/
    public void render() {

        int actual_x = 0;
        int actual_y = 0;

        if (this.getParent_watermark() == null) {
            LOGGER.warn("WatermarkText is not included in any Watermark; can't render WatermarkText");
            return;
        }

        BufferedImage bImage = this.getParent_watermark().getWatermarkImage(); // gets

        // create graphics to render the text
        Graphics2D g = bImage.createGraphics();

        // get color for string
        g.setColor(this.fontcolor);
        g.setPaint(this.fontcolor);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // set font for the text

        g.setFont(new Font(font, this.fontstyle, this.fontsize));
        FontMetrics fontMetrics = g.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(this.content, g); // dimensions

        // calculate the actual coordinates
        if (origin.equalsIgnoreCase("LEFT")) {
            actual_x = this.getX();
            actual_y = this.getY();
        } else if (origin.equalsIgnoreCase("RIGHT")) {
            actual_y = this.getY();
            actual_x = this.getParent_watermark().getWidth() - ((int) rect.getWidth()) - this.getX();
        } else if (origin.equalsIgnoreCase("CENTER")) {
            actual_y = this.getY();
            actual_x = (this.getParent_watermark().getWidth() - ((int) rect.getWidth())) / 2;
        }

        // draw the string
        g.drawString(this.content, actual_x, actual_y);
    }

    /*************************************************************************************
     * Getter for content
     * 
     * @return the content
     *************************************************************************************/
    public String getContent() {
        return content;
    }

    /**************************************************************************************
     * Setter for content
     * 
     * @param content the content to set
     **************************************************************************************/
    public void setContent(String content) {
        this.content = content;
    }

    /*************************************************************************************
     * Getter for font
     * 
     * @return the font
     *************************************************************************************/
    public String getFont() {
        return font;
    }

    /**************************************************************************************
     * Setter for font
     * 
     * @param font the font to set
     **************************************************************************************/
    public void setFont(String font) {
        this.font = font;
    }

    /*************************************************************************************
     * Getter for fontstyle
     * 
     * @return the fontstyle
     *************************************************************************************/
    public int getFontstyle() {
        return fontstyle;
    }

    /**************************************************************************************
     * Setter for fontstyle
     * 
     * @param fontstyle the fontstyle to set
     **************************************************************************************/
    public void setFontstyle(int fontstyle) {
        this.fontstyle = fontstyle;
    }

    /*************************************************************************************
     * Getter for fontsize
     * 
     * @return the fontsize
     *************************************************************************************/
    public int getFontsize() {
        return fontsize;
    }

    /**************************************************************************************
     * Setter for fontsize
     * 
     * @param fontsize the fontsize to set
     **************************************************************************************/
    public void setFontsize(int fontsize) {
        this.fontsize = fontsize;
    }

    /*************************************************************************************
     * Getter for fontcolor
     * 
     * @return the fontcolor
     *************************************************************************************/
    public Color getFontcolor() {
        return fontcolor;
    }

    /**************************************************************************************
     * Setter for fontcolor
     * 
     * @param fontcolor the fontcolor to set
     **************************************************************************************/
    public void setFontcolor(Color fontcolor) {
        this.fontcolor = fontcolor;
    }

}
