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
package de.intranda.test_ics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import magick.CompressionType;
import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;

import org.apache.log4j.Logger;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.TiffInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;

/**
 * The Class ImageHelper.
 */
public class ImageHelper {

    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ImageHelper.class);

    /**
     * Open image.
     * 
     * @param sourcePath the source path
     * @return the image manager
     */
    public static ImageManager openImage(String sourcePath) {

        ImageManager sourcemanager = null;

        try {
            sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return sourcemanager;
    }

    public static Rectangle getBounds(String sourcePath) {

        ImageManager sourcemanager = null;

        try {
            sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());
            int width = sourcemanager.getMyInterpreter().getWidth();
            int height = sourcemanager.getMyInterpreter().getHeight();

            return new Rectangle(width, height);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        // LOGGER.info("WIDTH:" + sourcemanager.getMyInterpreter().getWidth());
        // LOGGER.info("HEIGHT:" +
        // sourcemanager.getMyInterpreter().getHeight());

        return null;
    }

    public static void covertImageToTIFF(String sourcePath, String destPath, int compression) throws ParameterNotSupportedException,
            MalformedURLException, ImageManagerException {
        ImageManager sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());

        RenderedImage renderedImage = getRenderedImage(sourcemanager);
        BufferedImage bImage = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedImage);

        try {
            writeTIFFImage(bImage, destPath, compression, sourcemanager.getMyInterpreter().getXResolution(), sourcemanager.getMyInterpreter()
                    .getYResolution());
        } finally {
            bImage.flush();
        }
    }

    public static void createImageForOCR(String sourcePath, String destPath, Rectangle selection) {
        ImageManager sourcemanager = null;
        BufferedImage sourceImage = null;
        BufferedImage destImage = null;
        try {
            sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());
            RenderedImage renderedImage = getRenderedImage(sourcemanager);

            sourceImage = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedImage);
            destImage = new BufferedImage(selection.width, selection.height, sourceImage.getType());

            if (!destImage.createGraphics().drawImage(sourceImage.getSubimage(selection.x, selection.y, selection.width, selection.height), 0, 0,
                    null)) {
                LOGGER.error("Could not convert File."); //$NON-NLS-1$
            }

            writeTIFFImageForOCR(destImage, destPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (sourceImage != null) {
                sourceImage.flush();
            }
            if (destImage != null) {
                destImage.flush();
            }
        }

        // LOGGER.info("WIDTH:" + sourcemanager.getMyInterpreter().getWidth());
        // LOGGER.info("HEIGHT:" +
        // sourcemanager.getMyInterpreter().getHeight());

    }

    /**
     * Scale image.
     * 
     * @param sourcemanager the sourcemanager
     * @param width the width
     * @param height the height
     * @return the rendered image
     */
    public static RenderedImage getRenderedImage(ImageManager sourcemanager) {
        /*
         * -------------------------------- set the defaults --------------------------------
         */
        int angle = 0;
        int scaleX = 100;
        int scaleY = 100;
        int scaleType = ImageManager.SCALE_BY_PERCENT;
        LinkedList<String> highlightCoordinateList = null;
        Color highlightColor = null;
        Watermark myWatermark = null;

        RenderedImage targetImage = null;

        try {
            LOGGER.debug("Calling scaleImageByPixel from ImageHelper.getRenderedImage");
            targetImage =
                    sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, false,
                            ImageManager.BOTTOM);
        } catch (ImageManipulatorException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return targetImage;
    }

    public static RenderedImage scaleImage(ImageManager sourcemanager, int width, int height) {
        /*
         * -------------------------------- set the defaults --------------------------------
         */
        int angle = 0;
        int scaleX = 100;
        int scaleY = 100;
        int scaleType = ImageManager.SCALE_BY_PERCENT;
        LinkedList<String> highlightCoordinateList = null;
        Color highlightColor = null;
        Watermark myWatermark = null;

        /*
         * -------------------------------- rotate --------------------------------
         */angle = 0;

        /*
         * -------------------------------- width: scale image to fixed width --------------------------------
         */

        scaleX = width;
        scaleY = height;

        int sourceImageWidth = sourcemanager.getMyInterpreter().getWidth();
        int sourceImageHeight = sourcemanager.getMyInterpreter().getHeight();

        if (width / (double) height < sourceImageWidth / (double) sourceImageHeight) {
            scaleType = ImageManager.SCALE_BY_WIDTH;
            scaleX = width;
            scaleY = 0;
            // LOGGER.info("scale image to width:" + scaleX);
        } else {
            scaleType = ImageManager.SCALE_BY_HEIGHT;
            scaleX = 0;
            scaleY = height;
            // LOGGER.info("scale image to height:" + scaleY);
        }

        /*
         * -------------------------------- prepare target --------------------------------
         */
        // change to true if watermark should scale

        RenderedImage targetImage = null;

        try {
            LOGGER.debug("Calling scaleImageByPixel from ImageHelper.scaleImage");
            targetImage =
                    sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, false,
                            ImageManager.BOTTOM);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return targetImage;
    }

    public static RenderedImage rotateImage(ImageManager sourcemanager, int angle) {
        /*
         * -------------------------------- set the defaults --------------------------------
         */
        int scaleX = 100;
        int scaleY = 100;
        int scaleType = ImageManager.SCALE_BY_PERCENT;
        LinkedList<String> highlightCoordinateList = null;
        Color highlightColor = null;
        Watermark myWatermark = null;

        /*
         * -------------------------------- prepare target --------------------------------
         */
        // change to true if watermark should scale

        RenderedImage targetImage = null;

        try {
            LOGGER.debug("Calling scaleImageByPixel from ImageHelper.rotateImage");
            targetImage =
                    sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, false,
                            ImageManager.BOTTOM);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return targetImage;
    }

    /**
     * Write image.
     * 
     * @param targetImage the target image
     * @param targetPath the target path
     */
    public static void writeImage(RenderedImage targetImage, String targetPath) {
        int dotPos = targetPath.lastIndexOf('.');
        String extension = targetPath.substring(dotPos);

        ImageFileFormat targetFormat = ImageFileFormat.getImageFileFormatFromFileExtension(extension);
        ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
                                                                        // file

        /*
         * -------------------------------- set file name and attachment header from parameter or from configuration --------------------------------
         */

        wi.setXResolution(100);
        wi.setYResolution(100);

        LOGGER.debug("start writing"); //$NON-NLS-1$

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetPath);
            wi.writeToStream(null, fos);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        LOGGER.debug("finished"); //$NON-NLS-1$
    }

    public static void writeTIFFImage(RenderedImage targetImage, String targetPath, int compression, float xResolution, float yResolution)
            throws ParameterNotSupportedException {
        ImageFileFormat targetFormat = ImageFileFormat.TIFF;
        ImageInterpreter wi = targetFormat.getInterpreter(targetImage);
        wi.setWriterCompressionType(compression);
        wi.setXResolution(xResolution);
        wi.setYResolution(yResolution);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetPath);
            wi.writeToStream(null, fos);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public static void writeTIFFImageForOCR(RenderedImage targetImage, String targetPath) {
        //int dotPos = targetPath.lastIndexOf("."); //$NON-NLS-1$

        ImageFileFormat targetFormat = ImageFileFormat.TIFF;
        ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
                                                                        // file

        /*
         * -------------------------------- set file name and attachment header from parameter or from configuration --------------------------------
         */

        wi.setXResolution(100);
        wi.setYResolution(100);

        try {
            wi.setWriterCompressionType(TiffInterpreter.COMPRESSION_NONE);
        } catch (ParameterNotSupportedException e1) {
            LOGGER.error(e1.getMessage());
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetPath);
            wi.writeToStream(null, fos);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public static void writeTIFFImageTest(RenderedImage targetImage, String targetPath) {
        //int dotPos = targetPath.lastIndexOf("."); //$NON-NLS-1$

        ImageFileFormat targetFormat = ImageFileFormat.TIFF;
        ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
                                                                        // file

        /*
         * -------------------------------- set file name and attachment header from parameter or from configuration --------------------------------
         */

        wi.setXResolution(100);
        wi.setYResolution(100);

        try {
            wi.setWriterCompressionType(TiffInterpreter.COMPRESSION_NONE);
        } catch (ParameterNotSupportedException e1) {
            LOGGER.error(e1.getMessage());
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetPath);
            wi.writeToStream(null, fos);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Exchange file extension.
     * 
     * @param fileName the file name
     * @param newExtension the new extension
     * @return the string
     */
    public static String exchangeFileExtension(String fileName, String newExtension) {
        int dotPos = fileName.lastIndexOf('.');
        if (dotPos >= 0) {
            String name = fileName.substring(0, dotPos);
            return name + "." + newExtension; //$NON-NLS-1$
        } else {
            return fileName + "." + newExtension; //$NON-NLS-1$
        }
    }

    /*
     * public static String createThumbsForDir(String dirName, String thumbDirName, int width, int height, boolean force) {
     * 
     * 
     * File dir = new File(dirName); File tempdir = new File(dirName + File.separator + thumbDirName);
     * 
     * if (tempdir.exists()) { if (!force) return tempdir.getAbsolutePath(); else if (!FileUtils.deleteDir(tempdir)) {
     * LOGGER.error("FEHLER: Thumbnail-Directory konnte nicht gel�scht werden." ); return null; } }
     * 
     * if (!tempdir.mkdirs()) { LOGGER.error("Thumbnail-Directory konnte nicht geschrieben werden."); return null; }
     * 
     * ArrayList<String> filterList = new ArrayList<String>();
     * 
     * filterList.add("tif"); filterList.add("tiff"); filterList.add("jpg"); filterList.add("jpeg");
     * 
     * FileExtensionsFilter filter = new FileExtensionsFilter(filterList);
     * 
     * File[] files = dir.listFiles(filter);
     * 
     * for (int i = 0; i < files.length; i++) { File origFile = files[i];
     * 
     * String extension = "jpg";
     * 
     * String targetFileName = tempdir.getAbsolutePath() + File.separator + exchangeFileExtension(origFile.getName(), extension);
     * 
     * LOGGER.info("Target Filename: " + targetFileName);
     * 
     * writeImage( scaleImage(openImage(origFile.getAbsolutePath()), width, height), targetFileName);
     * 
     * }
     * 
     * return tempdir.getAbsolutePath(); }
     */
    // public static void main(String[] args) {
    // long startMillis = System.currentTimeMillis();
    //
    //		System.out.println("START"); //$NON-NLS-1$
    //
    //		String path = "C:\\Users\\Karsten\\Desktop\\testbilder\\00000006.tif"; //$NON-NLS-1$
    //		//String targetpath = "C:\\Users\\Karsten\\Desktop\\testbilder\\00000006_contentserver.tif"; //$NON-NLS-1$
    //
    // ImageManager imageManager = openImage(path);
    //
    // long stopMillis = System.currentTimeMillis();
    //		System.out.println("OPEN:" + (stopMillis - startMillis)); //$NON-NLS-1$
    // startMillis = System.currentTimeMillis();
    //
    // RenderedImage renderedImage = rotateImage(imageManager, 90);
    //
    // // stopMillis = System.currentTimeMillis();
    // // System.out.println("ROTATED:" + (stopMillis-startMillis));
    // // startMillis = System.currentTimeMillis();
    //
    // // RenderedImage renderedImage = scaleImage(imageManager, 100, 100);
    //
    // System.out.println(renderedImage.getColorModel());
    // System.out.println(renderedImage.getSampleModel());
    //
    // stopMillis = System.currentTimeMillis();
    //		System.out.println("SCALED:" + (stopMillis - startMillis)); //$NON-NLS-1$
    // startMillis = System.currentTimeMillis();
    //
    // //BufferedImage bum =
    // ImageManipulator.fromRenderedToBufferedNoAlpha(renderedImage);
    //
    // //ImageData imageData = GalleryItemRenderer.convertToSWTFast(bum);
    // //writeTIFFImage(renderedImage, targetpath);
    //
    // stopMillis = System.currentTimeMillis();
    //		System.out.println("WRITTEN:" + (stopMillis - startMillis)); //$NON-NLS-1$
    // }

    public static void main(String[] args) {

        DecimalFormat imageNameFormat = new DecimalFormat("00000000");

        int runs = 1;
        long[] timesIO = new long[runs];
        long[] timesMagick = new long[runs];

        System.out.println("START"); //$NON-NLS-1
        float scale = 0.5f;

        for (int i = 0; i < runs; i++) {
            String imagePath = "/opt/digiverso/viewer/tiff/MS137_AJ95_ADD1/" + imageNameFormat.format(i + 1) + ".jpg"; //$NON-NLS-1$
            // File imageFile = new File(path);
            FileOutputStream ostrIO = null, ostrMagick = null;
            BufferedOutputStream buffostrMagick = null, buffostrIO = null;
            try {
                ostrIO = new FileOutputStream("testIO.jpg");
                buffostrIO = new BufferedOutputStream(ostrIO);
                ostrMagick = new FileOutputStream("testMagick.jpg");
                buffostrMagick = new BufferedOutputStream(ostrMagick);
            } catch (FileNotFoundException e) {
                System.out.println("Unable to find file");
            }
            try {
                URL pathUrl = new URL("file://" + imagePath);
                long start1 = System.currentTimeMillis();
                ImageInterpreter myInterpreter = ImageFileFormat.getInterpreter(pathUrl, null, null, null, null);
                RenderedImage inImage = myInterpreter.getRenderedImage();
                // RenderedImage inImage = ImageIO.read(imageFile);
                RenderedImage outImage = scaleNextNeighbor(inImage, scale, scale);
                ImageIO.write(outImage, "jpeg", buffostrIO);
                long end1 = System.currentTimeMillis();
                timesIO[i] = end1 - start1;
                System.out.println(timesIO[i]);
            } catch (MalformedURLException e) {
                LOGGER.error(e);
            } catch (ImageInterpreterException e) {
                LOGGER.error(e);
            } catch (IOException e) {
                LOGGER.error(e);
            }

            try {
                ImageInfo imageInfo = new ImageInfo(imagePath);
                long start2 = System.currentTimeMillis();
                MagickImage inImage = new MagickImage(imageInfo);
                MagickImage outImage = scaleImage(inImage, scale, scale);
                // outImage.setFileName("testMagick.jpg");
                // outImage.writeImage(imageInfo);
                long end2 = System.currentTimeMillis();
                byte[] imageBlob = outImage.imageToBlob(imageInfo);
                ImageInfo outInfo = new ImageInfo();
                outInfo.setCompression(CompressionType.JPEGCompression);
                outInfo.setQuality(40);
                outInfo.getQuality();
                outImage = new MagickImage(outInfo, imageBlob);
                imageBlob = outImage.imageToBlob(outInfo);
                buffostrMagick.write(imageBlob);
                timesMagick[i] = end2 - start2;
                System.out.println(timesMagick[i]);
            } catch (MalformedURLException e) {
                LOGGER.error(e);
            } catch (MagickException e) {
                LOGGER.error(e);
            } catch (IOException e) {
                LOGGER.error(e);
            }

            try {
                if (buffostrIO != null) {
                    buffostrIO.flush();
                    buffostrIO.close();
                }
                if (buffostrMagick != null) {
                    buffostrMagick.flush();
                    buffostrMagick.close();
                }
            } catch (IOException e) {
                LOGGER.error("Error closing output streams");
            }
        } // loop end

        long avgIO = 0, avgMagick = 0;
        for (int i = 0; i < runs; i++) {
            avgIO += timesIO[i];
            avgMagick += timesMagick[i];
        }
        avgIO /= runs;
        avgMagick /= runs;

        System.out.println("Average time to process image with ImageIO: " + avgIO + " ms");
        System.out.println("Average time to process image with ImageMagick: " + avgMagick + " ms");

    }

    /**
     * Delete file extension.
     * 
     * @param fileName the file name
     * @return the string
     */
    public static String deleteFileExtension(String fileName) {
        int dotPos = fileName.lastIndexOf('.');
        String name = fileName;
        if (dotPos > 0) {
            name = fileName.substring(0, dotPos);
        }
        return name;
    }

    /**
     * Returns the color depth of the image at the given path.
     * 
     * @param imagePath The path of the image to check.
     * @return Color depth in bits.
     */
    public static int getColorDepth(String imagePath) {
        ImageManager imageManager = openImage(imagePath);
        return imageManager.getMyInterpreter().getColordepth();
    }

    public static String[] getImageFileExtensions() {
        String[] extensions = { "*.jpg", "*.jpeg", "*.tiff", "*.tif" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return extensions;
    }

    public void doGeneration(File[] imageFiles, File pdfFile) throws IOException, DocumentException, OutOfMemoryError {

        if (imageFiles.length > 0) {

            // allImages = reverseFileList(allImages);
            Document pdfDocument = null;
            @SuppressWarnings("unused")
            int pageCount = 1;
            PdfWriter pdfWriter = null;

            pdfDocument = new Document();
            FileOutputStream outputPdfFile = new FileOutputStream(pdfFile);
            pdfWriter = PdfWriter.getInstance(pdfDocument, outputPdfFile);
            pdfDocument.open();

            for (File imageFile : imageFiles) {
                addPage(imageFile, pdfWriter, pdfDocument, 1, 0);
                pageCount++;
            }

            pdfDocument.close();
            pdfWriter.close();
            try {
                if (outputPdfFile != null) {
                    outputPdfFile.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error on closing fileoutputstream");
            }
        }
    }

    @SuppressWarnings("unused")
    private void addFrontPage(File frontPage, File pdfFile) throws IOException, DocumentException {
        File tempFile = new File(pdfFile.getParent(), System.currentTimeMillis() + ".pdf");
        pdfFile.renameTo(tempFile);
        PdfReader reader1 = new PdfReader(frontPage.getAbsolutePath());
        PdfReader reader2 = new PdfReader(tempFile.getAbsolutePath());
        PdfCopyFields copy = new PdfCopyFields(new FileOutputStream(pdfFile));
        copy.addDocument(reader1);
        copy.addDocument(reader2);
        copy.close();
        if (tempFile != null && tempFile.isFile()) {
            tempFile.delete();
        }
    }

    @SuppressWarnings("unused")
    private void addPage(File imageFile, PdfWriter pdfWriter, Document pdfDocument, float shrinkRatio, float rotationDegree)
            throws DocumentException, IOException {

        float pointsPerInch = 200.0f;
        Image pageImage = null;
        float pageImageHeight = 0, pageImageWidth = 0;
        boolean lowMemory = (shrinkRatio == 1 ? false : true);

        URL inputImage = imageFile.toURI().toURL();

        pdfWriter.setFullCompression();
        pdfWriter.setStrictImageSequence(true);
        pdfWriter.setLinearPageMode();

        LOGGER.debug("Out of memory on loading image for pdf generation");
        // ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedImage bitmap = ImageIO.read(imageFile);
        // LOGGER.debug( "Size of temporary image bitmap: Width = " + bitmap.getWidth() + "; Height = " + bitmap.getHeight());
        LOGGER.debug("Reading file " + imageFile.getAbsolutePath());
        pageImage = Image.getInstance(bitmap, null, false);
        bitmap.flush();
        // stream.close();

        pageImage.setRotationDegrees(-rotationDegree);
        LOGGER.debug("Image dimensions: Width = " + pageImage.getWidth() + "; Height = " + pageImage.getHeight());
        pageImageHeight = pageImage.getHeight();
        pageImageWidth = pageImage.getWidth();
        pageImage.setAbsolutePosition(0, 0);
        // Rectangle pageRect = new Rectangle(pageImageWidth/shrinkRatio, pageImageHeight/shrinkRatio);
        com.lowagie.text.Rectangle pageRect = new com.lowagie.text.Rectangle(pageImageWidth, pageImageHeight);
        LOGGER.debug("Creating rectangle: Width = " + pageRect.getWidth() + "; Height = " + pageRect.getHeight());
        pdfDocument.setPageSize(pageRect);

        if (pdfDocument.isOpen()) {
            pdfDocument.newPage();
            pdfWriter.getDirectContent().addImage(pageImage);

        } else {
            pdfDocument.open();
            pdfWriter.getDirectContent().addImage(pageImage);
        }
        pdfWriter.flush();
        System.gc();
    }

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

    public static MagickImage scaleImage(MagickImage inImage, float internalScaling_x, float internalScaling_y) {
        try {
            Dimension imageDim = inImage.getDimension();
            MagickImage outImage = inImage.scaleImage((int) (imageDim.width * internalScaling_x), (int) (imageDim.height * internalScaling_y));
            return outImage;
        } catch (MagickException e) {
            LOGGER.error("Error scaling image");
            return inImage;
        }
    }

    public static FilenameFilter ImageFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            boolean validImage = false;
            // jpeg
            if (name.endsWith("jpg") || name.endsWith("JPG") || name.endsWith("jpeg") || name.endsWith("JPEG")) {
                validImage = true;
            }
            if (name.endsWith(".tif") || name.endsWith(".TIF")) {
                validImage = true;
            }
            // png
            if (name.endsWith(".png") || name.endsWith(".PNG")) {
                validImage = true;
            }
            // gif
            if (name.endsWith(".gif") || name.endsWith(".GIF")) {
                validImage = true;
            }
            // jpeg2000
            if (name.endsWith(".jp2") || name.endsWith(".JP2")) {
                validImage = true;
            }

            return validImage;
        }
    };
}
