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

import java.awt.color.ICC_Profile;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lowagie.text.pdf.PdfPageLabels;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.SimpleBookmark;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.PDFManagerException;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.pdflib.DocumentPart.DocumentPartType;
import de.unigoettingen.sub.commons.simplemets.METSParser;
import de.unigoettingen.sub.commons.simplemets.MetadataExtractor;
import de.unigoettingen.sub.commons.simplemets.StructureMetadataExtractor;
import de.unigoettingen.sub.commons.simplemets.exceptions.MetsException;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;

public class PDFCreator {

    private static final Logger LOGGER = Logger.getLogger(PDFCreator.class);

    public void createPDF(OutputStream out, List<DocumentPart> metsparts, PDFConfiguration pdfconfig, MetadataExtractor inMetadataExtractor,
            StructureMetadataExtractor inBookmarkExtractor, Watermark myWatermark) throws ImageManagerException, FileNotFoundException, IOException,
            PDFManagerException, ImageInterpreterException, URISyntaxException, MetsException {
        PDFManager pdfmanager = null;

        String creator = "";
        String title = "";
        String keywords = "";

        int documentpartcounter = 0;
        LinkedList<METSParser> allMetsParser = new LinkedList<METSParser>();
        HashMap<Integer, UrlImage> allPages = new HashMap<Integer, UrlImage>();
        HashMap<Integer, String> allPageNames = new HashMap<Integer, String>();
        HashMap<Integer, PDFTitlePage> allTitlePages = new HashMap<Integer, PDFTitlePage>();
        List<PDFBookmark> allRootBookmarks = new LinkedList<PDFBookmark>();

        // iterate over all DocumentParts
        for (DocumentPart dp : metsparts) {

            documentpartcounter++;
            String pdfdivid = null;
            Map<Integer, UrlImage> documentpartPages = null;

            if (dp.getType() == DocumentPartType.METS) {
                // read the METS file and handle all the images
                // TODO: Do not invoke the METS Parser direct, use the interface instead
                METSParser metsparser = new METSParser(dp.getUrl(), true);

                // set METSParser configuration
                metsparser.setMetadataextractor(inMetadataExtractor);
                metsparser.setStructureMetadataExtractor(inBookmarkExtractor);
                if (dp.getMetsFileGroup() != null) {
                    metsparser.setFilegroupsuseattributevalue(dp.getMetsFileGroup());
                }

                if (dp.getDivid() == null) {
                    pdfdivid = metsparser.getUppermostDivIDForPDF();
                } else {
                    pdfdivid = dp.getDivid();
                }

                // calculate metadata
                inMetadataExtractor.calculatePDFMetadata(pdfdivid, metsparser);

                String title1 = inMetadataExtractor.getPdftitle();
                String creator1 = inMetadataExtractor.getPdfcreator();
                String keywords1 = inMetadataExtractor.getPdfkeywords();

                if (title1 != null) {
                    if (title.equals("")) {
                        title = title1;
                    } else {
                        title = title + "; " + title1;
                    }
                }

                if (creator1 != null) {
                    if (creator.equals("")) {
                        creator = creator1;
                    } else {
                        creator = creator + "; " + creator1;
                    }
                }

                if (keywords1 != null) {
                    if (keywords.equals("")) {
                        keywords = keywords1;
                    } else {
                        keywords = keywords + "; " + keywords1;
                    }
                }

                LOGGER.debug("Title1: " + title1);
                LOGGER.debug("Creator1: " + creator1);
                LOGGER.debug("Keywords1: " + keywords1);

                metsparser.getAllFilesForRelatedDivs(pdfdivid); // get page

                // names
                Map<Integer, String> myPageNames = metsparser.getPageNames();

                // get list of files and pagenames
                documentpartPages = metsparser.getImageMap();

                if (documentpartPages.isEmpty()) {
                    // nothing in here; probably METS file has no pages
                    // don't add METS file to list
                    LOGGER.error("No page files / page urls available!");
                } else {
                    // change page names to make them unique
                    // within the PDF, different METSparsers
                    // will have same pageName

                    for (Integer i : documentpartPages.keySet()) {
                        UrlImage page = documentpartPages.get(i);
                        String pagename = myPageNames.get(i);
                        // calculate new integer
                        int dpc = (documentpartcounter * 1000) + i;

                        LOGGER.debug("adding page " + dpc + " to list");

                        // add to new HashMaps
                        allPages.put(dpc, page);
                        allPageNames.put(dpc, pagename);
                    }

                    // handle all bookmarks
                    // need to change page number as well
                    List<PDFBookmark> bookmarks;
                    bookmarks = PDFBookmark.convertList(metsparser.getStructureList());
                    for (PDFBookmark b : bookmarks) {
                        // change page numbers
                        changeBookmarksPagenumber(b, documentpartcounter);
                        allRootBookmarks.add(b);
                    }

                    // add METSParser to list
                    allMetsParser.add(metsparser);
                }
            } else if (dp.getType() == DocumentPartType.PDF) {
                // handle the PDF part
                PdfReader pdfreader = new PdfReader(dp.getUrl());

                int numberofpages = pdfreader.getNumberOfPages();
                for (Integer i = 1; i < numberofpages + 1; i++) {
                    PDFPage pdfpage = new PDFPage();
                    pdfpage.setPdfreader(pdfreader);
                    pdfpage.setPageNumber(i);

                    int dpc = (documentpartcounter * 1000) + (i);
                    LOGGER.debug("adding page " + dpc + " to list");

                    // add page to allPages
                    allPages.put(dpc, pdfpage);

                    // adding page labales
                    String labels[] = PdfPageLabels.getPageLabels(pdfreader);
                    if ((labels != null) && (i < labels.length)) {
                        LOGGER.debug("adding Page label (" + i + "):" + labels[i - 1]);
                        allPageNames.put(dpc, labels[i - 1].substring(0, labels[i - 1].length() - 1));
                    }
                }

                // add Bookmarks
                allRootBookmarks = extractBookmarksFromPDF(pdfreader, allRootBookmarks, documentpartcounter);
            }
            // handle the title page of this DocumentPart
            if (dp.getTitlepage() != null) {
                // title page is available
                // set the layout of the content file

                // set structType
                if (inMetadataExtractor.getStructType() != null) {
                    dp.getTitlepage().setStructuretype(inMetadataExtractor.getStructType());
                }

                dp.getTitlepage().deleteTitleLines();

                // set Lines
                if (inMetadataExtractor.getPdfTitlepageLine1() != null) {
                    PDFTitlePageLine ptl = new PDFTitlePageLine(inMetadataExtractor.getPdfTitlepageLine1());
                    ptl.setContent(inMetadataExtractor.getPdfTitlepageLine1());
                    ptl.setLinetype(2);
                    ptl.setFontsize(14);
                    dp.getTitlepage().addPDFTitlePageLine(ptl);
                }

                if (inMetadataExtractor.getPdfTitlepageLine2() != null) {
                    PDFTitlePageLine ptl = new PDFTitlePageLine(inMetadataExtractor.getPdfTitlepageLine2());
                    ptl.setLinetype(2);
                    ptl.setFontsize(10);
                    dp.getTitlepage().addPDFTitlePageLine(ptl);
                }

                if (inMetadataExtractor.getPdfTitlepageLine3() != null) {
                    PDFTitlePageLine ptl = new PDFTitlePageLine(inMetadataExtractor.getPdfTitlepageLine3());
                    ptl.setLinetype(2);
                    ptl.setFontsize(10);
                    dp.getTitlepage().addPDFTitlePageLine(ptl);
                }

                if (inMetadataExtractor.getPdfTitlepageLine4() != null) {
                    PDFTitlePageLine ptl = new PDFTitlePageLine(inMetadataExtractor.getPdfTitlepageLine4());
                    ptl.setLinetype(2);
                    ptl.setFontsize(10);
                    dp.getTitlepage().addPDFTitlePageLine(ptl);
                }

                // get name of the first page
                if (documentpartPages != null) {
                    Map<Integer, UrlImage> sortedMap = new TreeMap<Integer, UrlImage>(documentpartPages);
                    Iterator<Integer> it2 = sortedMap.keySet().iterator();
                    Integer firstpagename = 0;
                    // TODO: GDZ: Should this just get the first element? - yes
                    // I tried to find a more elegant way but my google didn't work #googleneverlikedme
                    while (it2.hasNext()) {
                        firstpagename = it2.next();
                        firstpagename = (documentpartcounter * 1000) + firstpagename;
                        LOGGER.debug("Adding PDFTitlePage at page " + firstpagename);
                        break;
                    }
                    allTitlePages.put(firstpagename, dp.getTitlepage());
                }
            }
        } // end of while over all document parts

        // setting for PDFManager
        pdfmanager = new PDFManager(allPages);
        pdfmanager.setAlwaysUseRenderedImage(pdfconfig.isPdfDefaultAlwaysUseRenderedImage());
        pdfmanager.setAlwaysCompressToJPEG(pdfconfig.isPdfDefaultAlwaysCompressToJPEG());
        pdfmanager.setPdfa(pdfconfig.isWriteAsPdfA());

        // set pages
        LOGGER.debug(allPages.size() + " pages for PDFManager set");
        pdfmanager.setImageURLs(allPages);
        pdfmanager.setImageNames(allPageNames);
        pdfmanager.setStructureList(allRootBookmarks);
        pdfmanager.setPdftitlepages(allTitlePages);

        // set metadata
        if (!title.equals("")) {
            pdfmanager.setTitle(title);
        }
        if (!creator.equals("")) {
            pdfmanager.setCreator(creator);
            pdfmanager.setAuthor(creator);
        }
        if (!keywords.equals("")) {
            pdfmanager.setKeyword(keywords);
        }

        // set an ICC profile
        if (pdfconfig.getIccinputfilename() != null) {
            ICC_Profile iccprofile = ICC_Profile.getInstance(pdfconfig.getIccinputfilename());
            pdfmanager.setIccprofile(iccprofile);
        }

        pdfmanager.createPDF(out, pdfconfig.getPagesize(), myWatermark);
    }

    private void changeBookmarksPagenumber(PDFBookmark b, Integer i) {
        int imagenumber = b.getImageNumber();

        int newimagenumber = (i * 1000) + imagenumber;
        b.setImageNumber(newimagenumber);

        List<PDFBookmark> children = b.getChildren();
        if ((children != null) && !children.isEmpty()) {
            changeBookmarksPagenumber(children.get(0), i);
        }
    }

    private List<PDFBookmark> extractBookmarksFromPDF(PdfReader pdfreader, List<PDFBookmark> pDFBookmarks, int documentpartnumber) {
        // add Bookmarks
        List<?> list = SimpleBookmark.getBookmark(pdfreader);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (list != null) {
            try {
                // TODO: GDZ: Do we really need this XML step here?
                SimpleBookmark.exportToXML(list, baos, "UTF8", false);
                String bms = baos.toString();
                LOGGER.debug(bms);
                // parse the xml, find Title elements
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(bms)));

                Element e = doc.getDocumentElement();
                NodeList nl = e.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    Node n = nl.item(i);
                    if ((n.getNodeType() == Node.ELEMENT_NODE) && (n.getNodeName().equals("Title"))) {
                        // found the right node, get title and pagenumber
                        PDFBookmark bm = parseTitleNode(n, documentpartnumber);
                        // this is the uppermost bookmark, add it to rootbookmark list
                        pDFBookmarks.add(bm);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("IOExeption occured", e);
            } catch (ParserConfigurationException e) {
                LOGGER.error("ParserConfigurationException occured", e);
            } catch (SAXException e) {
                LOGGER.error("SAXException occured", e);
            }
        }
        return pDFBookmarks;
    }

    /**
     * parses a single <title> element and its child elements
     * 
     * @param inTitleNode
     * @param documentpartcounter
     * @return a Bookmark element with content and pagenumber
     */
    private PDFBookmark parseTitleNode(Node inTitleNode, int documentpartcounter) {
        String bookmarktitle = getElementValue(inTitleNode);
        NamedNodeMap nnm = inTitleNode.getAttributes();
        Node pageattribute = nnm.getNamedItem("Page");
        String pagenumber[] = pageattribute.getNodeValue().split(" ");

        PDFBookmark bm = new PDFBookmark();
        bm.setContent(bookmarktitle);
        try {
            int pagenumber_int = Integer.parseInt(pagenumber[0]);
            // calculate new pagenumber
            int dpc = (documentpartcounter * 1000) + pagenumber_int;
            bm.setImageNumber(dpc);
        } catch (Exception e) {
            LOGGER.error("Bookmark does not contain a page number");
        }

        // check if this bookmark has further children
        NodeList nl = inTitleNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node childnode = nl.item(i);
            if ((childnode.getNodeType() == Node.ELEMENT_NODE) && (childnode.getNodeName().equals("Title"))) {
                PDFBookmark childbookmark = parseTitleNode(childnode, documentpartcounter);
                bm.addChildBookmark(childbookmark);
            }
        }

        return bm;
    }

    private String getElementValue(Node inNode) {
        NodeList nl = inNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                String value = n.getNodeValue();
                return value.trim();
            }
        }
        return null;
    }

}
