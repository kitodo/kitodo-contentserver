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

import java.awt.image.RenderedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.util.stream.StreamUtils;

/************************************************************************************
 * all file formats as central enumeration with some central getter methods for the requested {@link ImageInterpreter}, {@link ImageFileFormat} or
 * Mimetype
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public enum ImageFileFormat {
    /** TIFF images */
    TIFF,
    /** PNG images */
    PNG,
    /** JPG images */
    JPG,
    /** JP2 images */
    JP2;

    /************************************************************************************
     * get MimeType for file format as String
     * 
     * @return MimeType as {@link String}
     ************************************************************************************/
    public String getMimeType() {
        switch (this) {
            case TIFF:
                return "image/tiff";
            case PNG:
                return "image/png";
            case JPG:
                return "image/jpeg";
            case JP2:
                return "image/jp2";
            default:
                return "null/null";
        }
    }

    /************************************************************************************
     * get file extenstion for file format as String (without leading point)
     * 
     * @return file extension as {@link String}
     ************************************************************************************/
    public String getFileExtension() {
        switch (this) {
            case TIFF:
                return "tif";
            case PNG:
                return "png";
            case JPG:
                return "jpg";
            case JP2:
                return "jp2";
            default:
                return null;
        }
    }

    /************************************************************************************
     * get {@link ImageInterpreter} for {@link URL} as parameter using proxy informations
     * 
     * @return requested ImageInterpreter for file format
     * @param url as URL
     * @param httpproxyhost the host for the proxy
     * @param httpproxyport the port for the proxy
     * @param httpproxyuser the user name for the proxy
     * @param httpproxypasswd the password for the proxy
     * @throws ImageInterpreterException
     * @throws IOException
     ************************************************************************************/
    public static ImageInterpreter getInterpreter(URL url, String httpproxyhost, String httpproxyport, String httpproxyuser, String httpproxypasswd)
            throws ImageInterpreterException, IOException {

        String mimetype = StreamUtils.getMimeTypeFromUrl(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypasswd);
        ImageFileFormat iff = getImageFileFormatFromMimeType(mimetype);
        if (iff == null) {
            // check extension
            iff = getImageFileFormatFromFileExtension(url.toString());
        }
        InputStream myInputStream = StreamUtils.getInputStreamFromUrl(url);
        ImageInterpreter myInterpreter = iff.getInterpreter(myInputStream);
        if (myInputStream != null) {
            myInputStream.close();
        }
        return myInterpreter;
    }

    /************************************************************************************
     * get {@link ImageInterpreter} for file format by using {@link FileInputStream} as parameter
     * 
     * @return requested ImageInterpreter for file format
     * @param inputFileStream as FileInputStream
     * @throws ImageInterpreterException
     ************************************************************************************/
    public ImageInterpreter getInterpreter(InputStream inputFileStream) throws ImageInterpreterException {
        switch (this) {
            case TIFF:
                return new TiffInterpreter(inputFileStream);
            case PNG:
                return new PngInterpreter(inputFileStream);
            case JPG:
                return new JpegInterpreter(inputFileStream);
            case JP2:
                return new JpegTwoThousandInterpreter(inputFileStream);
            default:
                return new JpegInterpreter(inputFileStream);
        }
    }

    /************************************************************************************
     * get {@link ImageInterpreter} for file format by using {@link RenderedImage} as parameter
     * 
     * @param image the image as RenderedImage
     * @return requested ImageInterpreter for file format
     ************************************************************************************/
    public ImageInterpreter getInterpreter(RenderedImage image) {
        switch (this) {
            case TIFF:
                return new TiffInterpreter(image);
            case PNG:
                return new PngInterpreter(image);
            case JPG:
                return new JpegInterpreter(image);
            case JP2:
                return new JpegTwoThousandInterpreter(image);
            default:
                return new JpegInterpreter(image);
        }
    }

    /************************************************************************************
     * get {@link ImageFileFormat} for file format by using the file extension as {@link String} parameter
     * 
     * @param extension the file extension ob the file to use
     * @return requested ImageFileFormat
     ************************************************************************************/
    public static ImageFileFormat getImageFileFormatFromFileExtension(String urlstring) {
        if (urlstring == null) {
            return null;
        }

        String extensions[] = urlstring.split("\\.");

        if (extensions.length == 0) {
            // no extension found
            return null;

        }
        String ext = extensions[(extensions.length) - 1].toLowerCase(Locale.getDefault()).trim();
        if (ext.equals("tiff") || ext.equals("tif")) {
            return TIFF;
        } else if (ext.equals("png")) {
            return PNG;
        } else if (ext.equals("jpeg") || ext.equals("jpg")) {
            return JPG;
        } else if (ext.equals("jp2")) {
            return JP2;
        } else {
            return JPG;
        }
    }

    /************************************************************************************
     * get {@link ImageFileFormat} for by given Mime Type as String parameter
     * 
     * @param mimetype the mimetype to use
     * @return requested ImageFileFormat
     ************************************************************************************/
    public static ImageFileFormat getImageFileFormatFromMimeType(String mimetype) {
        if (mimetype == null) {
            return null;
        }
        String mt = mimetype.toLowerCase(Locale.getDefault()).trim();
        if (mt.equals("image/tiff")) {
            return TIFF;
        } else if (mt.equals("image/png")) {
            return PNG;
        } else if (mt.equals("image/jpeg")) {
            return ImageFileFormat.JPG;
        } else if (mt.equals("image/jpg")) {
            return ImageFileFormat.JPG;
        } else if (mt.equals("image/jp2")) {
            return JP2;
        } else {
            return null;
        }
    }

}