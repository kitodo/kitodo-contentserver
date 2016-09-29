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
package de.unigoettingen.sub.commons.simplemets;

import gov.loc.mets.DivType;
import gov.loc.mets.DivType.Mptr;
import gov.loc.mets.MdSecType.MdWrap.XmlData;
import gov.loc.mods.v3.DateType;
import gov.loc.mods.v3.IdentifierType;
import gov.loc.mods.v3.ModsDocument;
import gov.loc.mods.v3.NamePartType;
import gov.loc.mods.v3.NameType;
import gov.loc.mods.v3.PlaceTermType;
import gov.loc.mods.v3.RoleType;
import gov.loc.mods.v3.RoleType.RoleTerm;
import gov.loc.mods.v3.TitleInfoType;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.unigoettingen.sub.commons.simplemets.exceptions.MetsException;

/*******************************************************************************
 * Simple implementation of MetadataExtractor for PDF files
 * 
 * @version 12.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ******************************************************************************/
public class SimplePDFMetadataExtractor implements MetadataExtractor {

    // metadata fields for PDF
    private String pdftitle;
    private String pdfcreator;
    private String pdfkeywords;

    // metadata fields for PDF cover page
    private String pdfTitlepageLine1;
    private String pdfTitlepageLine2;
    private String pdfTitlepageLine3;
    private String pdfTitlepageLine4;

    // define standard value
    // these values are used for comparing the MODS sections with METS
    private String structtype = null;
    private String modsRoletermType = null;
    private String modsRoletermAuthority = null;
    private String modsRoletermAuthorvalue = null;
    private String modsRoletermEditorvalue = null;
    private String modsRoletermTranslator = null;
    private String modsRoletermCreatorvalue = null;
    private String modsIdentifierType = null;

    private static final Logger LOGGER = Logger.getLogger(SimplePDFMetadataExtractor.class);
    private String mods_namespaceDeclaration = "declare namespace mods='" + METSParser.MODS_NAMESPACE + "';";
    private ModsDocument mods = null; // the MODS document

    SimplePDFMetadataExtractor parent_spme = null; // metadataextractor for
                                                   // parent element

    public SimplePDFMetadataExtractor() {
        activateDFGConfiguration(); // set default values
    }

    @Override
    public String getStructType() {
        return structtype;
    }

    public String getTitle() {
        return pdftitle;
    }

    public String getCreator() {
        return pdfcreator;
    }

    public String getKeywords() {
        return pdfkeywords;
    }

    @Override
    public void calculatePDFMetadata(String inDivID, METSParser metsparser) throws MetsException {

        DivType inDiv = metsparser.getDIVbyID(inDivID);
        if (inDiv == null) {
            LOGGER.warn("Can't obtain <div> element with ID=" + inDivID + "/n no metadata could be calculated");
            return;
        }
        calculateMetadata(inDiv, metsparser);
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor# calculatePDFMetadata(gov.loc.mets.DivType,
     * de.unigoettingen.sub.gdz.goobi.contentServlet.METSParser)
     */
    @Override
    public void calculateMetadata(DivType inDiv, METSParser metsparser) throws MetsException {

        // get the appropriate metadata section
        XmlData xmldataElement = metsparser.getDmdSecForDiv(inDiv, "MODS");
        if (xmldataElement == null) {
            // no MODS section available
            LOGGER.debug("DIV does not have a DMDSEC with MODS metadata");
            return;
        }
        InputStream modsinputstream = xmldataElement.newInputStream();

        try {
            mods = ModsDocument.Factory.parse(modsinputstream);
        } catch (XmlException e) {
            LOGGER.error("Invalid MODS section, not compliant to MODS schema");
            MetsException metse = new MetsException("Invalid MODS section, not compliant to MODS schema");
            throw metse;
        } catch (IOException e) {
            LOGGER.error("IOException while parsing MODS section embedded into METS");
            MetsException metse = new MetsException("IOException while parsing MODS section embedded into METS");
            throw metse;
        }

        // calculate PDF Metadata
        this.extractTitle();
        this.extractCreator();
        this.extractKeywords();
        this.extractStructType(inDiv);

        // calculate lines for PDF titlepage
        // we have different methods, depending what type
        // the div is:
        //
        // a monograph: if the div is the uppermost and attached to a
        // physical div; the monograph div must not have any
        // mptr elements
        // a multivolume: if the div is the uppermost logical div and it
        // is not attached to any physical div
        // a volume: div which has only a single div element as a parent. The
        // parent div must have a mptr element
        // a chapter: for everything else

        DivType uppermostlogicalDiv = metsparser.getUppermostLogicalDiv();
        if (uppermostlogicalDiv.equals(inDiv)) {
            // it can be a multivolume or a monograph

            // check, if the uppermost logical div is connected to
            // any physical div

            List<DivType> physDivList = metsparser.getRelatedDivsForDiv(inDiv);
            if ((physDivList == null) || (physDivList.isEmpty())) {
                // no physical entity available;
                // it's a multivolume
                this.calculateLinesForMonograph();
            } else {
                // physical entity available
                this.calculateLinesForMonograph();
            }
        } else {
            // it's not the uppermost div; it either an article
            // or a volume
            // check if the parent div has a mptr element
            DivType parentDiv = metsparser.getParentDiv(inDiv);

            // LOGGER.debug("parent found: parent type is:" + parentDiv.getTYPE());

            if (parentDiv == null) {
                // no parent div; can't really be, as it is NOT the uppermost
                // div
                LOGGER.error("Can't create PDF metadata - div has no parent!");
                this.calculateLinesForMonograph();
            } else {
                LOGGER.debug("parent found: parent type is:" + parentDiv.getTYPE());

                // check mptr
                List<Mptr> metspointer = parentDiv.getMptrList();
                if (metspointer.isEmpty()) {
                    // its a volume; there must only be a single metspointer
                    // element
                    this.calculateLinesForVolume(metspointer.get(0), metsparser);
                } else {
                    // it's a chapter or article
                    this.calculateLinesForChapter(inDiv, metsparser);
                }
            }
        }
    }

    protected void extractStructType(DivType inDiv) {
        structtype = inDiv.getTYPE();
    }

    /***************************************************************************
     * extract title
     **************************************************************************/
    protected void extractTitle() {

        String maintitle = "";

        if (mods == null) {
            return;
        }

        // XPATH query for the MODS section
        // get the title
        String queryExpression = "//mods:titleInfo";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        for (XmlObject object : objects) {
            TitleInfoType titleinfo = (TitleInfoType) object;
            maintitle = addListToString(maintitle, titleinfo.getTitleList());

        }

        pdftitle = maintitle;
    }

    /**
     * extracts the volumenumber from mods:Part
     * 
     * @return
     */
    protected String extractVolumeNumber() {
        String volumenumber = "";

        if (mods == null) {
            return volumenumber;
        }

        // XPATH query for the MODS section
        // get the <titleInfo> element
        String queryExpression = "//mods:part[@type='host']/mods:detail/mods:number";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        if (objects.length > 0) {
            String nodecontents = getObjectContents(objects[0]);
            volumenumber = nodecontents;
        }

        return volumenumber;
    }

    /**
     * extract the main title
     * 
     * @return
     */
    public String extractMainTitle() {

        String maintitle = "";

        if (mods == null) {
            return maintitle;
        }

        // XPATH query for the MODS section
        // get the <titleInfo> element
        String queryExpression = "//mods:titleInfo";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        // iterate over the elements; there may be many <title> elements
        // even a <nonSort> element
        for (XmlObject object : objects) {
            TitleInfoType titleinfo = (TitleInfoType) object;

            // nonSort elements are always before <title> elements
            // they contain the part of the title, which is NOT used for
            // sorting
            maintitle = addListToString(maintitle, titleinfo.getNonSortList());
            maintitle = addListToString(maintitle, titleinfo.getTitleList());

        }
        return maintitle;
    }

    /**
     * add content of String-List to String
     * 
     * @return completed String
     */
    private String addListToString(String inString, List<String> inList) {
        for (String tit : inList) {
            if (tit != null) {
                // delete whitespaces and linebreaks
                String content = tit.replaceAll("\\s{2,}", " ");
                inString = inString + " " + content.replaceAll("\n", "");
            }
        }
        return inString;
    }

    /**
     * extracts the places of publication; takes only non-encoded (text based) information If information is not available, an empty string is
     * returned. The string is NOT null;
     * 
     * @return
     */
    protected String extractPlacePublication() {

        String place = "";

        if (mods == null) {
            return place;
        }

        // XPATH query for the MODS section
        // get the <titleInfo> element
        String queryExpression = "//mods:originInfo/mods:place/mods:placeTerm[@type='text']";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        for (XmlObject object : objects) {
            PlaceTermType placeterm = (PlaceTermType) object;
            String nodecontents = getObjectContents(placeterm);
            if (place.equals("")) {
                place = nodecontents;
            } else {
                place = place + ", " + nodecontents;
            }
        }

        return place;
    }

    /**
     * extract the date of publication in the original form. &lt:mods:originInfo&gt;/&lt;mods:dateIssued&gt; is extracted independ of any The
     * characters "[" and "]" are deleted if the value of the node starts/ends with them.
     * 
     * @return
     */
    protected String extractDatePublication() {

        String date = "";

        if (mods == null) {
            return date;
        }

        // XPATH query for the MODS section
        // get the <titleInfo> element
        String queryExpression = "//mods:originInfo/mods:dateIssued";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        for (XmlObject object : objects) {
            DateType placeterm = (DateType) object;
            String nodecontents = getObjectContents(placeterm);
            if (nodecontents.startsWith("[")) { // delete the first character
                nodecontents = nodecontents.substring(1);
            }
            if (nodecontents.endsWith("]")) { // delete the last character if
                // a bracket
                nodecontents = nodecontents.substring(0, nodecontents.length() - 1);
            }

            if (date.equals("")) {
                date = nodecontents;
            } else {
                date = date + ", " + nodecontents;
            }
        }
        return date;
    }

    /***************************************************************************
     * Extracts all the creators from the MODS record using their
     **************************************************************************/
    protected void extractCreator() {
        String creator = "";

        if (mods == null) {
            return;
        }

        // XPATH query for the MODS section
        // get the author
        String queryExpression = "//mods:name";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        // iterate over all mods name elements
        for (XmlObject object : objects) {
            // just set those roles which are added (in a particular
            // order) to the creator string
            boolean isauthor = false;
            boolean iscreator = false;
            boolean iseditor = false;
            boolean istranslator = false;

            String concatname = null; // concatented name being added
            String displayname = null;
            String firstname = null;
            String lastname = null;

            NameType name = (NameType) object;

            // get name parts (first and lastname)
            List<NamePartType> npts = name.getNamePartList();
            // the first and
            // lastname

            // get roles; iterate over all roles
            // onl certain roles are displayed
            //
            List<RoleType> rtypes = name.getRoleList();

            for (RoleType rtype : rtypes) {

                List<RoleTerm> rterms = rtype.getRoleTermList();
                // iterate over terms
                // only use authors (and if not available translators)
                // one RoleType may have several terms

                for (RoleTerm rterm : rterms) {
                    String code = null;
                    String authority = null;
                    try {
                        code = rterm.getType().toString();
                        authority = rterm.getAuthority();
                    } catch (Exception e) {
                        // probably null pointer exception, because roleTerm
                        // didn't have an quthority or type attribute
                        LOGGER.debug("roleTerm element hasn't either a type or a authority attribute");
                        continue;
                    }
                    if ((code == null) || (authority == null)) {
                        continue; // not the right one
                    } else if ((code.equals(this.modsRoletermType)) && (authority.equals(this.modsRoletermAuthority))) {
                        // get value of roleterm
                        String role = rterm.getStringValue();

                        // check if this role is used as a creator
                        if (role.equalsIgnoreCase(this.modsRoletermAuthorvalue)) {
                            isauthor = true;
                        }
                        if (role.equalsIgnoreCase(this.modsRoletermTranslator)) {
                            istranslator = true;
                        }
                        if (role.equalsIgnoreCase(this.modsRoletermEditorvalue)) {
                            iseditor = true;
                        }
                        if (role.equalsIgnoreCase(this.modsRoletermCreatorvalue)) {
                            iscreator = true;
                        }
                    }
                }// end of iteration over all role terms
            } // end of iteration over all role types

            // check if name needs to be added, if so extract name
            if ((isauthor) || (istranslator) || (iseditor) || (iscreator)) {
                // extract name and
                // iterate over all name part elements
                for (NamePartType npt : npts) {
                    String nameparttype = npt.getType().toString();
                    if (nameparttype.equals("displayname")) {
                        displayname = npt.getStringValue();
                    } else if (nameparttype.equals("family")) {
                        lastname = npt.getStringValue();
                    } else if (nameparttype.equals("given")) {
                        firstname = npt.getStringValue();
                    }
                }
                // concatenate name
                if ((lastname == null) && (firstname == null)) {
                    // no first and lastname given, take display name
                    concatname = displayname;
                } else {
                    // first and lastname given
                    if ((lastname == null) && (firstname != null)) {
                        concatname = firstname;
                    } else if ((lastname != null) && (firstname == null)) {
                        concatname = lastname;
                    } else {
                        concatname = lastname + ", " + firstname;
                    }
                }
            }

            if (concatname != null) {
                // add it
                if (creator.equals("")) {
                    creator = concatname;
                } else {
                    creator = creator + "; " + concatname;
                }
            }

        } // end of iteraton over all names

        pdfcreator = creator;
    }

    /***************************************************************************
     * Extracts all the creators from the MODS record using their
     **************************************************************************/
    protected String extractAuthor() {
        String creator = "";

        if (mods == null) {
            return creator;
        }

        // XPATH query for the MODS section
        // get the author
        String queryExpression = "//mods:name";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        // iterate over all mods name elements
        for (XmlObject object : objects) {
            // just set those roles which are added (in a particular
            // order) to the creator string
            boolean isauthor = false;

            String concatname = null; // concatented name being added
            String displayname = null;
            String firstname = null;
            String lastname = null;

            NameType name = (NameType) object;

            // get name parts (first and lastname)
            List<NamePartType> npts = name.getNamePartList();

            // get roles; iterate over all roles
            // onl certain roles are displayed
            List<RoleType> rtypes = name.getRoleList();
            for (RoleType rtype : rtypes) {
                List<RoleTerm> rterms = rtype.getRoleTermList();
                // iterate over terms
                // only use authors (and if not available translators)
                // one RoleType may have several terms
                for (RoleTerm rterm : rterms) {
                    String code = null;
                    String authority = null;
                    try {
                        code = rterm.getType().toString();
                        authority = rterm.getAuthority();
                    } catch (Exception e) {
                        // probably null pointer exception, because roleTerm
                        // didn't have an quthority or type attribute
                        LOGGER.debug("roleTerm element hasn't either a type or a authority attribute");
                        continue;
                    }
                    if ((code == null) || (authority == null)) {
                        continue; // not the right one
                    } else if ((code.equals(this.modsRoletermType)) && (authority.equals(this.modsRoletermAuthority))) {
                        // get value of roleterm
                        String role = rterm.getStringValue();

                        // check if this role is used as a creator
                        if (role.equalsIgnoreCase(this.modsRoletermAuthorvalue)) {
                            isauthor = true;
                        }
                    }
                }// end of iteration over all role terms
            } // end of iteration over all role types

            // check if name needs to be added, if so extract name
            if (isauthor) {
                // extract name and
                // iterate over all name part elements
                for (NamePartType npt : npts) {
                    String nameparttype = npt.getType().toString();
                    if (nameparttype.equals("displayname")) {
                        displayname = npt.getStringValue();
                    } else if (nameparttype.equals("family")) {
                        lastname = npt.getStringValue();
                    } else if (nameparttype.equals("given")) {
                        firstname = npt.getStringValue();
                    }
                }
                // concatenate name
                if ((lastname == null) && (firstname == null)) {
                    // no first and lastname given, take display name
                    concatname = displayname;
                } else {
                    // first and lastname given
                    if ((lastname == null) && (firstname != null)) {
                        concatname = firstname;
                    } else if ((lastname != null) && (firstname == null)) {
                        concatname = lastname;
                    } else {
                        concatname = lastname + ", " + firstname;
                    }
                }
            }

            if (concatname != null) {
                // add it
                if (creator.equals("")) {
                    creator = concatname;
                } else {
                    creator = creator + "; " + concatname;
                }
            }

        } // end of iteraton over all names

        return creator;
    }

    /***************************************************************************
     * extracts keywords, currently the only keyword is the identifier
     **************************************************************************/
    protected void extractKeywords() {
        String keywords = null;

        if (mods == null) {
            return;
        }

        String queryExpression = "//mods:identifier[@type=\"" + modsIdentifierType + "\"]";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        for (XmlObject object : objects) {
            IdentifierType identifier = (IdentifierType) object;
            if (keywords == null) {
                keywords = identifier.getStringValue();
            } else {
                keywords = "\n" + identifier.getStringValue();
            }
        }

        this.pdfkeywords = keywords;
    }

    /**
     * fills the pdf_titlepage_lines 1-4 with sensible values for a Monograph or Monograph-like &lt;div&gt; elements
     * 
     * first line contains the main title second line contains the author third line contains publisher and year of publication
     */
    private void calculateLinesForMonograph() {
        String title = this.extractMainTitle();
        String author = this.extractAuthor();
        String place = this.extractPlacePublication();
        String date = this.extractDatePublication();

        this.pdfTitlepageLine1 = title; // maybe we should shorten the title
        if (author != null) {
            this.pdfTitlepageLine2 = "by: " + author; // author doesn't need
            // to be
        }
        // shortened
        if (place.equals("") && (!date.equals(""))) {
            this.pdfTitlepageLine3 = "published in " + date;
        } else if ((!place.equals("")) && (date.equals(""))) {
            this.pdfTitlepageLine3 = place;
        } else {
            this.pdfTitlepageLine3 = place + "; " + date;
        }
    }

    /**
     * retireves the parent METS file. The file's location is stored in the Mptr element as a URL (file or http url).
     * 
     * @param inMptr
     * @return
     */
    private SimplePDFMetadataExtractor parseParentMETSFile(Mptr inMptr, URL baseurl) {

        if (parent_spme != null) {
            // we already read the parent METS file and parsed the metadata
            return parent_spme;
        }

        // get METSParser for parent
        String mptrLocation = inMptr.getHref();
        URL parentURL;
        try {

            if (mptrLocation.startsWith(".")) {
                // it's a relative URL;
                parentURL = new URL(baseurl, mptrLocation);
            } else {
                parentURL = new URL(mptrLocation);
            }

            METSParser parentParser = new METSParser(parentURL, false);
            parent_spme = new SimplePDFMetadataExtractor();
            parentParser.setMetadataextractor(parent_spme);

            parentParser.loadMETS(); // load the file and parse it

            // calculate the metadata for the parent; the
            // parent is always the logical topmost div
            DivType uppermostDiv = parentParser.getUppermostLogicalDiv();
            if (uppermostDiv != null) {
                parent_spme.calculateMetadata(uppermostDiv, parentParser);
            } else {
                LOGGER.error("Parent METS file " + mptrLocation + " doesn't have an uppermost logical div");
                return null;
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid URL for pointing to parent METS file:" + mptrLocation);
            return null;
        } catch (MetsException me) {
            LOGGER.warn("Parsing parent-METS file NOT successful:" + mptrLocation + "\n" + me);
            return null;
        } catch (IOException ieo) {
            LOGGER.warn("Reading parent-METS file NOT successful:" + mptrLocation + "\n" + ieo);
            return null;
        } catch (URISyntaxException use) {
            LOGGER.warn("URIfor parent-METS file seems to be invalid:" + mptrLocation + "\n" + use);
            return null;
        }

        return parent_spme;

    }

    /**
     * line1: title of multivolume, if not available, title of the volume line2: volume number line3: author(s) of the multivolume; if not available,
     * authors of the volume line4: year, publisher and location of volume, if not available of multivolume
     * 
     * @param inDiv
     * @param inMpts
     * @param inParser
     */
    private void calculateLinesForVolume(Mptr inMptr, METSParser inParser) {

        String place = this.extractPlacePublication();
        String date = this.extractDatePublication();
        String volumenumber = this.extractVolumeNumber();

        LOGGER.debug("Parse anchor/parent METS file for extracting metadata!");
        SimplePDFMetadataExtractor spme = parseParentMETSFile(inMptr, inParser.getUrl());

        if (spme == null) {
            LOGGER.warn("Couldn't read METS parent METS file from " + inMptr.getHref() + "\nUsing Monograph method as a fallback!");
            this.calculateLinesForMonograph();
            return;
        }

        // now we can extract the parent's metadata
        // line1 contains the title
        if (spme.getTitle() == null) {
            this.pdfTitlepageLine1 = this.extractMainTitle(); // maybe we
            // should shorten the
            // title
        } else {
            String myTitle = spme.getTitle();
            this.pdfTitlepageLine1 = myTitle;
            this.pdftitle = myTitle;
        }

        // line 2 contains the volume number
        if (volumenumber == null) {
            pdfTitlepageLine2 = "unknown volume";
        } else {
            pdfTitlepageLine2 = "volume: " + volumenumber;
        }

        // line3 contains author information
        if ((this.extractAuthor() == null) || (this.extractAuthor().equals(""))) {
            String author = spme.extractAuthor();
            if ((author == null) || (author.equals(""))) {
                // no author, neither in volume nor in multivolume
                this.pdfTitlepageLine3 = "by unknown author";
                this.pdfcreator = "unknown author";
            } else {
                // set author from parent (multivolume)
                this.pdfTitlepageLine3 = "by: " + author; // author
                this.pdfcreator = author;
            }
        } else {
            // set author of volume
            this.pdfTitlepageLine3 = "by: " + this.extractAuthor(); // author
            this.pdfcreator = this.extractAuthor();
        }

        // line 4 contains place and year
        if (place.equals("")) {
            // get place from parent
            place = spme.extractPlacePublication();
        }

        if (date.equals("")) {
            date = spme.extractDatePublication();
        }

        if (place.equals("") && (!date.equals(""))) {
            this.pdfTitlepageLine4 = "published in " + date;
        } else if ((!place.equals("")) && (date.equals(""))) {
            this.pdfTitlepageLine4 = place;
        } else {
            this.pdfTitlepageLine4 = place + "; " + date;
        }
    }

    /**
     * line1: title of chapter/article line2: if available, author of chapter/article; otherwise of parent line3: title of parent; if available volume
     * number of parent; page numbers line4: date and location of publication
     * 
     * @param inDiv
     * @param myMETSParser
     */
    private void calculateLinesForChapter(DivType inDiv, METSParser myMETSParser) {
        String parenttitle = null;
        String parentauthor = null;
        String parentvolumenumber = null;
        String parentplace = null;
        String parentyear = null;
        String startpagenumber = "unknown";
        String endpagenumber = "unknown";

        SimplePDFMetadataExtractor workMetadataExtractor = null; // extractor
                                                                 // for
                                                                 // parent
                                                                 // element

        // get title of chapter
        String chaptertitle = this.extractMainTitle();

        // check, if we have an author
        String chapterauthor = this.extractAuthor();

        // get title of work; therefore we must check,
        // if the uppermost structure is contained in this METS file, of
        // if we need to get another one.

        // get the uppermost logical structure
        DivType uppermostDiv = myMETSParser.getUppermostLogicalDiv();
        try {
            workMetadataExtractor = new SimplePDFMetadataExtractor();
            workMetadataExtractor.calculateMetadata(uppermostDiv, myMETSParser);
        } catch (MetsException e) {
            LOGGER.error("Can't parse the uppermost div-Element to retrieve metadata\nMETSException" + e);
            return;
        }
        parentvolumenumber = workMetadataExtractor.extractVolumeNumber();

        // now we can retrieve author and title information

        if (uppermostDiv.getMptrList().size() > 0) {
            // the uppermost div is NOT contained in this
            // METS file; the Mptr element contains the URL
            Mptr metspointer = uppermostDiv.getMptrArray(0); // get the first
            // one

            SimplePDFMetadataExtractor spme = this.parseParentMETSFile(metspointer, myMETSParser.getUrl());
            parenttitle = spme.getTitle();
            parentauthor = spme.getCreator();
            parentplace = spme.extractPlacePublication();
            parentyear = spme.extractDatePublication();

            // get volume number of uppermostdiv
            // the parent does not contain a volume number
            parentvolumenumber = workMetadataExtractor.extractVolumeNumber();

        } else {
            // the uppermost div is the parent
            parenttitle = workMetadataExtractor.extractMainTitle();
            parentauthor = workMetadataExtractor.extractAuthor();
            parentplace = workMetadataExtractor.extractPlacePublication();
            parentyear = workMetadataExtractor.extractDatePublication();
        }

        // get page numbers of the chapter
        //
        String divid = inDiv.getID();
        try {
            endpagenumber = myMETSParser.getEndpageNumber(divid);
            startpagenumber = myMETSParser.getStartpageNumber(divid);
        } catch (MetsException e) {
            LOGGER.error("Error while getting page numbers", e);
        }

        // build the lines
        //

        this.pdfTitlepageLine1 = chaptertitle;

        if ((chapterauthor == null) || (chapterauthor.equals(""))) {
            this.pdfTitlepageLine2 = "by " + parentauthor;
            this.pdfcreator = parentauthor;
        } else {
            this.pdfTitlepageLine2 = "by " + chapterauthor;
            this.pdfcreator = chapterauthor;
        }

        // line 3
        if ((parenttitle != null) && (parenttitle.length() > 80)) {
            parenttitle = parenttitle.substring(0, 80);
        }

        this.pdfTitlepageLine3 = "in: " + parenttitle;
        if ((parentvolumenumber != null) && (!parentvolumenumber.equals(""))) {
            this.pdfTitlepageLine3 = "; volume:" + parentvolumenumber;
        }
        this.pdfTitlepageLine3 = this.pdfTitlepageLine3 + ", (page(s) " + startpagenumber + " - " + endpagenumber + ")";

        // line 4
        if (parentplace.equals("") && (!parentyear.equals(""))) {
            this.pdfTitlepageLine4 = "published in " + parentyear;
        } else if ((!parentplace.equals("")) && (parentyear.equals(""))) {
            this.pdfTitlepageLine4 = parentplace;
        } else {
            this.pdfTitlepageLine4 = parentplace + "; " + parentyear;
        }
    }

    //
    // methods to implement the MetadataExtractor interface
    // this interface needs to be available in order
    // to extract metadata for the PDF from the METS file
    //
    //

    /*
     * (non-Javadoc)
     * 
     * @see de.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor#getPdftitle ()
     */
    @Override
    public String getPdftitle() {
        return pdftitle;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor#getPdfcreator ()
     */
    @Override
    public String getPdfcreator() {
        return pdfcreator;
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor# getPdfkeywords()
     */
    @Override
    public String getPdfkeywords() {
        return pdfkeywords;
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor# getPdf_titlepage_line1()
     */
    @Override
    public String getPdfTitlepageLine1() {
        return pdfTitlepageLine1;
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor# getPdf_titlepage_line2()
     */
    @Override
    public String getPdfTitlepageLine2() {
        return pdfTitlepageLine2;
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor# getPdf_titlepage_line3()
     */
    @Override
    public String getPdfTitlepageLine3() {
        return pdfTitlepageLine3;
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.unigoettingen.sub.gdz.goobi.contentServlet.MetadataExtractor# getPdf_titlepage_line4()
     */
    @Override
    public String getPdfTitlepageLine4() {
        return pdfTitlepageLine4;
    }

    //
    // helper methods
    //

    /***************************************************************************
     * get content of object as {@link String}
     * 
     * @param inObject {@link XmlObject} with content to get
     **************************************************************************/
    private String getObjectContents(XmlObject inObject) {
        Node node = inObject.getDomNode();
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node childnode = nl.item(i);
            if (childnode.getNodeType() == 3) {
                String content = childnode.getNodeValue();

                // delete whitespaces and linebreaks
                content = content.replaceAll("\\s{2,}", " ");
                content = content.replaceAll("\n", "");
                return content;
            }
        }
        return null;
    }

    public boolean hasAuthor() {
        // XPATH query for the MODS section
        // get the author
        String queryExpression = "//mods:name";
        String path = mods_namespaceDeclaration + " " + queryExpression;
        XmlObject[] objects = mods.selectPath(path);

        if (objects.length == 0) { // no authors
            return false;
        }

        // iterate over all mods name elements
        for (XmlObject object : objects) {
            NameType name = (NameType) object;

            // get roles; iterate over all roles
            List<RoleType> ryptes = name.getRoleList();
            for (RoleType rtype : ryptes) {
                List<RoleTerm> rterms = rtype.getRoleTermList();

                // iterate over terms
                // only use authors
                // one RoleType may have several terms

                for (int y = 0; y < objects.length; y++) {
                    String code = null;
                    String authority = null;
                    try {
                        code = rterms.get(y).getType().toString();
                        authority = rterms.get(y).getAuthority();
                    } catch (Exception e) {
                        // probably null pointer exception, because roleTerm
                        // didn't have an quthority or type attribute
                        LOGGER.debug("roleTerm element hasn't either a type or a authority attribute");
                        continue;
                    }
                    if ((code == null) || (authority == null)) {
                        continue; // not the right one
                    } else if ((code.equals(this.modsRoletermType)) && (authority.equals(this.modsRoletermAuthority))) {
                        // get value of roleterm
                        String role = rterms.get(y).getStringValue();

                        // check if this role is used as a creator
                        if (role.equals(this.modsRoletermAuthorvalue)) {
                            return true;
                        }
                    }
                }
            } // end iteration over all roles
        } // end of iteration over all names
        return false;
    }

    /***************************************************************************
     * for compatibility reasons exists this special adaptation for mods
     **************************************************************************/
    public void activateGDZConfiguration() {

        // MODS roleterm
        this.modsRoletermType = "code";
        this.modsRoletermAuthority = "gdz";
        this.modsRoletermAuthorvalue = "aut";
        this.modsRoletermEditorvalue = "edt";
        this.modsRoletermTranslator = "tra";
        this.modsRoletermCreatorvalue = "cre";

        // MODS identifier type
        this.modsIdentifierType = "GDZ";
    }

    /***************************************************************************
     * set here the configuration for DFG Viewer compatibility for mods
     **************************************************************************/
    public final void activateDFGConfiguration() {
        // roleterm
        this.modsRoletermType = "code";
        this.modsRoletermAuthority = "marcrelator";
        this.modsRoletermAuthorvalue = "AUT";
        this.modsRoletermEditorvalue = "EDT";
        this.modsRoletermTranslator = "TRA";
        this.modsRoletermCreatorvalue = "CRE";

        // MODS identifier type
        this.modsIdentifierType = "urn";
    }

}
