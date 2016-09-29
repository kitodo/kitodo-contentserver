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
package de.unigoettingen.sub.commons.contentlib.servlet;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.imagelib.WatermarkImage;
import de.unigoettingen.sub.commons.contentlib.imagelib.WatermarkText;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

public class ServletWatermark extends Watermark {
    public ServletWatermark(int width, int heigth) {
        super(width, heigth);
    }

    public ServletWatermark(File inFile) throws WatermarkException {
        super(inFile);

    }

    /***********************************************************************
     * Create error image using the watermark creation possibilities
     * 
     * @param inputFileStream - Input File Stream
     * @param inMessage the message of the error string
     * @return generated watermark
     * @throws ImageInterpreterException
     ***********************************************************************/
    public static Watermark generateErrorWatermark(FileInputStream inputFileStream, String inMessage) throws ImageInterpreterException {
        ContentServerConfiguration config = ContentServerConfiguration.getInstance();
        JpegInterpreter si = new JpegInterpreter(inputFileStream);
        RenderedImage ri1 = si.getRenderedImage();
        Watermark wm = new Watermark(si.getWidth(), si.getHeight());

        WatermarkImage wmi = new WatermarkImage(0, ri1);
        wmi.setX(500);
        wmi.setY(500);
        wm.addWatermarkComponent(wmi);

        /*-------------------------------------
         * Generate watermark text components
         *------------------------------------*/
        // Exception Name.
        String exName = inMessage.substring(0, inMessage.indexOf(':') + 1);
        // Exception Message.
        String exString = String.valueOf(inMessage.subSequence(inMessage.indexOf(':') + 2, inMessage.length()));

        int maxLength = config.getErrorMessageMaxLineLength();
        int exLength = exString.length();
        int exCounter = exLength / maxLength;

        /*-------------------------------- 
         * wrap message text at line end
         * --------------------------------*/
        Color color = new Color(0f, 0f, 0f);
        for (int i = 0; i <= exCounter; i++) {
            String tempString = "";
            if (exString.length() <= i * maxLength + maxLength) {
                tempString = exString.substring(i * maxLength, exString.length());
            } else {
                tempString = exString.substring(i * maxLength, i * maxLength + maxLength);
            }
            WatermarkText wmt = new WatermarkText(0, tempString);
            wmt.setX(10);
            wmt.setY(60 + ((config.getErrorMessageFontSize() + 5) * i + config.getErrorMessageFontSize()));
            wmt.setFontsize(config.getErrorMessageFontSize());
            wmt.setFontcolor(color);

            wm.addWatermarkComponent(wmt);
        }

        WatermarkText wmt = new WatermarkText(0, exName);
        wmt.setX(10);
        wmt.setY(60);
        wmt.setFontsize(config.getErrorMessageFontSize());
        wmt.setFontcolor(color);

        WatermarkText wmt1 = new WatermarkText(0, config.getErrorTitle());
        wmt1.setX(220);
        wmt1.setY(30);
        wmt1.setFontsize(config.getErrorTitleFontSize());
        wmt1.setFontcolor(color);

        wm.addWatermarkComponent(wmt);
        wm.addWatermarkComponent(wmt1);
        return wm;
    }

}
