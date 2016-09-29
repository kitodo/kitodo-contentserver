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

import java.awt.image.RenderedImage;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;

/************************************************************************************
 * the error image to show or to embedd in pdf file, of no exception in a jsp file can be shown
 * 
 * @version 02.05.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
// TODO: check if this is needed and if so let it implement the Image interface
public class ErrorImage {

    private List<WatermarkText> allTexts = new LinkedList<WatermarkText>();
    private URL uri;

    /**
     * set url for error image
     * 
     * @param inuri the given url
     */
    public ErrorImage(URL inuri) {
        uri = inuri;
    }

    /**
     * Add text to Watermark
     * 
     * @param wt the watermark to add
     */
    public void addWatermarkText(WatermarkText wt) {
        allTexts.add(wt);
    }

    /**
     * Writes the whole Image as a JPEG Image!
     * 
     * @param ostream the outputstream to write to
     * @throws ImageManagerException
     */
    public void renderAsJPG(OutputStream ostream) throws ImageManagerException {
        // get image
        ImageManager imagemanager = new ImageManager(uri);
        ImageInterpreter myInterpreter = imagemanager.getMyInterpreter();

        RenderedImage backgroundImage = myInterpreter.getRenderedImage();

        Watermark newImage = new Watermark(backgroundImage.getHeight(), backgroundImage.getWidth());

        // add BackgroundImage
        WatermarkImage wi = new WatermarkImage(0, backgroundImage);
        wi.setX(0);
        wi.setY(0);
        newImage.addWatermarkComponent(wi);

        // add all Text Components
        newImage.setAllWatermarkComponents(new LinkedList<WatermarkComponent>(allTexts));

        // write the new image to output

        RenderedImage ri = newImage.getRenderedImage();

        JpegInterpreter ti = new JpegInterpreter(ri);
        ti.setXResolution(72f);
        ti.setYResolution(72f);
        ti.setColordepth(8);
        ti.setSamplesperpixel(3);
        ti.writeToStream(null, ostream);
    }

}
