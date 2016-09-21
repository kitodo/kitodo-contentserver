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

import java.awt.image.BufferedImage;

import org.w3c.dom.Node;

import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;

/************************************************************************************
 * WatermarkComponent class
 * 
 * @version 20.11.2010
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * @author Igor Toker
 ************************************************************************************/
public class WatermarkComponent {

    protected int id;
    Integer x = 0; // coordinates in pixels
    Integer y = 0; // coordinates in pixels
    BufferedImage targetImage; // the canvas on which the component is rendered

    Watermark parentWatermark = null;

    /*************************************************************************************
     * empty Constructor
     ************************************************************************************/
    public WatermarkComponent(int id) {
        this.id = id;
    }

    public WatermarkComponent(Node configNode) throws WatermarkException {
        try {
            this.id = Integer.valueOf(configNode.getAttributes().getNamedItem("id").getNodeValue());
        } catch (Exception e) {
            throw new WatermarkException("Can't find id for Watermark Component");
        }
    }

    /*************************************************************************************
     * Getter for x
     * 
     * @return the x
     *************************************************************************************/
    public Integer getX() {
        return x;
    }

    /**************************************************************************************
     * Setter for x
     * 
     * @param x the x to set
     **************************************************************************************/
    public void setX(Integer x) {
        this.x = x;
    }

    /*************************************************************************************
     * Getter for y
     * 
     * @return the y
     *************************************************************************************/
    public Integer getY() {
        return y;
    }

    /**************************************************************************************
     * Setter for y
     * 
     * @param y the y to set
     **************************************************************************************/
    public void setY(Integer y) {
        this.y = y;
    }

    /*************************************************************************************
     * Getter for targetImage
     * 
     * @return the targetImage
     *************************************************************************************/
    public BufferedImage getTargetImage() {
        return targetImage;
    }

    /**************************************************************************************
     * Setter for targetImage
     * 
     * @param targetImage the targetImage to set
     **************************************************************************************/
    public void setTargetImage(BufferedImage targetImage) {
        this.targetImage = targetImage;
    }

    /**
     * @return the parent_watermark
     */
    protected Watermark getParent_watermark() {
        return parentWatermark;
    }

    /**
     * @param parent_watermark the parent_watermark to set
     */
    protected void setParent_watermark(Watermark parent_watermark) {
        this.parentWatermark = parent_watermark;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
