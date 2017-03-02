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
package de.unigoettingen.sub.commons.contentlib.pdflib;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfBoolean;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfICCBased;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfIndirectObject;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPage;
import com.lowagie.text.pdf.PdfPageLabels;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.xml.xmp.DublinCoreSchema;
import com.lowagie.text.xml.xmp.PdfSchema;
import com.lowagie.text.xml.xmp.XmpSchema;
import com.lowagie.text.xml.xmp.XmpWriter;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.exceptions.PDFManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator.MergingMode;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegTwoThousandInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.TiffInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.servlet.ServletWatermark;
import de.unigoettingen.sub.commons.contentlib.servlet.Util;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.util.datasource.Structure;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;

/*******************************************************************************
 * PDFManager controls the generation of pdf files from images.
 * 
 * @version 20.11.2010
 * @author Markus Enders
 * @author Igor Toker
 ********************************************************************************/
// TODO: This should use the ImageSource interface
/**
 * @author itoker
 * 
 */
public class PDFManager {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(PDFManager.class);

    /** Image, that used for pages with error. */
    // private static final String ERROR_PAGE = Loader.getResource("errorfile.jpg").toExternalForm();

    // ----------------------------------------------------------------------------------------

    public enum PdfPageSize {
        /** every page has the size of the image */
        ORIGINAL("original"),

        /** every page has A4 size, the page image is horizontally centered */
        A4("A4"),

        /**
         * every page has A4 size, the page image is horizontally centered. A small, black bounding box is drawn around the original page.
         */
        A4BOX("A4Box");
        private String name;

        private PdfPageSize(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum PaperSize {
        A4(210, 290);
        public int width;
        public int height;

        private PaperSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    // ----------------------------------------------------------------------------------------

    public enum Embedd {
        ORIGBYTESTREAM, /* RENDEREDIMAGE, */JPEG, LOSSYJPEG2000, LOSSLESSJPEG2000, TIFFG4
    }

    /** The always use rendered image. */
    Boolean alwaysUseRenderedImage = false; // uses rendered Image and embedd
                                            // this into PDF
    /** The always compress to jpeg. */
    Boolean alwaysCompressToJPEG = false;

    /** The embedd bitonal image. */
    Embedd embeddBitonalImage = Embedd.ORIGBYTESTREAM;

    /** The embedd greyscale image. */
    Embedd embeddGreyscaleImage = Embedd.ORIGBYTESTREAM;

    /** The embedd color image. */
    Embedd embeddColorImage = Embedd.ORIGBYTESTREAM;
    // ----------------------------------------------------------------------------------------

    /** The creator. */
    private String creator = null;

    /** The author. */
    private String author = null;

    /** The title. */
    private String title = null;

    /** The subject. */
    private String subject = null;

    /** The keyword. */
    private String keyword = null;
    // ----------------------------------------------------------------------------------------

    /** The pdftitlepage. */
    private PDFTitlePage pdftitlepage = null; // object defining the contents
                                              // of the PDF title page
    /** The image names. */
    private Map<Integer, String> imageNames = null; // contains all the page
                                                    // numbers
    /** The image urls. */
    private Map<Integer, UrlImage> imageURLs = null;

    /**
     * A PDF file may consists of several parts. These parts may have their own title page. The integer contains the pagenumber before the appropriate
     * title page is added to the PDF.
     */
    private Map<Integer, PDFTitlePage> pdftitlepages = null;

    /** The structure list. */
    private List<PDFBookmark> structureList = null;

    /** The pdfa. */
    private boolean pdfa = true; // if set to true, PDF/A is created;
    // otherwise not
    /** The iccprofile. */
    private ICC_Profile iccprofile = null; // ICC color profile; needed for
    // PDFA

    // ----------------------------------------------------------------------------------------
    /** The httpproxyhost. */
    String httpproxyhost = null;

    /** The httpproxyport. */
    String httpproxyport = null;

    /** The httpproxyuser. */
    String httpproxyuser = null;

    /** The httpproxypassword. */
    String httpproxypassword = null;

    // ----------------------------------------------------------------------------------------

    /****************************************************************************
     * The PDFManager class organizes all pdf generation handlings depending on its parameters the images get compressed, is written as pdf/a etc.
     * 
     * The {@link Integer} for the images in HashMap has to start at 1 for references of image names
     * *************************************************************************/
    // public PDFManager() {
    // }

    /***************************************************************************************************************
     * Constructor for {@link PDFManager}
     * 
     * @param inPages {@link Map} with {@link UrlImage}
     ***************************************************************************************************************/
    public PDFManager(Map<Integer, UrlImage> inPages) {
        imageURLs = inPages;
        LOGGER.debug("PDFManager intstantiated");
    }

    /***************************************************************************
     * Constructor for {@link PDFManager}.
     * 
     * @param inPages a {@link Map} with {@link PdfPage}
     * @param inPdfa a boolean set to true, if the pdf should be written in pdf/a mode
     ****************************************************************************/
    public PDFManager(Map<Integer, UrlImage> inPages, boolean inPdfa) {
        this.pdfa = inPdfa;
        imageURLs = inPages;
        LOGGER.debug("PDFManager intstantiated");
    }

    /***************************************************************************
     * Creates a PDF, which is streams to the OutputStream out.
     * 
     * @param out {@link OutputStream}
     * @param pagesizemode {@link PdfPageSize}
     * 
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileNotFoundException the file not found exception
     * @throws ImageManagerException the image manager exception
     * @throws PDFManagerException the PDF manager exception
     * @throws ImageInterpreterException the image interpreter exception
     * @throws URISyntaxException
     ****************************************************************************/
    public void createPDF(OutputStream out, PdfPageSize pagesizemode, Watermark myWatermark) throws ImageManagerException, FileNotFoundException,
            IOException, PDFManagerException, ImageInterpreterException, URISyntaxException {

        Document pdfdoc = null;
        PdfWriter writer = null;

        Rectangle pagesize = null; // pagesize of the first page
        PdfPageLabels pagelabels = null; // object to store all page labels

        try {
            if ((imageURLs == null) || (imageURLs.isEmpty())) {
                throw new PDFManagerException("No URLs for images available, HashMap is null or empty");
            }

            // set the page sizes & pdf document
            pdfdoc = setPDFPageSizeForFirstPage(pagesizemode, pagesize);

            // writer for creating the PDF
            writer = createPDFWriter(out, pdfdoc);

            // set metadata for PDF as author and title
            // ------------------------------------------------------------------------------------
            if (this.title != null) {
                pdfdoc.addTitle(this.title);
            }
            if (this.author != null) {
                pdfdoc.addAuthor(this.author);
            }
            if (this.keyword != null) {
                pdfdoc.addKeywords(this.keyword);
            }
            if (this.subject != null) {
                pdfdoc.addSubject(this.subject);
            }
            // add title page to PDF
            if (pdftitlepage != null) {
                // create a title page
                pdftitlepage.render(pdfdoc);
            }

            // iterate over all files, they must be ordered by the key
            // the key contains the page number (as integer), the String
            // contains the Page name
            // ----------------------------------------------------------------------

            pagelabels = addAllPages(pagesizemode, writer, pdfdoc, myWatermark);

            // add page labels
            if (pagelabels != null) {
                writer.setPageLabels(pagelabels);
            }

            // create the required xmp metadata
            // for pdfa
            if (pdfa) {
                writer.createXmpMetadata();
            }
        } catch (ImageManagerException e) {
            if (pdfdoc != null) {
                pdfdoc.close();
            }
            if (writer != null) {
                writer.close();
            }
            throw e;
        } catch (PDFManagerException e) {
            if (pdfdoc != null) {
                pdfdoc.close();
            }
            if (writer != null) {
                writer.close();
            }
            throw e;
        } catch (ImageInterpreterException e) {
            if (pdfdoc != null) {
                pdfdoc.close();
            }
            if (writer != null) {
                writer.close();
            }
            throw e;
        } catch (IOException e) {
            if (pdfdoc != null) {
                pdfdoc.close();
            }
            if (writer != null) {
                writer.close();
            }
            throw e;
        }
        // close documents and writer
        try {
            if (pdfdoc != null && pdfdoc.isOpen()) {
                pdfdoc.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IllegalStateException e) {
            LOGGER.warn("Caught IllegalStateException when closing pdf document.");
        } catch (NullPointerException e) {
            throw new PDFManagerException("Nullpointer occured while closing pdfwriter");
        }
        LOGGER.debug("PDF document and writer closed");
    }

    /******************************************************************************************************
     * Adds the all pages.
     * 
     * @param pagesizemode {@link PdfPageSize}
     * @param writer {@link PdfWriter}
     * @param pdfdoc {@link Document}
     * @return {@link PdfPageLabels}
     * 
     * 
     * @throws ImageInterpreterException the image interpreter exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MalformedURLException the malformed url exception
     * @throws PDFManagerException the PDF manager exception
     * @throws ImageManagerException
     *******************************************************************************************************/
    private PdfPageLabels addAllPages(PdfPageSize pagesizemode, PdfWriter writer, Document pdfdoc, Watermark myWatermark)
            throws ImageInterpreterException, IOException, MalformedURLException, PDFManagerException, ImageManagerException {

        PdfPageLabels pagelabels = new PdfPageLabels();
        int pageadded = 0;

        // sort the HashMap by the KeySet (pagenumber)
        Map<Integer, UrlImage> sortedMap = new TreeMap<Integer, UrlImage>(imageURLs);

        float scalefactor = 1; // scaling factor of the image
        int page_w = PaperSize.A4.width;
        int page_h = PaperSize.A4.height;
        LOGGER.debug("iterate over " + imageURLs.size() + " pages.");
        for (Integer imageKey : sortedMap.keySet()) {

            Watermark watermark = myWatermark;
            Image pdfImage = null; // PDF-Image
            LOGGER.debug("Writing page " + imageKey);

            boolean errorPage = false; // true if the image does not exists
            URL errorUrl = null; // url of the image that does not exists

            // ------------------------------------------------------------------------------------------------
            // Title page available. Render it in pdftitlepage
            // ------------------------------------------------------------------------------------------------
            if ((pdftitlepages != null) && (pdftitlepages.get(imageKey) != null)) {
                // title page
                PDFTitlePage pdftitlepage = pdftitlepages.get(imageKey);
                // create new PDF page
                try {
                    pdfdoc.setPageSize(PageSize.A4);
                    pdfdoc.setMargins(pdftitlepage.getLeftMargin(), pdftitlepage.getRightMargin(), pdftitlepage.getTopMargin(), pdftitlepage
                            .getBottomMargin());
                    pageadded++;
                    pdfdoc.newPage(); // create new page
                    // set page name
                    pagelabels.addPageLabel(pageadded, PdfPageLabels.EMPTY, "-");
                } catch (Exception e1) {
                    throw new PDFManagerException("PDFManagerException occured while creating new page in PDF", e1);
                }
                // render title page
                pdftitlepage.render(pdfdoc);
            }

            // ------------------------------------------------------------------------------------------------
            // Process image with imageKey
            // ------------------------------------------------------------------------------------------------
            UrlImage pdfpage = imageURLs.get(imageKey);
            if (pdfpage.getURL() != null) {
                boolean added = false;
                boolean scaled = false;
                URL url = pdfpage.getURL();
                // pdf hack
                if (ContentServerConfiguration.getInstance().getUsePdf()) {
                    LOGGER.debug("trying to find original pdf");
                    PdfContentByte pdfcb = null;
                    PdfReader pdfreader = null;
                    PdfImportedPage importpage = null;
                    try {
                        String pdfpath = ContentServerConfiguration.getInstance().getRepositoryPathPdf().replace("file:///", "");
                        LOGGER.debug("looking in " + pdfpath + " for pdf file");
                        String tiffPath = ContentServerConfiguration.getInstance().getRepositoryPathImages().replace("file:///", "");
                        // String urlString = url.toString();
                        int pageNumber = pdfpage.getPageNumber();
                        // UrlImage copy = new PDFPage(pdfpage);
                        URL pdfurl =
                                new URL(url.toString().replace(tiffPath, pdfpath).replace(url.toString().substring(url.toString().lastIndexOf(".")),
                                        ".pdf"));
                        LOGGER.debug("pdfurl = " + pdfurl);

                        if (new File(pdfurl.toURI()).exists()) {
                            LOGGER.debug("found pdf " + pdfurl.toURI());
                            // copy.setURL(pdfurl);
                            pdfcb = writer.getDirectContent();
                            pdfreader = new PdfReader(pdfurl);
                            importpage = writer.getImportedPage(pdfreader, pageNumber);

                            LOGGER.debug("creating orig pdf page");
                            Rectangle rect = pdfreader.getPageSize(pageNumber);
                            try {
                                pdfdoc.setPageSize(rect);
                                pdfdoc.newPage(); // create new page
                            } catch (Exception e1) {
                                throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
                            }
                            pageadded++;
                            pdfcb.addTemplate(importpage, 0, 0);
                            added = true;
                            LOGGER.debug("page:" + imageKey + "  url: " + pdfurl.toString());

                        }
                    } catch (URISyntaxException e) {
                        LOGGER.debug(e);
                        added = false;
                    } finally {
                        if (writer != null) {
                            writer.freeReader(pdfreader);
                            writer.flush();
                        }
                        if (pdfreader != null) {
                            pdfreader.close();
                        }
                    }
                }
                if (!added) {
                    // image file
                    LOGGER.debug("using image to create pdf page");
                    // try to get ImageInterpreter from url
                    ImageInterpreter myInterpreter =
                            ImageFileFormat.getInterpreter(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);

                    try {
                        // check preferred compression type depending on color depth
                        Embedd preferredEmbeddingType = Embedd.ORIGBYTESTREAM;
                        if (myInterpreter.getColordepth() == 1) {
                            // bitonal image
                            preferredEmbeddingType = embeddBitonalImage;
                        } else if ((myInterpreter.getColordepth() > 1) && (myInterpreter.getSamplesperpixel() == 1)) {
                            // greyscale image
                            preferredEmbeddingType = embeddGreyscaleImage;
                        } else {
                            // color image
                            preferredEmbeddingType = embeddColorImage;
                        }

                        // -------------------------------------------------------------------------------------
                        // Try to generate image
                        // -------------------------------------------------------------------------------------
                        pdfImage = generatePdfImageFromInterpreter(myInterpreter, preferredEmbeddingType, errorPage, watermark, errorUrl);

                        // -------------------------------------------------------------------------------------
                        // image couldn't be embedded yet (emergencyCase)
                        // -------------------------------------------------------------------------------------
                        if (pdfImage == null) {
                            LOGGER.warn("Couldn't use preferred method for embedding the image. Instead had to use JPEG or RenderedImage");

                            // Get Interpreter and rendered Image
                            // ---------------------------------------------------------------------------------------------------------------------------------
                            RenderedImage ri = null;
                            if (preferredEmbeddingType == embeddBitonalImage) {
                                ImageManager sourcemanager = new ImageManager(url);
                                boolean watermarkscale = ContentServerConfiguration.getInstance().getScaleWatermark(); // should we scale
                                // the watermark ?
                                ri =
                                        sourcemanager.scaleImageByPixel(3000, 0, ImageManager.SCALE_BY_WIDTH, 0, null, null, watermark,
                                                watermarkscale, ImageManager.BOTTOM);
                                myInterpreter = sourcemanager.getMyInterpreter();
                            } else {
                                ri = myInterpreter.getRenderedImage();
                                if (watermark != null) {
                                    ri = addwatermark(ri, watermark, 2);
                                    myInterpreter.setHeight(myInterpreter.getHeight() + watermark.getRenderedImage().getHeight());
                                }
                            }

                            // scale rendered image
                            // ---------------------------------------------------------------------------------------------------------------------------------
                            // float scalefactorX = 1;
                            // float scalefactorY = 1;
                            // switch (pagesizemode) {
                            // case ORIGINAL:
                            // scalefactorX = 72f / myInterpreter.getXResolution();
                            // scalefactorY = 72f / myInterpreter.getYResolution();
                            // break;
                            // default:
                            // /*
                            // * check, if the image needs to be scaled, because
                            // * it's bigger than A4 calculate the new scalefactor
                            // */
                            // float page_w_pixel = (float) (page_w *
                            // myInterpreter.getXResolution() / 25.4);
                            // float page_h_pixel = (float) (page_h *
                            // myInterpreter.getYResolution() / 25.4);
                            //
                            // float res_x = myInterpreter.getXResolution();
                            // float res_y = myInterpreter.getYResolution();
                            //
                            // long w = myInterpreter.getWidth(); // get height and
                            // // width
                            // long h = myInterpreter.getHeight();
                            //
                            // if ((w > page_w_pixel) || (h > page_h_pixel)) {
                            // LOGGER.debug("scale image to fit the page");
                            // float scalefactor_w = page_w_pixel / w;
                            // float scalefactor_h = page_h_pixel / h;
                            // if (scalefactor_h < scalefactor_w) {
                            // scalefactor = scalefactor_h;
                            // } else {
                            // scalefactor = scalefactor_w;
                            // }
                            // w = (long) (w * scalefactor);
                            // h = (long) (h * scalefactor);
                            // }
                            // scalefactorX = (72f / res_x) * scalefactor;
                            // scalefactorY = (72f / res_y) * scalefactor;
                            // break;
                            // }
                            // //scalefactorX = 0.2f;
                            // //scalefactorY = 0.2f;
                            // if (preferredEmbeddingType == embeddBitonalImage) {
                            // ImageManager sourcemanager = new ImageManager(url);
                            // ri = sourcemanager.scaleImageByPixel((int)
                            // (scalefactorX*100), (int) (scalefactorY*100),
                            // ImageManager.SCALE_BY_PERCENT, 0, null, null,
                            // watermark, true, ImageManager.BOTTOM);
                            // }else{
                            // ri = ImageManipulator.scaleInterpolationBilinear(ri,
                            // scalefactorX, scalefactorY);
                            // }
                            // myInterpreter.setHeight(ri.getHeight());
                            // myInterpreter.setWidth(ri.getWidth());
                            // scaled = true;

                            // add Watermark
                            // ---------------------------------------------------------------------------------------------------------------------------------
                            // ri = addwatermark(ri, watermark,
                            // ImageManager.BOTTOM);
                            // myInterpreter.setHeight(myInterpreter.getHeight() +
                            // watermark.getRenderedImage().getHeight());

                            // Try to write into pdfImage
                            // ---------------------------------------------------------------------------------------------------------------------------------
                            if (myInterpreter.getColordepth() > 1) {
                                // compress image if greyscale or color
                                ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
                                // JpegInterpreter jpint = new JpegInterpreter(ri);
                                // jpint.setXResolution(myInterpreter.getXResolution());
                                // jpint.setYResolution(myInterpreter.getYResolution());
                                // jpint.writeToStream(null, bytesoutputstream);
                                LOGGER.error("WritingJPEGImage");
                                writeJpegFromRenderedImageToStream(bytesoutputstream, ri, null, myInterpreter);
                                byte[] returnbyteArray = bytesoutputstream.toByteArray();
                                if (bytesoutputstream != null) {
                                    bytesoutputstream.flush();
                                    bytesoutputstream.close();
                                }
                                pdfImage = Image.getInstance(returnbyteArray);
                                returnbyteArray = null;
                            } else {
                                // its bitonal, but can't be embedded directly,
                                // need to go via RenderedImage
                                BufferedImage buffImage = ImageManipulator.fromRenderedToBuffered(ri);
                                pdfImage = Image.getInstance(buffImage, null, false);
                                if (myWatermark != null) {
                                    // create Image for Watermark
                                    JpegInterpreter jpint = new JpegInterpreter(myWatermark.getRenderedImage());
                                    ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
                                    jpint.setXResolution(myInterpreter.getXResolution());
                                    jpint.setYResolution(myInterpreter.getYResolution());
                                    jpint.writeToStream(null, bytesoutputstream);
                                    byte[] returnbyteArray = bytesoutputstream.toByteArray();
                                    jpint.clear();
                                    if (bytesoutputstream != null) {
                                        bytesoutputstream.flush();
                                        bytesoutputstream.close();
                                    }
                                    Image blaImage = Image.getInstance(returnbyteArray);
                                    returnbyteArray = null;
                                    // set Watermark as Footer at fixed position
                                    // (200,200)
                                    Chunk c = new Chunk(blaImage, 200, 200);
                                    Phrase p = new Phrase(c);
                                    HeaderFooter hf = new HeaderFooter(p, false);
                                    pdfdoc.setFooter(hf);
                                }
                                // pdfdoc.setPageSize(arg0)
                                // TODO das scheint nicht zu funktionieren... sollte
                                // dieser Code entfernt werden?

                            }
                        } // end of : if (pdfImage == null) {
                    } catch (BadElementException e) {
                        throw new PDFManagerException("Can't create a PDFImage from a Buffered Image.", e);
                    } catch (ImageManipulatorException e) {
                        LOGGER.warn(e);
                    }

                    // ---------------------------------------------------------------------------------------------------------
                    // place the image on the page
                    // ---------------------------------------------------------------------------------------------------------
                    if (pagesizemode == PdfPageSize.ORIGINAL) {
                        // calculate the image width and height in points, create
                        // the rectangle in points

                        Rectangle rect = null;
                        if (!scaled) {
                            float image_w_points = (myInterpreter.getWidth() / myInterpreter.getXResolution()) * 72;
                            float image_h_points = ((myInterpreter.getHeight()) / myInterpreter.getYResolution()) * 72;
                            rect = new Rectangle(image_w_points, image_h_points);
                        } else {
                            rect = new Rectangle(myInterpreter.getWidth(), myInterpreter.getHeight());
                        }

                        // create the pdf page according to this rectangle
                        LOGGER.debug("creating original page sized PDF page:" + rect.getWidth() + " x " + rect.getHeight());
                        pdfdoc.setPageSize(rect);

                        // create new page to put the content
                        try {
                            pageadded++;
                            pdfdoc.newPage();
                        } catch (Exception e1) {
                            throw new PDFManagerException("DocumentException occured while creating page " + pageadded + " in PDF", e1);
                        }

                        // scale image and place it on page; scaling the image does
                        // not scale the images bytestream
                        if (!scaled) {
                            pdfImage.scalePercent((72f / myInterpreter.getXResolution() * 100), (72f / myInterpreter.getYResolution() * 100));
                        }
                        pdfImage.setAbsolutePosition(0, 0); // set image to lower
                                                            // left corner

                        boolean result;
                        try {
                            result = pdfdoc.add(pdfImage); // add it to PDF
                            if (!result) {
                                throw new PDFManagerException("Image \"" + url.toString()
                                        + "\" can's be added to PDF! Error during placing image on page");
                            }
                        } catch (DocumentException e) {
                            throw new PDFManagerException("DocumentException occured while adding the image to PDF", e);
                        }
                    } else {
                        /*
                         * it is not the original page size PDF will contain only A4 pages
                         */
                        LOGGER.debug("creating A4 pdf page");

                        // create new page to put the content
                        try {
                            pageadded++;
                            pdfdoc.setPageSize(PageSize.A4);
                            pdfdoc.newPage(); // create new page
                        } catch (Exception e1) {
                            throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
                        }

                        float page_w_pixel = (float) (page_w * myInterpreter.getXResolution() / 25.4);
                        float page_h_pixel = (float) (page_h * myInterpreter.getYResolution() / 25.4);

                        float res_x = myInterpreter.getXResolution();
                        float res_y = myInterpreter.getYResolution();

                        long w = myInterpreter.getWidth(); // get height and width
                        long h = myInterpreter.getHeight();

                        /*
                         * if the page is landscape, we have to rotate the page; this is only done in PDF, the orig image bytestream is NOT rotated
                         */
                        if (w > h) {
                            LOGGER.debug("rotate image");
                            // must be rotated
                            pdfImage.setRotationDegrees(90);
                            // change width and height
                            long dummy = w;
                            w = h;
                            h = dummy;
                            // change the resolutions x and y
                            float dummy2 = res_x;
                            res_x = res_y;
                            res_y = dummy2;
                        }

                        /*
                         * check, if the image needs to be scaled, because it's bigger than A4 calculate the new scalefactor
                         */
                        if ((w > page_w_pixel) || (h > page_h_pixel)) {
                            LOGGER.debug("scale image to fit the page");
                            float scalefactor_w = page_w_pixel / w;
                            float scalefactor_h = page_h_pixel / h;
                            if (scalefactor_h < scalefactor_w) {
                                scalefactor = scalefactor_h;
                            } else {
                                scalefactor = scalefactor_w;
                            }
                            w = (long) (w * scalefactor);
                            h = (long) (h * scalefactor);
                        }
                        if (!scaled) {
                            pdfImage.scalePercent((72f / res_x * 100) * scalefactor, (72f / res_y * 100) * scalefactor);
                        }

                        // center the image on the page
                        // ---------------------------------------------------------------
                        float y_offset = 0; // y - offset
                        // get image size in cm; height
                        float h_cm = (float) (h / (res_x / 2.54));
                        // float w_cm = (float) (w / (res_y / 2.54)); // and width
                        if ((h_cm + 2) < (page_h / 10)) {
                            y_offset = 2 * 72f / 2.54f;
                        }
                        float freespace_x = ((page_w_pixel - w) / res_x * 72f);
                        float freespace_y = ((page_h_pixel - h) / res_y * 72f) - (y_offset);
                        // set position add image
                        pdfImage.setAbsolutePosition(freespace_x / 2, freespace_y);
                        boolean result;
                        try {
                            result = pdfdoc.add(pdfImage);
                        } catch (DocumentException e) {
                            LOGGER.error(e);
                            throw new PDFManagerException("DocumentException occured while adding the image to PDF", e);
                        }
                        if (!result) {
                            // placing the image in the PDF was not successful
                            throw new PDFManagerException("Image \"" + url.toString()
                                    + "\" can's be added to PDF! Error during placing image on page");
                        }

                        // draw box around the image page
                        // ------------------------------------------------------------------------------------------------
                        if (pagesizemode == PdfPageSize.A4BOX) {
                            LOGGER.debug("draw box around the image page");

                            // draw a black frame around the image
                            PdfContentByte pcb = writer.getDirectContent();

                            // calculate upper left corner of the box (measurment is
                            // in points)
                            float left_x = (freespace_x / 2);
                            float left_y = freespace_y;

                            // calculate the lower right corner of the box
                            // (measurement is in points)
                            float image_w_points = (w / res_x) * 72;
                            float image_h_points = (h / res_y) * 72;

                            pcb.setLineWidth(1f);
                            pcb.stroke();
                            pcb.rectangle(left_x, left_y, image_w_points, image_h_points);

                            pcb.stroke();
                        }

                    } // end of: if (pagesizemode == PdfPageSize.ORIGINAL) {
                    pdfImage = null;
                    myInterpreter.clear();
                    // writer.freeReader(new PdfReader(pdfpage.getURL()));
                } // end of : if (pdfpage.getURL() != null) {

                // ------------------------------------------------------------------------------------------------
                // it is a page from a PDF file which should be inserted
                // ------------------------------------------------------------------------------------------------
                else if (pdfpage.getClass() == PDFPage.class && ((PDFPage) pdfpage).getPdfreader() != null) {

                    PdfContentByte pdfcb = writer.getDirectContent();

                    PdfReader pdfreader = ((PDFPage) pdfpage).getPdfreader();
                    PdfImportedPage importpage = writer.getImportedPage(pdfreader, pdfpage.getPageNumber());

                    if (pagesizemode == PdfPageSize.ORIGINAL) {
                        LOGGER.debug("creating orig pdf page");
                        Rectangle rect = pdfreader.getPageSize(pdfpage.getPageNumber());
                        try {
                            pdfdoc.setPageSize(rect);
                            pdfdoc.newPage(); // create new page
                        } catch (Exception e1) {
                            throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
                        }
                        // add content
                        pageadded++;
                        pdfcb.addTemplate(importpage, 0, 0);

                    } else {
                        LOGGER.debug("creating A4 pdf page");
                        try {
                            pdfdoc.setPageSize(PageSize.A4);
                            pdfdoc.newPage(); // create new page
                        } catch (Exception e1) {
                            throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
                        }

                        // add content
                        pageadded++;
                        pdfcb.addTemplate(importpage, 0, 0);

                        // draw box
                        // if (pagesizemode == PdfPageSize.A4BOX) {
                        // FIXME: nichts implementiert ?
                        // }
                    }

                }
                // handle pagename
                if (imageNames != null) {
                    String pagename = imageNames.get(imageKey);

                    if (pagename != null) {
                        pagelabels.addPageLabel(pageadded, PdfPageLabels.EMPTY, pagename);
                    } else {
                        pagelabels.addPageLabel(pageadded, PdfPageLabels.EMPTY, "unnumbered");
                    }
                }
                // handle bookmarks and set destinator for bookmarks
                LOGGER.debug("handle bookmark(s) for page");

                PdfDestination destinator = new PdfDestination(PdfDestination.FIT);
                setBookmarksForPage(writer, destinator, imageKey); // the key in the
                writer.flush();
                // mashMap is the pagenumber

            } // end of while iterator over all pages

        }
        return pagelabels;

    }

    /***************************************************************************************************************
     * Writes JPEG to outputstream from {@link RenderedImage}. This method is used by
     * {@link PDFManager#generatePdfImageFromInterpreter(ImageInterpreter, Embedd, boolean, Watermark, URL)}
     * 
     * @param bytesoutputstream {@link ByteArrayOutputStream}
     * @param ri {@link RenderedImage}
     * @param preferredEmbeddingType {@link Embedd}
     * @param myInterpreter {@link ImageInterpreter}
     ***************************************************************************************************************/
    private void writeJpegFromRenderedImageToStream(ByteArrayOutputStream bytesoutputstream, RenderedImage ri, Embedd preferredEmbeddingType,
            ImageInterpreter myInterpreter) {
        JpegInterpreter jpint = new JpegInterpreter(ri);
        jpint.setXResolution(myInterpreter.getXResolution());
        jpint.setYResolution(myInterpreter.getYResolution());
        if (preferredEmbeddingType != null) {
            try {
                switch (preferredEmbeddingType) {
                    case LOSSLESSJPEG2000:
                        jpint.setWriterCompressionType(JpegTwoThousandInterpreter.LOSSLESS);
                        break;
                    case LOSSYJPEG2000:
                        jpint.setWriterCompressionType(JpegTwoThousandInterpreter.LOSSY);
                        jpint.setWriterCompressionValue(80);
                        break;
                    default:
                        LOGGER.error("Preferred embedding type does not match any of the supported types");
                }
            } catch (ParameterNotSupportedException e) {
                LOGGER.error("Error: ", e);
            }
        }
        jpint.writeToStream(null, bytesoutputstream);
        jpint.clear();
    }

    /***************************************************************************************************************
     * Generates {@link Image} from {@link ImageInterpreter} that we can embedd in PDF. Used by
     * {@link PDFManager#addAllPages(PdfPageSize, PdfWriter, Document, Watermark)}
     * 
     * @param myInterpreter {@link ImageInterpreter}
     * @param preferredEmbeddingType {@link Embedd}
     * 
     * @param errorPage {@link Boolean} is this an errorpage ?
     * @param watermark {@link Watermark}
     * @param errorUrl {@link URL} link to image, that caused the error
     * 
     * @return {@link Image} or null
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws BadElementException
     * 
     * @throws ImageInterpreterException - if we can't generate Watermark
     * @throws ImageManipulatorException - if we can't generate Watermark
     ***************************************************************************************************************/
    private Image generatePdfImageFromInterpreter(ImageInterpreter myInterpreter, Embedd preferredEmbeddingType, boolean errorPage,
            Watermark watermark, URL errorUrl) throws BadElementException, MalformedURLException, IOException, PDFManagerException,
            ImageInterpreterException, ImageManipulatorException {
        ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
        RenderedImage ri = null;
        switch (preferredEmbeddingType) {

            case ORIGBYTESTREAM: // if the bytestream is directly embeddable
                // should be embedded, but is it the bytestream
                // embeddable?
                if (myInterpreter.pdfBytestreamEmbeddable()) {
                    Image image = Image.getInstance(myInterpreter.getImageByteStream());
                    myInterpreter.clear();
                    return image;
                } else {
                    return null;
                }
                // ----------------------------------------------------------------------------------------------------
            case JPEG: // it is NOT directly embeddable!
                if (myInterpreter.getColordepth() <= 1) {
                    return null;
                }
                ri = myInterpreter.getRenderedImage();
                // if there is an error - write error watermark!
                if (errorPage) {
                    watermark = getNoImageErrorWatermark(errorUrl.toString());
                    ri = addwatermark(ri, watermark, ImageManager.TOP);
                    myInterpreter.setHeight(myInterpreter.getHeight() + watermark.getRenderedImage().getHeight());
                } else if (watermark != null) {
                    try {
                        ri = addwatermark(ri, watermark, 2);
                        myInterpreter.setHeight(myInterpreter.getHeight() + watermark.getRenderedImage().getHeight());
                    } catch (NullPointerException e) {
                        throw new PDFManagerException("Error while loading watermark");
                    }
                }

                try {
                    writeJpegFromRenderedImageToStream(bytesoutputstream, ri, preferredEmbeddingType, myInterpreter);
                } catch (NullPointerException e) {
                    throw new PDFManagerException("Error while rendering Image");
                }
                break;
            // ----------------------------------------------------------------------------------------------------
            case LOSSLESSJPEG2000:
                if (myInterpreter.getColordepth() > 1) {
                    ri = myInterpreter.getRenderedImage();
                    writeJpegFromRenderedImageToStream(bytesoutputstream, ri, preferredEmbeddingType, myInterpreter);
                } else {
                    return null;
                }
                break;
            case LOSSYJPEG2000:
                if (myInterpreter.getColordepth() > 1) {
                    ri = myInterpreter.getRenderedImage();
                    writeJpegFromRenderedImageToStream(bytesoutputstream, ri, preferredEmbeddingType, myInterpreter);
                } else {
                    return null;
                }
                break;
            // ----------------------------------------------------------------------------------------------------
            case TIFFG4:
                if (myInterpreter.getColordepth() > 1) {
                    // it's not a bitonal image
                    return null;
                } else {
                    TiffInterpreter tiffint = new TiffInterpreter();

                    tiffint.setXResolution(myInterpreter.getXResolution());
                    tiffint.setYResolution(myInterpreter.getYResolution());
                    try {
                        tiffint.setWriterCompressionType(TiffInterpreter.COMPRESSION_CCITTFAX4);
                    } catch (ParameterNotSupportedException e) {
                        // should never happen, as the TiffInterpreter
                        // supports this
                        // kind of compression
                        LOGGER.warn("Can't create TIFF G4 compressed image for embedding into PDF", e);
                    }

                    tiffint.writeToStream(null, bytesoutputstream);

                }
                break;
            default:
                LOGGER.error("Preferred embedding type does not match any of the supported types");
        }
        myInterpreter.clear();
        ri = null;
        byte[] returnbyteArray = bytesoutputstream.toByteArray();
        if (bytesoutputstream != null) {
            bytesoutputstream.flush();
            bytesoutputstream.close();
        }
        Image image = Image.getInstance(returnbyteArray);
        returnbyteArray = null;
        return image;
    }

    /***************************************************************************************************************
     * Generate error watermark with the text and url of missing image
     * 
     * @param errorUrl {@link String} url of the image that does not exists
     * @return {@link Watermark} with error message about image that does not exists..
     * @throws FileNotFoundException
     * @throws ImageInterpreterException
     ***************************************************************************************************************/
    private Watermark getNoImageErrorWatermark(String errorUrl) throws FileNotFoundException, ImageInterpreterException {
        String errorString = "Error: Image: " + errorUrl + " does not exists!";
        String jpgfile = new File(Util.getBaseFolderAsFile(), "errorfile.jpg").getAbsolutePath();
        FileInputStream inputFileStream = new FileInputStream(jpgfile);
        return ServletWatermark.generateErrorWatermark(inputFileStream, errorString);

    }

    private RenderedImage addwatermark(RenderedImage outImage, Watermark inWatermark, Integer watermarkposition) throws ImageManipulatorException {
        RenderedImage watermarkRi = null;
        // int orginalSize = outImage.getHeight();
        if (inWatermark != null) {
            // watermark is get as big as image
            if ((watermarkposition == ImageManager.TOP) || (watermarkposition == ImageManager.BOTTOM)) {
                inWatermark.overrideWidth(outImage.getWidth());
            } else {
                inWatermark.overrideHeight(outImage.getHeight());
            }

            watermarkRi = inWatermark.getRenderedImage();

            LOGGER.debug("Watermark size is: " + watermarkRi.getWidth() + " / " + watermarkRi.getHeight());

            // add renderedImage of Watermark to outImage
            if (watermarkposition == ImageManager.RIGHT) {
                outImage = ImageManipulator.mergeImages(outImage, watermarkRi, MergingMode.HORIZONTALLY);
            } else if (watermarkposition == ImageManager.LEFT) {
                outImage = ImageManipulator.mergeImages(watermarkRi, outImage, MergingMode.HORIZONTALLY);
            } else if (watermarkposition == ImageManager.TOP) {
                outImage = ImageManipulator.mergeImages(watermarkRi, outImage, MergingMode.VERTICALLY);
            } else if (watermarkposition == ImageManager.BOTTOM) {
                outImage = ImageManipulator.mergeImages(outImage, watermarkRi, MergingMode.VERTICALLY);
            }
        }

        // ImageManipulator.scaleCoordinates(inCoordinates, scalex, scaley)

        // free watermark memory
        watermarkRi = null;

        return outImage;
    }

    /**
     * Creates the pdf writer.
     * 
     * @param out the out
     * @param writer the writer
     * @param pdfdoc the pdfdoc
     * 
     * @return the pdf writer
     * 
     * @throws PDFManagerException the PDF manager exception
     */
    private PdfWriter createPDFWriter(OutputStream out, Document pdfdoc) throws PDFManagerException {
        PdfWriter writer = null;
        try {
            // open the pdfwriter using the outstream
            writer = PdfWriter.getInstance(pdfdoc, out);
            LOGGER.debug("PDFWriter intstantiated");

            // register Fonts
            int numoffonts = FontFactory.registerDirectories();

            LOGGER.debug(numoffonts + " fonts found and registered!");

            if ((pdfa) && (iccprofile != null)) {
                // we want to write PDFA, we have to set the PDFX conformance
                // before we open the writer
                writer.setPDFXConformance(PdfWriter.PDFA1B);
            }

            // open the pdf document to add pages and other content
            try {
                pdfdoc.open();
                LOGGER.debug("PDFDocument opened");
            } catch (Exception e) {
                throw new PDFManagerException("PdfWriter was opened, but the pdf document couldn't be opened", e);
            }

            if ((pdfa) && (iccprofile != null)) {

                // set the required PDFDictionary which
                // contains the appropriate ICC profile
                PdfDictionary pdfdict_out = new PdfDictionary(PdfName.OUTPUTINTENT);

                // set identifier for ICC profile
                pdfdict_out.put(PdfName.OUTPUTCONDITIONIDENTIFIER, new PdfString("sRGBIEC61966-2.1"));
                pdfdict_out.put(PdfName.INFO, new PdfString("sRGB IEC61966-2.1"));
                pdfdict_out.put(PdfName.S, PdfName.GTS_PDFA1);

                // PdfICCBased ib = new PdfICCBased(iccprofile);
                // writer.setOutputIntents("Custom", "PDF/A sRGB", null, "PDF/A
                // sRGB ICC Profile, sRGB_IEC61966-2-1_withBPC.icc",
                // colorProfileData);

                // read icc profile
                // ICC_Profile icc = ICC_Profile.getInstance(new
                // FileInputStream("c:\\srgb.profile"));
                PdfICCBased ib = new PdfICCBased(iccprofile);
                ib.remove(PdfName.ALTERNATE);

                PdfIndirectObject pio = writer.addToBody(ib);
                pdfdict_out.put(PdfName.DESTOUTPUTPROFILE, pio.getIndirectReference());
                writer.getExtraCatalog().put(PdfName.OUTPUTINTENTS, new PdfArray(pdfdict_out));

                // create MarkInfo elements
                // not sure this is necessary; maybe just needed for tagged PDFs
                // (PDF/A 1a)
                PdfDictionary markInfo = new PdfDictionary(PdfName.MARKINFO);
                markInfo.put(PdfName.MARKED, new PdfBoolean("false"));
                writer.getExtraCatalog().put(PdfName.MARKINFO, markInfo);

                // write XMP
                this.writeXMPMetadata(writer);
            }
        } catch (Exception e) {
            LOGGER.error("Can't open the PdfWriter object\n" + e.toString() + "\n" + e.getMessage());
            throw new PDFManagerException("Can't open the PdfWriter object", e);
        }
        return writer;
    }

    /**
     * Sets the default size of the page and creates the pdf document (com.lowagie.text.Document) instance.
     * 
     * @param pagesizemode the pagesizemode
     * @param pagesize the pagesize
     * 
     * @return the pdf-document instance
     * 
     * @throws ImageInterpreterException the image interpreter exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Document setPDFPageSizeForFirstPage(PdfPageSize pagesizemode, Rectangle pagesize) throws ImageInterpreterException, IOException {

        Document pdfdoc;
        boolean isTitlePage = false;

        // set page size of the PDF
        if ((pagesizemode == PdfPageSize.ORIGINAL) && (pdftitlepage == null)) {
            LOGGER.debug("Page size of the first page is size of first image");

            // GDZ: Check if this changes the order of the Pages
            // What if 0000002 ist intentionaly before 00000001 ?

            // page size is set to size of first page of the document
            // (first image of imageURLs)
            Map<Integer, UrlImage> sortedMap = new TreeMap<Integer, UrlImage>(imageURLs);
            for (Integer key : sortedMap.keySet()) {

                // do the image exists ?
                while (key < sortedMap.size() && sortedMap.get(key).getURL().openConnection().getContentLength() == 0) {
                    key++;
                }

                if ((pdftitlepages != null) && (pdftitlepages.get(key) != null)) {
                    // title page for Document Part available; set pagesize to
                    // A4
                    pagesize = setA4pagesize();
                    isTitlePage = true;
                    break;
                }

                // no title page, so get the size of the first page
                UrlImage pdfpage = imageURLs.get(key);

                if (pdfpage.getURL() != null) {
                    // it's an image file
                    URL url = pdfpage.getURL();
                    LOGGER.debug("Using image" + pdfpage.getURL().toString());
                    ImageInterpreter myInterpreter =
                            ImageFileFormat.getInterpreter(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);

                    float xres = myInterpreter.getXResolution();
                    float yres = myInterpreter.getYResolution();

                    int height = myInterpreter.getHeight();
                    int width = myInterpreter.getWidth();

                    int image_w_points = (width * 72) / ((int) xres);
                    int image_h_points = (height * 72) / ((int) yres);

                    pagesize = new Rectangle(image_w_points, image_h_points); // set
                    // a retangle in the size of the image
                    break; // get out of loop
                } else if (pdfpage.getClass() == PDFPage.class && ((PDFPage) pdfpage).getPdfreader() != null) {
                    // a pdf page, not an image file

                    PdfReader pdfreader = ((PDFPage) pdfpage).getPdfreader();
                    pagesize = pdfreader.getPageSize(pdfpage.getPageNumber());

                }
            }

        } else if (pdftitlepage != null) {
            isTitlePage = true;
            LOGGER.debug("Page size of the first page is A4, cause it is a title page");
            pagesize = setA4pagesize();
        } else {
            // page size is set to A4, because either the whole
            // PDF is in A4 or we will have a title page which is
            // in A4
            LOGGER.debug("Page size of the first page is A4, page size mode is " + pagesizemode);
            pagesize = setA4pagesize();
        }

        if (pagesize != null) { // pagesize is a rectangle; pagesize sets the
            // page for the first page
            pdfdoc = new Document(pagesize, 2.5f * 72f / 2.54f, 2.5f * 72f / 2.54f, 2.5f * 72f / 2.54f, 3f * 72f / 2.54f);
            if (isTitlePage) {
                pdfdoc.setMargins(pdftitlepage.getLeftMargin(), pdftitlepage.getRightMargin(), pdftitlepage.getTopMargin(), pdftitlepage
                        .getBottomMargin());
            }
        } else {
            LOGGER.warn("No pagesize available.... strange!");
            pdfdoc = new Document();
        }
        return pdfdoc;
    }

    /**
     * ************************************************************************* Sets all the bookmarks which have the same page name for this page.
     * Te hierachical relationships between bookmarks are recognized
     * 
     * @param writer the writer
     * @param pdfdestination The PDF destination of the page
     * @param pagenumber the name of the page ****************************************** ******************************
     */
    private void setBookmarksForPage(PdfWriter writer, PdfDestination pdfdestination, Integer pagenumber) {
        PdfContentByte cb = writer.getDirectContent();
        // PdfOutline rootoutline = cb.getRootOutline();

        // iterate through the tree and find all the bookmarks for this page
        // bookmarks for this page will have the same pagenumber

        if ((structureList == null) || (structureList.isEmpty())) {
            return; // no bookmarks available
        }

        // iterate over all parent bookmarks
        for (PDFBookmark bm : structureList) {
            if (bm.getImageNumber().intValue() == pagenumber.intValue()) {
                // add bookmark
                // rootoutline = cb.getRootOutline(); // get root outline
                PdfOutline outline = new PdfOutline(cb.getRootOutline(), pdfdestination, bm.getContent()); // create
                // a
                // new
                // outline as child
                // of rootoutline
                bm.setPdfOutline(outline);
            }

            checkChildrenBookmarks(bm, pdfdestination, pagenumber); // check for
            // bookmarks
            // children

        }
    }

    /**
     * ************************************************************************* checks all children of a bookmark and see if any of them fits to the
     * appropriate page name/ page number.
     * 
     * @param parent the parent
     * @param pdfdestination the pdfdestination
     * @param pagenumber ************************************************************** **********
     */
    private void checkChildrenBookmarks(PDFBookmark parent, PdfDestination pdfdestination, Integer pagenumber) {
        for (PDFBookmark child : parent.getChildren()) {
            if (child == null) {
                // should not happen, but may happen, if we have a logical
                // <div> which
                // does not link to a phys div
                continue; // get next in loop
            }
            if (child.getImageNumber().intValue() == pagenumber.intValue()) {
                // must set a bookmark for this page
                PDFBookmark childsparent = findParentBookmark(child);
                if (childsparent != null) {
                    // parent was found, so add this bookmark to the PDF
                    PdfOutline parentOutline = childsparent.getPdfOutline();
                    if (parentOutline == null) {
                        // parent doesn't have an outline probably because
                        // it started on a later
                        // page - anyhow write something to logfile
                        LOGGER.error("Parent Bookmark \"" + childsparent.getContent() + "\"has no PdfOutline.");
                    } else {
                        PdfOutline outline = new PdfOutline(parentOutline, pdfdestination, child.getContent()); // create
                        // a new outline as child of rootoutline
                        child.setPdfOutline(outline);
                        LOGGER.debug("Bookmark \"" + childsparent.getContent() + "\"set successfully");
                    }
                }
            }

            // check children of this child
            checkChildrenBookmarks(child, pdfdestination, pagenumber);
        }

    }

    /**
     * ************************************************************************* find parent {@link PDFBookmark} from given {@link PDFBookmark}.
     * 
     * @param inBookmark given {@link PDFBookmark}
     * 
     * @return parent {@link PDFBookmark} ***************************************************************** *******
     */
    private PDFBookmark findParentBookmark(PDFBookmark inBookmark) {
        for (PDFBookmark rootBookmark : structureList) {
            if (rootBookmark.equals(inBookmark)) {
                // bookmark is a root bookmark and therefore has no parent
                return null;
            }

            // search for bookmark just under the current myBookmark
            PDFBookmark foundBookmark = findParentInBranch(inBookmark, rootBookmark);
            if (foundBookmark != null) {
                return foundBookmark;
            }
        }
        return null; // no parent found
    }

    /**
     * ************************************************************************* find parent {@link PDFBookmark} in branch from given
     * {@link PDFBookmark}.
     * 
     * @param inBookmark given {@link PDFBookmark}
     * @param topBookmark given {@link PDFBookmark}
     * 
     * @return parent {@link PDFBookmark} ***************************************************************** *******
     */
    private PDFBookmark findParentInBranch(PDFBookmark inBookmark, PDFBookmark topBookmark) {
        for (PDFBookmark checkBM : topBookmark.getChildren()) {
            if (checkBM.equals(inBookmark)) {
                // inBookmark is a child, s return topBookmark as a parent
                return topBookmark;
            }

            // check if the inBookmark is child of any children
            PDFBookmark foundBookmark = findParentInBranch(inBookmark, checkBM);
            if (foundBookmark != null) {
                return foundBookmark;
            }
        }
        return null;
    }

    /**
     * Write xmp metadata.
     * 
     * @param inWriter the in writer
     */
    private void writeXMPMetadata(PdfWriter inWriter) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            XmpWriter xmp = new XmpWriter(os);
            XmpSchema dc = new DublinCoreSchema();

            // set DublinCore metadata
            if (this.subject != null) {
                dc.setProperty(DublinCoreSchema.SUBJECT, this.subject);
            }
            if (this.title != null) {
                dc.setProperty(DublinCoreSchema.TITLE, this.title);
            }
            if (this.author != null) {
                dc.setProperty(DublinCoreSchema.CREATOR, this.author);
            }

            // add the DublinCore Simple Metadata to the RDF container

            xmp.addRdfDescription(dc);

            PdfSchema pdf = new PdfSchema();
            // set keywords
            pdf.setProperty(PdfSchema.KEYWORDS, "Hello World, XMP, Metadata");

            // set the version; must be 1.4 for PDF/A
            pdf.setProperty(PdfSchema.VERSION, "1.4");
            xmp.addRdfDescription(pdf);

            xmp.close();
        } catch (IOException e) {
            LOGGER.error("error occured while writing xmp metadata", e);
        }
        inWriter.setXmpMetadata(os.toByteArray());
    }

    /**
     * ************************************************************************* create a {@link Rectangle} for DIN A4 format.
     * 
     * @return {@link Rectangle} with A4 size *********************************** *************************************
     */
    private Rectangle setA4pagesize() {
        int page_w = 210; // dimensions of the page; A4 in mm
        int page_h = 297;

        int page_w_points = (int) ((page_w * 72) / 25.4);
        int page_h_points = (int) ((page_h * 72) / 25.4);
        // front page, it's always A4
        Rectangle pageSize = new Rectangle(page_w_points, page_h_points);
        return pageSize;
    }

    /**
     * ************************************************************************* Getter for creator.
     * 
     * @return the creator ****************************************************** ******************
     */
    public String getCreator() {
        return creator;
    }

    /**
     * ************************************************************************* Setter for creator.
     * 
     * @param creator the creator to set ******************************************** ****************************
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * ************************************************************************* Getter for author.
     * 
     * @return the author ******************************************************* *****************
     */
    public String getAuthor() {
        return author;
    }

    /**
     * ************************************************************************* Setter for author.
     * 
     * @param author the author to set ********************************************* ***************************
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * ************************************************************************* Getter for title.
     * 
     * @return the title ******************************************************** ****************
     */
    public String getTitle() {
        return title;
    }

    /**
     * ************************************************************************* Setter for title.
     * 
     * @param title the title to set ********************************************** **************************
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * ************************************************************************* Getter for subject.
     * 
     * @return the subject ****************************************************** ******************
     */
    public String getSubject() {
        return subject;
    }

    /**
     * ************************************************************************* Setter for subject.
     * 
     * @param subject the subject to set ******************************************** ****************************
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * ************************************************************************* Getter for keyword.
     * 
     * @return the keyword ****************************************************** ******************
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * ************************************************************************* Setter for keyword.
     * 
     * @param keyword the keyword to set ******************************************** ****************************
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * ************************************************************************* Getter for imageNames.
     * 
     * @return the imageNames *************************************************** *********************
     */
    public Map<Integer, String> getImageNames() {
        return imageNames;
    }

    /**
     * ************************************************************************* Sets Image names. The HashMap contains an integer number as a string
     * for identifying the page number and the name of the page. The {@link Integer} for the image has to start at 1.
     * 
     * @param imageNames the imageNames to set, first page Integer must start with 1 *** ************************************************************
     *            *********
     */
    public void setImageNames(Map<Integer, String> imageNames) {
        this.imageNames = imageNames;
    }

    /**
     * ************************************************************************* Getter for imageURLs.
     * 
     * @return the imageURLs **************************************************** ********************
     */
    public Map<Integer, UrlImage> getImageURLs() {
        return this.imageURLs;
    }

    /**
     * Sets the iccprofile.
     * 
     * @param iccprofile the iccprofile to set
     */
    public void setIccprofile(ICC_Profile iccprofile) {
        this.iccprofile = iccprofile;
    }

    /**
     * ************************************************************************* Getter for rootBookmarkList.
     * 
     * @return the rootBookmarkList ********************************************* ***************************
     */
    public List<? extends Structure> getStructureList() {
        return structureList;
    }

    /**
     * ************************************************************************* Setter for rootBookmarkList.
     * 
     * @param structureList the structure list
     */
    public void setStructureList(List<? extends Structure> structureList) {
        this.structureList = PDFBookmark.convertList(structureList);
    }

    /**
     * This is the only mandatory method which must be called before createPDF is called and the PDF is created. The HashMap must contain a String for
     * order (an integer number as a string) and the URL
     * 
     * @param imageURLs the imageURLs to set
     */
    public void setImageURLs(HashMap<Integer, UrlImage> imageURLs) {
        this.imageURLs = imageURLs;
    }

    /**
     * A PDF file may consists of several parts. These parts may have their own title page. The integer contains the pagenumber before the appropriate
     * title page is added to the PDF.
     * 
     * @return the pdftitlepages
     */
    public Map<Integer, PDFTitlePage> getPdftitlepages() {
        return pdftitlepages;
    }

    /**
     * Sets the pdftitlepages.
     * 
     * @param pdftitlepages the pdftitlepages to set
     */
    public void setPdftitlepages(Map<Integer, PDFTitlePage> pdftitlepages) {
        this.pdftitlepages = pdftitlepages;
    }

    /**
     * ************************************************************************* Getter for pdftitlepage.
     * 
     * @return the pdftitlepage ************************************************* ***********************
     */
    public PDFTitlePage getPdftitlepage() {
        return pdftitlepage;
    }

    /**
     * ************************************************************************* Setter for pdftitlepage.
     * 
     * @param pdftitlepage the pdftitlepage to set *************************************** *********************************
     */
    public void setPdftitlepage(PDFTitlePage pdftitlepage) {
        this.pdftitlepage = pdftitlepage;
    }

    // /**
    // * Checks if is always use rendered image.
    // *
    // * @return the alwaysUseRenderedImage
    // */
    // private boolean isAlwaysUseRenderedImage() {
    // return alwaysUseRenderedImage;
    // }

    /**
     * Checks if is always compress to jpeg.
     * 
     * @return the alwaysCompressToJPEG
     */
    public boolean isAlwaysCompressToJPEG() {
        return alwaysCompressToJPEG;
    }

    /**
     * Sets the always use rendered image.
     * 
     * @param alwaysUseRenderedImage the alwaysUseRenderedImage to set
     */
    // TODO: there is a bug in here, since it only works correctly if this
    // method is called before setAlwaysCompressToJPEG
    // Since RenderedImage is never used, just ignore this setting
    public void setAlwaysUseRenderedImage(boolean alwaysUseRenderedImage) {
        this.alwaysUseRenderedImage = alwaysUseRenderedImage;
    }

    /**
     * Sets the always compress to jpeg.
     * 
     * @param alwaysCompressToJPEG the alwaysCompressToJPEG to set
     */
    public void setAlwaysCompressToJPEG(boolean alwaysCompressToJPEG) {
        this.alwaysCompressToJPEG = alwaysCompressToJPEG;
        if (alwaysCompressToJPEG) {
            // set everything to jpeg
            // this.embeddBitonalImage = this.embeddGreyscaleImage = this.embeddColorImage = Embedd.JPEG;

            // DIPF hack: do not apply this setting for bitonal images
            this.embeddGreyscaleImage = this.embeddColorImage = Embedd.JPEG;
        }
    }

    //
    // http configuration, setter for proxy
    //

    /**
     * Gets the httpproxyhost.
     * 
     * @return the httpproxyhost
     */
    public String getHttpproxyhost() {
        return httpproxyhost;
    }

    /**
     * Sets the httpproxyhost.
     * 
     * @param httpproxyhost the httpproxyhost to set
     */
    public void setHttpproxyhost(String httpproxyhost) {
        this.httpproxyhost = httpproxyhost;
    }

    /**
     * Gets the httpproxyport.
     * 
     * @return the httpproxyport
     */
    public String getHttpproxyport() {
        return httpproxyport;
    }

    /**
     * Sets the httpproxyport.
     * 
     * @param httpproxyport the httpproxyport to set
     */
    public void setHttpproxyport(String httpproxyport) {
        this.httpproxyport = httpproxyport;
    }

    /**
     * Gets the httpproxyuser.
     * 
     * @return the httpproxyuser
     */
    public String getHttpproxyuser() {
        return httpproxyuser;
    }

    /**
     * Sets the httpproxyuser.
     * 
     * @param httpproxyuser the httpproxyuser to set
     */
    public void setHttpproxyuser(String httpproxyuser) {
        this.httpproxyuser = httpproxyuser;
    }

    /**
     * Gets the httpproxypassword.
     * 
     * @return the httpproxypassword
     */
    public String getHttpproxypassword() {
        return httpproxypassword;
    }

    /**
     * Sets the httpproxypassword.
     * 
     * @param httpproxypassword the httpproxypassword to set
     */
    public void setHttpproxypassword(String httpproxypassword) {
        this.httpproxypassword = httpproxypassword;
    }

    /**
     * Gets the page size from a string.
     * 
     * @param strPageSize the str page size
     * 
     * @return {@link PdfPageSize}
     */
    public static PdfPageSize getPageSizefromString(String strPageSize) {
        for (PdfPageSize pageSize : PdfPageSize.values()) {
            if (pageSize.getName().equalsIgnoreCase(strPageSize)) {
                return pageSize;
            }
        }
        return null;
    }

    public void setPdfa(boolean writeAsPdfA) {
        this.pdfa = writeAsPdfA;

    }
}
