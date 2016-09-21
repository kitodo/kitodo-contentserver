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
package de.unigoettingen.sub.commons.contentlib.pdflib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;

import de.unigoettingen.sub.commons.contentlib.exceptions.PDFManagerException;

/************************************************************************************
 * PDFTitlePage for customizing the front page of a pdf file
 * 
 * @version 06.01.2009 
 * @author Markus Enders
 ************************************************************************************/
public class PDFTitlePage implements Cloneable {

    private static final Logger LOGGER = Logger.getLogger(PDFTitlePage.class);

    // these string contain the contents of the 4 lines
    List<PDFTitlePageLine> allTitleLines = new LinkedList<PDFTitlePageLine>();
    List<PDFTitlePageParagraph> allParagraphs = new LinkedList<PDFTitlePageParagraph>();
    List<PDFTitlePageImage> allImages = new LinkedList<PDFTitlePageImage>();
    String ttffontpath = null; // filepath to truetype font(s)
    String termsandconditionstitle = null;
    String structuretype = null;
    private int left = 36;
    private int right = 72;
    private int top = 108;
    private int bottom = 180;

    /************************************************************************************
     * public empty constructor
     ************************************************************************************/
    public PDFTitlePage() {
        // May be used for sublassing
    }

    /************************************************************************************
     * read xml for TitlePage via http
     * 
     * @param url Url as String
     * @throws IOException , PDFManagerException
     ************************************************************************************/
    private String readXMLviaHTTP(URI url) throws IOException, PDFManagerException {

        String response = "";
        URL myURL = url.toURL();
        URLConnection urlConn = myURL.openConnection(); // open connection

        if (urlConn instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) urlConn; // cast
            // the
            // class,
            int code = httpConnection.getResponseCode();

            if (code != 200) {
                PDFManagerException pdfe = new PDFManagerException("Can't read configuration file for PDF Title Page; http return code != 200");
                throw pdfe;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), "UTF8"));

            String input;
            StringBuffer response_buf = new StringBuffer();
            while ((input = in.readLine()) != null) {
                for (int i = 0; i < input.length(); i++) {
                    if ((input.length() > (i + 4)) && (input.substring(i, i + 4)).equals("&lt;")) {
                        // nothing should happen
                    } else if ((input.length() > (i + 5)) && (input.substring(i, i + 5)).equals("&amp;")) {
                        // nothing should happen
                    } else if ((input.length() > (i + 4)) && (input.substring(i, i + 5)).equals("&gt;")) {
                        // nothing should happen
                    }
                    if ((input.length() > (i + 1)) && (input.substring(i, i + 1)).equals("&")) {
                        // convert the ampersand to "&amp;
                        String b = input.substring(0, i - 1);
                        String c = input.substring(i + 1, input.length());
                        input = b + "&amp;" + c;
                    }
                }
                response_buf.append(input);
            }
            response = response_buf.toString();
        }
        return response;
    }

    /************************************************************************************
     * read configuration for title page from xml of url
     * 
     * @param inUrl Url of xml file for configuration
     * @throws PDFManagerException
     ************************************************************************************/
    public void readConfiguration(URI inUrl) throws PDFManagerException {

        // first we read the configuration file via http
        // then we parse this file and correct the XML.
        // This is necessary thanks to bloody GDZ data, which contains invalid
        // XML (the ampersand is not always stored as an appropriate entity)
        //
        Document xmldoc = null;
        try {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setValidating(false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            docBuilder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    if (systemId.contains("pdftitlepage.dtd")) {
                        return new InputSource(new StringReader(""));
                    } else {
                        return null;
                    }
                }
            });

            if (inUrl.getScheme().equals("http")) {
                // read via HTTP
                String response = readXMLviaHTTP(inUrl);
                xmldoc = docBuilder.parse(new InputSource(new StringReader(response)));
            } else if (inUrl.getScheme().equals("file")) {
                // read from file system
                File file = new File(inUrl);
                if (!file.exists() || !file.canRead()) {
                    throw new PDFManagerException("File for pdf title page configuration can not be read: " + inUrl.toString());
                }
                xmldoc = docBuilder.parse(file);
            } else {
                // unknown, throw exception
                throw new PDFManagerException("Invalid URI for reading PDF's title page configuration");
            }

            // iterate through thee DOM tree

            Node topmostelement = xmldoc.getDocumentElement(); // get uppermost
            // element

            if (!topmostelement.getNodeName().equals("pdftitlepage")) {
                LOGGER.error("Don't get correct xml response via HTTP system");
                PDFManagerException pdfe =
                        new PDFManagerException("Response received for PDF's title page configuration file doesn't start with <pdftitlepage>.");
                throw pdfe;
            }

            NodeList children = topmostelement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node singlenode = children.item(i);

                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().startsWith("pagemargins"))) {
                    NamedNodeMap map = singlenode.getAttributes();
                    if (map != null) {
                        Node marginLeft = map.getNamedItem("marginLeft");
                        if (marginLeft != null) {
                            String s = marginLeft.getNodeValue();
                            if (s.matches("\\d*")) {
                                left = Integer.getInteger(s);
                            }
                        }

                        Node marginRight = map.getNamedItem("marginRight");
                        if (marginRight != null) {
                            String s = marginRight.getNodeValue();
                            if (s.matches("\\d*")) {
                                right = Integer.getInteger(s);
                            }
                        }

                        Node marginTop = map.getNamedItem("marginTop");
                        if (marginTop != null) {
                            String s = marginTop.getNodeValue();
                            if (s.matches("\\d*")) {
                                top = Integer.getInteger(s);
                            }
                        }

                        Node marginBottom = map.getNamedItem("marginBottom");
                        if (marginBottom != null) {
                            String s = marginBottom.getNodeValue();
                            if (s.matches("\\d*")) {
                                bottom = Integer.getInteger(s);
                            }
                        }
                    }

                }

                // read <parenttype> element
                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().startsWith("parenttype"))) {
                    structuretype = getValueOfElement(singlenode);
                }

                // read all the title lines; there is an unlimited number
                // the just need to begin with <line....>
                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().startsWith("line"))) {
                    // new line detected
                    PDFTitlePageLine pdftpl = new PDFTitlePageLine();

                    // parse the <line> element
                    String content = getValueOfElement(singlenode);
                    pdftpl.setContent(content);

                    NamedNodeMap nnm = singlenode.getAttributes();
                    if (nnm != null) {
                        Node maxlinelengthnode = nnm.getNamedItem("maxlinelength");
                        if (maxlinelengthnode != null) {
                            String linewrap_str = maxlinelengthnode.getNodeValue();
                            // string needs to be converted to integer
                            try {
                                int linewrap = Integer.parseInt(linewrap_str);
                                pdftpl.setLinewrap(linewrap);
                            } catch (Exception e) {
                                LOGGER.warn("maxlinelength attribute in PDF's title page configuration file has not an integer value");
                                pdftpl.setLinewrap(50);
                            }
                        }
                        Node maxlinetotalhnode = nnm.getNamedItem("maxtotallength");
                        if (maxlinetotalhnode != null) {
                            String linetotallength_str = maxlinetotalhnode.getNodeValue();
                            try {
                                int linetotallength = Integer.parseInt(linetotallength_str);
                                pdftpl.setShortentextlength(linetotallength);
                            } catch (Exception e) {
                                LOGGER.warn("maxtotallength attribute in PDF's title page configuration file has not an integer value");
                                pdftpl.setShortentextlength(100);
                            }
                        }
                        Node fontsizenode = nnm.getNamedItem("size");
                        if (fontsizenode != null) {
                            String fontsize_str = fontsizenode.getNodeValue();
                            try {
                                int fontsize = Integer.parseInt(fontsize_str);
                                pdftpl.setFontsize(fontsize);
                            } catch (Exception e) {
                                LOGGER.warn("size attribute in PDF's title page configuration file has not an integer value");
                                pdftpl.setFontsize(12);
                            }
                        }
                        Node fonttypenode = nnm.getNamedItem("fonttype");
                        if (fonttypenode != null) {
                            String fonttype = fonttypenode.getNodeValue();
                            pdftpl.setFonttype(fonttype);
                        }
                    }

                    // adding the line to the LinkedList
                    if (pdftpl.getContent() != null) {
                        ((LinkedList<PDFTitlePageLine>) allTitleLines).addLast(pdftpl);
                    } else {
                        LOGGER.warn("Line configuration in PDF's title configuration has NO content");
                    }

                } // end of iteration over DOM elements

                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("termsconditions"))) {
                    // get terms and conditions title
                    NamedNodeMap nnm = singlenode.getAttributes();
                    if (nnm != null) {
                        Node tactitle = nnm.getNamedItem("title"); // get title
                        // attribute
                        // in
                        // termsandconditions
                        // element
                        if (tactitle != null) {
                            termsandconditionstitle = tactitle.getNodeValue();
                        }
                    }

                    // get all paragraphs within terms and conditions
                    //
                    this.getParagraphs(singlenode);
                }

                // get all images
                //
                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("image"))) {
                    this.readPDFTitlePageImageConfig(singlenode);
                }
            }
            // catch all the exception which may occur during XML parsing
        } catch (MalformedURLException mue) {
            LOGGER.error("This seems to be a funny URL - it is invalid:" + mue.toString());
            throw new PDFManagerException("Invalid URL for PDF's title page configuration", mue);
        } catch (IOException ioe) {
            LOGGER.error("ERROR: IOException occured while accessing PDF's title page configuration file", ioe);
            throw new PDFManagerException("IOException occured while accessing PDF's title page configuration file", ioe);
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            LOGGER.error("ERROR: couldn't parse XML file ", pce);
            throw new PDFManagerException("couldn't parse PDF's title page configuration XML file ", pce);
        } catch (org.xml.sax.SAXException saxe) {
            LOGGER.error("ERROR: SAX exception ", saxe);
            throw new PDFManagerException(" SAX exception while parsing PDF's title page configuration file ", saxe);
        }

    }

    /************************************************************************************
     * Retrieves the value of the textnode which must be child of an Element node in the DOM tree
     * 
     * @return Value of given Element as String
     ************************************************************************************/
    private String getValueOfElement(Node inNode) {
        NodeList childnodes = inNode.getChildNodes();

        for (int i = 0; i < childnodes.getLength(); i++) {
            Node singlenode = childnodes.item(i);
            if (singlenode.getNodeType() == Node.TEXT_NODE) {
                String value = singlenode.getNodeValue();
                return value;
            }
        }
        return null;
    }

    /************************************************************************************
     * Retrieves the configuration for all paragraphs. For each paragraph a PDFTitlePageParagraph object is intantiated and added to the allParagraphs
     * LinkedList.
     * 
     * @param inNode
     ************************************************************************************/
    private void getParagraphs(Node inNode) {

        NodeList childnodes = inNode.getChildNodes();
        for (int i = 0; i < childnodes.getLength(); i++) {
            Node singlenode = childnodes.item(i);
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("p"))) {
                String value = getValueOfElement(singlenode);
                // paragraph found, create new PDFTitlePageParagraph instance

                PDFTitlePageParagraph p = new PDFTitlePageParagraph();
                p.setContent(value);
                NamedNodeMap nnm = singlenode.getAttributes();
                if (nnm != null) {
                    Node node_style = nnm.getNamedItem("style");
                    Node node_size = nnm.getNamedItem("size");

                    if (node_style != null) {
                        String style = node_style.getNodeValue();
                        p.setFonttype(style);
                    }
                    if (node_size != null) {
                        String size_str = node_size.getNodeValue();
                        try {
                            int size = Integer.parseInt(size_str);
                            p.setFontsize(size);
                        } catch (Exception e) {
                            LOGGER.warn("DocumentConverter: paragraph doesn't contain integer for font-size");
                            p.setFontsize(12);
                        }
                    }
                }
                allParagraphs.add(p);
            }
        }
    }

    /************************************************************************************
     * retrieves Configuration for a single image on PDFTitlePage
     * 
     * @param inNode the domnode to parse
     ************************************************************************************/
    private void readPDFTitlePageImageConfig(Node inNode) {
        String filename = null;
        float xpos = 0;
        float ypos = 0;
        int scaling = 0;
        String x_str = null;
        String y_str = null;
        String scaling_str = null;

        filename = getValueOfElement(inNode);
        NamedNodeMap nnm = inNode.getAttributes();
        if (nnm != null) {
            Node node_x = nnm.getNamedItem("x");
            Node node_y = nnm.getNamedItem("y");
            Node node_scale = nnm.getNamedItem("scale");
            if ((node_x != null) && (node_y != null)) {
                x_str = node_x.getNodeValue();
                y_str = node_y.getNodeValue();
                scaling_str = node_scale.getNodeValue();
            } else {
                LOGGER.error("No coordinates given for PDFTitlePageImage");
                return;
            }
            try {
                ypos = Float.parseFloat(y_str);
                xpos = Float.parseFloat(x_str);
                if (scaling_str != null) {
                    scaling = Integer.parseInt(scaling_str);
                }
            } catch (Exception e) {
                LOGGER.error("DocumentConverter.getImageItem: value for coordinates are not in float format");
                return;
            }
        }

        // all necessary values are available
        // create the instance of PDFTitlePageImage
        // and add it
        if ((filename != null) && (xpos != 0) && (ypos != 0)) {
            PDFTitlePageImage pdftpi = new PDFTitlePageImage();
            pdftpi.setFilename(filename);
            pdftpi.setXCoordinate(xpos);
            pdftpi.setYCoordinate(ypos);

            if (scaling != 0) {
                pdftpi.setScalefactor(scaling);
            }
            allImages.add(pdftpi);
        } else {
            LOGGER.error("DocumentConverter.getImageItem: no filename given");
        }
    }

    /************************************************************************************
     * renders the title page according to its setting
     * 
     * @param pdfDoc the given pdf document to render
     * @throws PDFManagerException
     ************************************************************************************/
    public void render(com.lowagie.text.Document pdfDoc) throws PDFManagerException {

        // set margins
        pdfDoc.setMargins(left, right, top, bottom);

        try {
            // set fonts
            Font smallFont = getPDFFont(8);
            // Font myFont = getPDFFont("Helvetica", 12);

            // write <parenttype> element
            if (this.structuretype != null) {
                PDFTitlePageLine structureLine = new PDFTitlePageLine();
                structureLine.setContent(this.structuretype);
                renderTextLine(structureLine, smallFont, pdfDoc);
            }

            // iterate over all lines
            Iterator<PDFTitlePageLine> it = allTitleLines.iterator();
            while (it.hasNext()) {
                PDFTitlePageLine pdftpl = it.next();

                Font tplFont = getPDFFont(pdftpl.getFontsize());
                renderTextLine(pdftpl, tplFont, pdfDoc);
            }

            // render terms and conditions title
            if (this.getTermsandconditionstitle() != null) {
                PDFTitlePageParagraph tacparagraph = new PDFTitlePageParagraph();
                tacparagraph.setContent(this.getTermsandconditionstitle());
                tacparagraph.setFontsize(14);
                tacparagraph.setFonttype("underline");
                renderParagraph(tacparagraph, pdfDoc);
            }

            // iterate over all paragraphs
            Iterator<PDFTitlePageParagraph> it_paragraph = allParagraphs.iterator();
            while (it_paragraph.hasNext()) {
                PDFTitlePageParagraph pdftpp = it_paragraph.next();
                renderParagraph(pdftpp, pdfDoc);
            }

            // iterate over all images
            Iterator<PDFTitlePageImage> it_image = allImages.iterator();
            while (it_image.hasNext()) {
                PDFTitlePageImage pdftpi = it_image.next();
                renderImage(pdftpi, pdfDoc);
            }

            // other, additional PDF settings for the page
            pdfDoc.setMargins(0, 0, 0, 0); // delete all margins
        } catch (DocumentException de) {
            // something went wrong while adding data to the PDF
            LOGGER.error("ERROR: Something serious went wrong while adding data to the PDF title page", de);
            throw new PDFManagerException("ERROR: Something serious went wrong while adding data to the PDF title page", de);
        }
    }

    /************************************************************************************
     * get font as {@link Font} from given font name and size
     * 
     * @param fontname name of font
     * @param fontsize size of font
     * 
     * @throws PDFManagerException
     ************************************************************************************/
    private Font getPDFFont(int fontsize) throws PDFManagerException {
        Font resultfont = null;

        // set the base font
        try {
            if (this.ttffontpath == null) {
                // don't use TTF

                LOGGER.debug("Do not use TrueType Font... instead standard Arial is used");
                resultfont = FontFactory.getFont("Arial", BaseFont.CP1252, BaseFont.EMBEDDED, fontsize);

                // String[] codePages = basefont.getCodePagesSupported();
                // System.out.println("All available encodings for font:\n\n");
                // for (int i = 0; i < codePages.length; i++) {
                // System.out.println(codePages[i]);
                // }
            } else {
                // load font, embedd it - use unicode
                LOGGER.debug("Use TrueType Font... at:" + this.ttffontpath);

                BaseFont bf = BaseFont.createFont(this.ttffontpath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                resultfont = new Font(bf, fontsize);
            }

        } catch (Exception e) {
            LOGGER.error("DocumentException while creating title page for PDF", e);
            throw new PDFManagerException("Exception while creating Titlepage for PDF", e);
        }

        return resultfont;
    }

    private void renderTextLine(PDFTitlePageLine inTextLine, Font pdffont, com.lowagie.text.Document pdfdoc) throws DocumentException {
        String content = inTextLine.getContent();

        if (content == null) {
            return;
        }

        if ((content != null) && (content.length() > inTextLine.getShortentextlength())) {
            content = content.substring(0, inTextLine.getShortentextlength()) + "...";
        }

        // show first line
        Paragraph p1 = new Paragraph();

        // check, if content needs to be wrapped
        if (content.length() > inTextLine.getLinewrap()) {
            String allwords[] = content.split(" ");
            int numberofwords = allwords.length;
            int charsinline = 0; // counter for characters per line
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < numberofwords; i++) {
                if ((charsinline + (allwords[i].length()) + 1) < inTextLine.getLinewrap()) {
                    // word does fit in line, so add it to paragraph
                    // ch1 = ch1 + allwords[i] + " ";
                    charsinline = charsinline + (allwords[i].length()) + 1;
                } else { // word does not fit in line;
                    p1 = new Paragraph(stringBuilder.toString(), pdffont); // create a new
                    // paragraph
                    pdfdoc.add(p1); // finally add the current paragraph to the
                    // document
                    stringBuilder.setLength(0);
                    charsinline = (allwords[i].length()); // counter for
                    // characters
                }
                stringBuilder.append(allwords[i]);
                stringBuilder.append(' ');
            }

            p1 = new Paragraph(stringBuilder.toString(), pdffont); // create a new paragraph
            pdfdoc.add(p1); // finally add the current paragraph to the document
        } else {
            // content doesn't need to be wrapped
            p1 = new Paragraph(new Chunk(content, pdffont));
            pdfdoc.add(p1);
        }

    }

    /************************************************************************************
     * render paragraph into title page
     * 
     * @param pdftpp given {@link PDFTitlePageParagraph} to render
     * @param pdfdoc given {@link com.lowagie.text.Document} where to render
     * @throws DocumentException
     ************************************************************************************/
    private void renderParagraph(PDFTitlePageParagraph pdftpp, com.lowagie.text.Document pdfdoc) throws DocumentException {
        String text = pdftpp.getContent();
        if (text == null) {
            text = "";
        }
        int fontstyle = Font.NORMAL;
        if (pdftpp.getFonttype().equals("bold")) {
            fontstyle = Font.BOLD;
        }
        if (pdftpp.getFonttype().equals("italic")) {
            fontstyle = Font.ITALIC;
        }
        if (pdftpp.getFonttype().equals("bolditalic")) {
            fontstyle = Font.BOLDITALIC;
        }
        if (pdftpp.getFonttype().equals("underline")) {
            fontstyle = Font.UNDERLINE;
        }
        if (pdftpp.getFonttype().equals("strikethru")) {
            fontstyle = Font.STRIKETHRU;
        }
        // create BaseFont for embedding
        try {
            Font font = FontFactory.getFont("Arial", BaseFont.CP1252, BaseFont.EMBEDDED, pdftpp.getFontsize(), fontstyle);
            Paragraph p2 = new Paragraph(new Chunk(text, font));
            // Paragraph p2=new Paragraph(text,
            // FontFactory.getFont(FontFactory.TIMES_ROMAN, 12));
            pdfdoc.add(p2);
        } catch (Exception e) {
            LOGGER.error("error occured while generating paragraph for titlepage", e);
        }
    }

    /************************************************************************************
     * render image for titlepage
     * 
     * @param pdftpi the {@link PDFTitlePageImage} which shoud be renderd
     * @param pdfdoc the {@link com.lowagie.text.Document} where the titlepage shoud be rendered
     ************************************************************************************/
    private void renderImage(PDFTitlePageImage pdftpi, com.lowagie.text.Document pdfdoc) throws DocumentException {
        try {
            Image img = Image.getInstance(pdftpi.getFilename());
            // calculate the absolute position
            float absposx = pdftpi.getXCoordinate() * 72f / 2.54f;
            float absposy = pdftpi.getYCoordinate() * 72f / 2.54f;
            img.setAbsolutePosition(absposx, absposy);
            if (pdftpi.getScalefactor() != 0) {
                img.scalePercent(pdftpi.getScalefactor()); // scale image (in
                // percent)
            }
            pdfdoc.add(img);
        } catch (MalformedURLException mue) {
            LOGGER.error("WARNING: Can't read image " + pdftpi.getFilename() + " for PDF title page, invalid URL");
        } catch (IOException ioe) {
            LOGGER.error("WARNING: Can't read image:" + pdftpi.getFilename() + " for PDF title page - IO Exception");
        }
    }

    /************************************************************************************
     * add a {@link PDFTitlePageLine} to this {@link PDFTitlePage}
     * 
     * @param pdftpl the given {@link PDFTitlePageLine} to add
     ************************************************************************************/
    public void addPDFTitlePageLine(PDFTitlePageLine pdftpl) {
        allTitleLines.add(pdftpl);
    }

    /************************************************************************************
     * add a {@link PDFTitlePageParagraph} to this {@link PDFTitlePage}
     * 
     * @param pdftpp the given {@link PDFTitlePageParagraph} to add
     ************************************************************************************/
    public void addPDFTitlePageParagraph(PDFTitlePageParagraph pdftpp) {
        allParagraphs.add(pdftpp);
    }

    /************************************************************************************
     * add a {@link PDFTitlePageImage} to this {@link PDFTitlePage}
     * 
     * @param pdftpi the given {@link PDFTitlePageImage} to add
     ************************************************************************************/
    public void addPDFTitlePageImage(PDFTitlePageImage pdftpi) {
        allImages.add(pdftpi);
    }

    /**
     * @return the termsandconditionstitle
     */
    protected String getTermsandconditionstitle() {
        return termsandconditionstitle;
    }

    /**
     * @param termsandconditionstitle the termsandconditionstitle to set
     */
    public void setTermsandconditionstitle(String termsandconditionstitle) {
        this.termsandconditionstitle = termsandconditionstitle;
    }

    /**
     * @return the divtype
     */
    public String getStructuretype() {
        return structuretype;
    }

    /**
     * @param divtype the divtype to set
     */
    public void setStructuretype(String structuretype) {
        this.structuretype = structuretype;
    }

    /**
     * clones the current PDFTitlePage
     * 
     * @return
     */
    @Override
    public PDFTitlePage clone() throws CloneNotSupportedException {

        PDFTitlePage newtitlepage = new PDFTitlePage();
        newtitlepage.allImages = this.allImages;
        newtitlepage.allParagraphs = this.allParagraphs;
        newtitlepage.allTitleLines = this.allTitleLines;
        newtitlepage.termsandconditionstitle = this.termsandconditionstitle;
        newtitlepage.ttffontpath = this.ttffontpath;
        newtitlepage.structuretype = this.structuretype;

        return newtitlepage;
    }

    public void deleteTitleLines() {
        allTitleLines = new LinkedList<PDFTitlePageLine>();
    }

    public int getLeftMargin() {
        return left;
    }

    public int getRightMargin() {
        return right;
    }

    public int getTopMargin() {
        return top;
    }

    public int getBottomMargin() {
        return bottom;
    }
}
