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
import gov.loc.mets.DivType.Fptr;
import gov.loc.mets.DivType.Mptr;
import gov.loc.mets.FileType;
import gov.loc.mets.FileType.FLocat;
import gov.loc.mets.MdSecType;
import gov.loc.mets.MdSecType.MdWrap;
import gov.loc.mets.MdSecType.MdWrap.XmlData;
import gov.loc.mets.MetsDocument;
import gov.loc.mets.MetsType.FileSec.FileGrp;
import gov.loc.mets.StructLinkType.SmLink;
import gov.loc.mets.StructMapType;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import de.unigoettingen.sub.commons.simplemets.exceptions.MetsException;
import de.unigoettingen.sub.commons.util.datasource.ImageSource;
import de.unigoettingen.sub.commons.util.datasource.SimpleStructure;
import de.unigoettingen.sub.commons.util.datasource.SimpleUrlImage;
import de.unigoettingen.sub.commons.util.datasource.Structure;
import de.unigoettingen.sub.commons.util.datasource.StructureSource;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;
import de.unigoettingen.sub.commons.util.stream.StreamUtils;

/************************************************************************************
 * The central METS-Parser class for all METS handling tasks.
 * 
 * @version 08.03.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 * **********************************************************************************/
// TODO: Add MetadataSource interface
public class METSParser implements StructureSource, ImageSource {

    public static final String METS_NAMESPACE = "http://www.loc.gov/METS/";
    public static final String MODS_NAMESPACE = "http://www.loc.gov/mods/v3";
    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    private URL url = null; // URL of METS file
    private String metsBasepath = null; // base path of METS file
    private String filegroupsuseattributevalue = "DEFAULT"; // default value
    private StructureMetadataExtractor sme = null;
    private MetadataExtractor metadataextractor = null;
    private MetsDocument mets = null; // the METS-Document itself
    private Map<String, DivType> divIDs = new HashMap<String, DivType>(); // contains all <div> elements which have ID attributes
    private Map<String, FileType> fileIDs = new HashMap<String, FileType>(); // contains <file> elements which have ID attributes
    private Map<String, FileGrp> allFileGroups = new HashMap<String, FileGrp>();
    private Map<FileType, FileGrp> allFiles = new HashMap<FileType, FileGrp>();
    private DivType uppermostlogicalDiv = null;
    private DivType uppermostphysicalDiv = null;
    protected boolean useCachedIDs = true;
    private Map<Integer, URL> pageUrls; // HashMap containing the pageurls
    private Map<Integer, String> pageNames = null; // contains all the page names (=page numbers)
    private List<SimpleStructure> rootStructureList = null; // list containing several root structures

    private static final Logger LOGGER = Logger.getLogger(METSParser.class);
    private static final String METS_NAMESPACEDECLARATION = "declare namespace mets='" + METS_NAMESPACE + "';";
    private static final String XLINK_NAMESPACEDECLARATION = "declare namespace xlink='" + XLINK_NAMESPACE + "';";

    /*************************************************************************************
     * Constructor for METSParser.
     * 
     * @param inUrl as {@link URL} for Mets file
     * 
     * @throws MetsException the mets exception
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException
     * ***********************************************************************************/
    public METSParser(URL inUrl) throws MetsException, URISyntaxException, IOException {
        this.init(inUrl, true);
    }

    /*************************************************************************************
     * Constructor for METSParser.
     * 
     * @param inUrl as {@link URL} for Mets file
     * @param cacheIDs true, if div and file ID attributes should be cached
     * 
     * @throws MetsException the mets exception
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException
     * **********************************************************************************/

    public METSParser(URL inUrl, boolean cacheIDs) throws MetsException, URISyntaxException, IOException {
        this.init(inUrl, cacheIDs);
    }

    /*************************************************************************************
     * initalizes the parser, reads the METS file etc.
     * 
     * @param inUrl as {@link URL} for Mets file
     * @param cacheIDs true, if div and file ID attributes should be cached
     * 
     * @throws MetsException the mets exception
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException
     * **********************************************************************************/
    private final void init(URL inUrl, boolean cacheIDs) throws MetsException, URISyntaxException, IOException {
        pageUrls = new HashMap<Integer, URL>(); // HashMap contains an integer
        // for ordering
        // and a URL (as string)
        pageNames = new HashMap<Integer, String>();
        rootStructureList = new LinkedList<SimpleStructure>();
        this.url = inUrl;
        this.useCachedIDs = cacheIDs;

        LOGGER.debug("METS parser instantiated");
        this.loadMETS(); // load the METS file

        if (cacheIDs) {
            readAllFILEwithID();
            readAllDIVwithID();
            readAllFileGroups();
        }
    }

    /*************************************************************************************
     * loads the METS file using xmlbeans.
     * 
     * @throws MetsException the mets exception
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException
     * ***********************************************************************************/
    protected void loadMETS() throws MetsException, URISyntaxException, IOException {
        InputStream metsInputStream = null;
        LOGGER.debug("load METS file " + this.url);

        metsBasepath = this.url.getPath();

        metsInputStream = StreamUtils.getInputStreamFromUrl(this.url, metsBasepath);
        try {
            mets = MetsDocument.Factory.parse(metsInputStream);
        } catch (XmlException e) {
            LOGGER.error("Error parsing the METS document " + this.url, e);
            throw new MetsException("ERROR while parsing METS file.", e);
        } catch (IOException e) {
            LOGGER.error("Error reading the METS document from " + this.url, e);
            return;
        }
        metsInputStream.close(); // close the input stream
    }

    /**************************************************************************************
     * Retrieves the &lt;div&gt; element's ID value for the entity for which a PDF file can be created.
     * 
     * @return the uppermost div id for pdf
     *************************************************************************************/
    public String getUppermostDivIDForPDF() {
        DivType pdfdiv = null;
        DivType uplogdiv = this.getUppermostLogicalDiv();
        if (uplogdiv == null) {
            LOGGER.debug("\n*\n* Can't create PDF; div seems to be an anchor\n*\n");
            return null;
        }

        // check, if we have <mptr> as children
        List<Mptr> mptr = uplogdiv.getMptrList();
        if ((mptr == null) || (mptr.isEmpty())) {
            // no mptr - must be a monograph
            // in this case the uppermost logical id is the one we are looking
            // for
            pdfdiv = uplogdiv;
        } else {
            // check, if we have a physical structmap
            DivType physDiv = this.getUppermostPhysicalDiv();
            if (physDiv == null) {
                // it is a multivolume or a periodical or anything like this
                // in this case the uppermost logical div is the one for which
                // we create the PDF
                pdfdiv = uplogdiv;
            } else {
                // it is the first child div; this represents the volume
                List<DivType> children = uplogdiv.getDivList();
                if ((children == null) || (children.isEmpty())) {
                    // this shouldn't happen
                    LOGGER.debug("Can't create PDF; can't find a div");
                    return null;
                }
                pdfdiv = children.get(0); // the first child
            }
        }

        String upid = pdfdiv.getID();

        return upid;
    }

    /*************************************************************************************
     * Retrieves the filename for the appropriate div. It uses the div elements FILEID attribute to find the correct FileType object (&lt;file&gt;
     * element) and uses it's FLOCAT child element. FCONTENT is unsupported. If the filegroupsuseattribute variable is set (see separate method to set
     * this value), it uses this value to find the appropriate filegroup. The div's file must be member of this group. If this switch is not set, it
     * uses the first &lt;file&gt; element The method is used to retrieve a single image file for a specific &lt;div&gt;. It does not matter if the
     * &lt;div&gt; is part of the logical or physical structure.
     * 
     * @param dividvalue the dividvalue
     * 
     * @return URL
     * 
     * @throws MetsException the mets exception
     * @throws MalformedURLException
     * ***********************************************************************************/
    public URL getURLForSingleDiv(String dividvalue) throws MetsException, MalformedURLException {
        LOGGER.debug("getFilenameForSingleDiv: dividvalue=" + dividvalue);

        // get <div> element with the specific value for its ID attribute
        DivType div = getDIVbyID(dividvalue);

        if (div == null) {
            LOGGER.debug("Can't find a <div> element with ID=" + dividvalue);
            throw new MetsException("Can't find a <div> element with ID=" + dividvalue);
        }

        // get file pointer for this Div
        FileType divsfile = getFileForDIV(div, filegroupsuseattributevalue);
        if (divsfile == null) {
            LOGGER.error("Can't find referenced <file> element for <div ID=\"" + dividvalue + "\">");
            throw new MetsException("Can't find referenced <file> element for <div ID=\"" + dividvalue + "\">");
        }
        LOGGER.debug("<file> element found with ID=" + divsfile.getID());

        // get filename for the file, therefore
        // we have to get the file location element
        if (divsfile.getFLocatList().size() == 0) {
            LOGGER.error("<file> element has no <FLocat> child!");
            throw new MetsException("<file> element has no <FLocat> child!");
        }

        FLocat filelocation = divsfile.getFLocatArray(0);
        // String locationtype = filelocation.getType();
        URL locationhref = new URL(filelocation.getHref());

        if (filelocation.getLOCTYPE() == null) {
            LOGGER.error("LOCTYPE attribute has no value");
            throw new MetsException("LOCTYPE attribute has no value");
        }
        String locationLocType = filelocation.getLOCTYPE().toString();

        // check, if we support thie location type.
        // currently we only suppoert URLs
        if (!"URL".equals(locationLocType)) {
            // the href location is NOT a URL
            throw new MetsException("Unsupported type for file locator for file with ID=" + divsfile.getID());
        }
        return locationhref;
    }

    /*************************************************************************************
     * get ID as {@link String} of upper most physical div element.
     * 
     * @return id of div as {@link String}
     * ***********************************************************************************/

    public String getIDofUppermostPhysicalDiv() {
        LOGGER.debug("get the ID of uppermost physical <div>");
        DivType uppermost = getUppermostPhysicalDiv();
        if (uppermost == null) {
            LOGGER.warn("Can't find uppermost physical <div> in METS file");
            return null; // no uppermost div was found
        } else {
            String id = uppermost.getID();
            if (id == null) {
                LOGGER.warn("Uppermost <div> element has no ID attribute!");
                return null;
            }
            return id;
        }
    }

    /*************************************************************************************
     * Parses all pages (if they are children of the uppermost physical structure) and fills the pageUrls and pageNames lists.
     * 
     * @param inphysDiv the inphys div
     * 
     * @throws MetsException the mets exception
     * @throws MalformedURLException
     * ***********************************************************************************/
    public void getAllPages(DivType inphysDiv) throws MetsException, MalformedURLException {
        List<DivType> allpages = inphysDiv.getDivList();

        if (allpages.isEmpty()) {
            // the div has own page links
            for (DivType page : allpages) {
                parsePageDIVs(page);
            }
        }
    }

    /*************************************************************************************
     * get dmd section for given {@link DivType}.
     * 
     * @param inDiv DivType as parameter
     * @param inMetadatatype as parameter
     * 
     * @return xmldata
     * @throws MetsException the mets exception
     *************************************************************************************/
    @SuppressWarnings("unchecked")
    public XmlData getDmdSecForDiv(DivType inDiv, String inMetadatatype) throws MetsException {
        List<String> dmdidlist = inDiv.getDMDID();

        if (dmdidlist == null) {
            // no DMDSec available
            return null;
        }

        // iterate over list and get the metadata type
        for (String dmdid : dmdidlist) {
            // get object with dmdid
            LOGGER.debug("getMetadataSectionForDiv: checking if metadata section " + dmdid + " is the one we are looking for");

            // get the logical StructMap
            String queryExpression = "//mets:dmdSec[@ID='" + dmdid + "']";

            XmlObject xObj = getXmlObjectFromIDQuery(queryExpression);

            try {
                // get the value of the MDTYPE and
                // OTHERMDTYPE attribute
                MdSecType dmdsec = (MdSecType) xObj;
                MdWrap mdwrap = dmdsec.getMdWrap();
                if (mdwrap == null) {
                    // it's an MdRef, which is not supported
                    LOGGER.warn("mdRef is unsupported for Metadata section ID=" + dmdid);
                    continue;
                }

                // get the metadata type of the MdSecTyoe
                String metadatatype = null; // type of extension schema
                String mdtype = mdwrap.getMDTYPE().toString();

                if (mdtype.equalsIgnoreCase("OTHER")) {
                    // we need to get the OTHERMDTYPE attribute
                    metadatatype = mdwrap.getOTHERMDTYPE();
                } else {
                    metadatatype = mdtype;
                }

                if (metadatatype.equals(inMetadatatype)) {
                    return mdwrap.getXmlData();
                }

            } catch (ClassCastException e) {
                LOGGER.error("Metadata Section with ID=" + dmdid + " is not a valid MdSecType", e);
                throw new MetsException("Metadata Section with ID=" + dmdid + " is not a valid MdSecType", e);
            }

        } // end of for over all dmdids
        return null;
    }

    /*************************************************************************************
     * Retrieves the ID attribute's value of the uppermost &lt;div&gt; element in the logical &lt;structMap&gt;.
     * 
     * @return value of ID attribute of uppermost div element
     * ***********************************************************************************/
    public String getIDofUppermostLogicalDiv() {
        LOGGER.debug("get the ID of uppermost logical <div>");
        DivType uppermost = getUppermostLogicalDiv();
        if (uppermost == null) {
            LOGGER.warn("Can't find uppermost logical <div> in METS file");
            return null; // no uppermost div was found
        } else {
            String id = uppermost.getID();
            if (id == null) {
                LOGGER.warn("Uppermost <div> element has no ID attribute!");
                return null;
            }
            return id;
        }
    }

    /**************************************************************************************
     * Returns the uppermost logical div, which has a relationship with a physical div. Only the first two hierarchical levels are considered.
     * 
     * @return the uppermost DivType element or null
     *************************************************************************************/
    public DivType getUppermostLogicalDivWithPhysRelationship() {
        LOGGER.debug("get the ID of uppermost logical <div> with a physical relationship");

        DivType uppermostdiv = getUppermostLogicalDiv();

        LinkedList<DivType> allRelated = null;
        try {
            allRelated = this.getRelatedDivsForDiv(uppermostdiv);
        } catch (MetsException e1) {
            return null;
        }

        if ((allRelated == null) || (allRelated.isEmpty())) {
            // this one has no relationships
            List<DivType> divs = uppermostdiv.getDivList();

            if (divs.isEmpty()) {
                // no children available
                return null;
            }

            for (int i = 0; i < divs.size(); i++) {
                LinkedList<DivType> childrelated = null;
                try {
                    childrelated = this.getRelatedDivsForDiv(divs.get(i));
                } catch (MetsException e) {
                    continue;
                }
                if (childrelated.size() > 0) {
                    // this child has related divs; so return it
                    return divs.get(i);
                }
                // no child has related divs
                return null;
            }
        } else {
            // has relationships; return it
            return uppermostdiv;
        }

        return null;
    }

    /**************************************************************************************
     * retrieves the uppermost physical DIV element.
     * 
     * @return the uppermost physical div
     *************************************************************************************/
    public DivType getUppermostPhysicalDiv() {
        LOGGER.debug("get the ID of uppermost physical <div>");

        if (uppermostphysicalDiv != null) {
            return uppermostphysicalDiv;
        }

        // get the logical StructMap
        String queryExpression = "//mets:structMap[@TYPE='PHYSICAL']";

        List<XmlObject> objects = readQueryList(queryExpression);
        LOGGER.trace("XPath retrieved " + objects.size() + " objects");
        if (objects.size() == 0) {
            LOGGER.warn("XPath didn't find any object, can't retrieve a physical <structMap>!");
            return null;
        } else {
            StructMapType physical = (StructMapType) objects.get(0);
            uppermostphysicalDiv = physical.getDiv(); // get the <div>
            return uppermostphysicalDiv;
        }
    }

    /**************************************************************************************
     * retrieves the uppermost logical DIV element.
     * 
     * @return the uppermost logical div
     *************************************************************************************/
    public DivType getUppermostLogicalDiv() {
        LOGGER.debug("get the ID of uppermost logical <div>");

        if (uppermostlogicalDiv != null) {
            return uppermostlogicalDiv;
        }

        // get the logical StructMap
        String queryExpression = "//mets:structMap[@TYPE='LOGICAL']";

        List<XmlObject> objects = readQueryList(queryExpression);
        LOGGER.debug("XPath retrieved " + objects.size() + " objects");
        if (objects.size() == 0) {
            LOGGER.warn("XPath didn't find any object, can't retrieve a logical <structMap>!");
            return null;
        } else {
            StructMapType logical = (StructMapType) objects.get(0);
            uppermostlogicalDiv = logical.getDiv(); // get the <div>
            return uppermostlogicalDiv;
        }
    }

    /**************************************************************************************
     * Gets the xml object from id query.
     * 
     * @param query the query
     * 
     * @return the xml object from id query
     *************************************************************************************/
    protected XmlObject getXmlObjectFromIDQuery(String query) {
        String path = METS_NAMESPACEDECLARATION + " $this" + query;
        // TODO: GDZ: Use the Method for selecting lists:
        // List<XmlObject> objects = readQueryList(queryExpression);

        List<XmlObject> objects = Arrays.asList(mets.selectPath(path));
        LOGGER.trace("XPath retrieved " + objects.size() + " objects");
        LOGGER.trace("XPath was set to:\n" + path);

        if (objects.size() == 1) {
            return objects.get(0);
        } else if (objects.size() > 1) {
            throw new IllegalStateException("XML IDs should be unique!");
        }
        return null;
    }

    /*************************************************************************************
     * Get all files for related divs. This method is used for filling the pageURLs and pageName HashMap. The given ID value for the div must be an ID
     * value of a &lt;div&t; element in the logical &lt;structMap&gt;
     * 
     * @param dividvalue value of the ID attribute
     * 
     * @throws MetsException the mets exception
     * @throws MalformedURLException
     * ***********************************************************************************/
    public void getAllFilesForRelatedDivs(String dividvalue) throws MetsException, MalformedURLException {
        LOGGER.debug("getAllFilesForRelatedDivs: <div ID=\"" + dividvalue + "\">");
        // get <div> element with the specific value for its ID attribute
        DivType div = getDIVbyID(dividvalue);
        if (div == null) {
            LOGGER.warn("Can't find any <div> element with ID=" + dividvalue);
            throw new MetsException("Can't find any <div> element with ID=" + dividvalue);
        }

        // get all associated <div> elements; those div
        // elements this object is pointing to in the
        // structLink section
        LinkedList<DivType> alldivs = getRelatedDivsForDiv(div);

        LOGGER.debug("retrieved the related <div> elements:" + alldivs.size() + " found");

        // iterate over the list and retrieve the appropriate file
        // names for those divs
        for (DivType myDiv : alldivs) {

            if (!myDiv.getTYPE().equalsIgnoreCase("page")) {
                // might be the link to the "physSequence"
                // get all pages for this physSequence
                this.getAllPages(myDiv);
            } else {
                LOGGER.debug("get file for related (page) div: ID=" + myDiv.getID());
                parsePageDIVs(myDiv); // get ORDER and ORDERLABEL
            }
        }
        createStructureList(dividvalue);
    }

    /*************************************************************************************
     * Retrieves page information from the &lt;div&gt; element as ORDER and ORDERLABEL as well as the filename (resolving the &lt;fptr&gt; element.
     * 
     * @param inDiv the in div
     * 
     * @throws MetsException the mets exception
     * @throws MalformedURLException
     ************************************************************************************/
    private void parsePageDIVs(DivType inDiv) throws MetsException, MalformedURLException {
        LOGGER.debug("parse page div:" + inDiv.getID());

        if (!inDiv.getTYPE().equals("page")) {
            // not a page, return
            LOGGER.debug("It's not a page div");
            return;
        }

        URL fileUrl = getURLForSingleDiv(inDiv.getID());
        LOGGER.debug("It is a page div: file found:" + fileUrl);

        if (fileUrl == null) { // no filename found
            // no filename available; check if we have a child div
            // which will have a filename
            // iterate over children and get their filenames
            for (DivType divType : inDiv.getDivList()) {
                parsePageDIVs(divType);// get filenames from pages
            }
        } else {

            // store filenames with pagenumber in a list
            BigInteger order_bi = inDiv.getORDER(); // get value of ORDER
            // attribute
            if (order_bi == null) {
                MetsException me = new MetsException("Page element has no ORDER attribute");
                throw me;
            }
            String order_str = order_bi.toString();
            int order = 0;
            try {
                order = Integer.parseInt(order_str);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid value for ORDER attribute for <file ID=\"" + inDiv.getID() + "\">", e);
                throw new MetsException("Invalid value for ORDER attribute for <file>", e);
            }
            if (order == 0) {
                LOGGER.warn("ORDER attribute of <file> element is 0");
            }

            if (!pageUrls.containsValue(fileUrl)) {
                // file is not yet in list
                //
                pageUrls.put(order, fileUrl);
                // store page number and page name in a list
                String orderlabel = inDiv.getORDERLABEL();
                if (orderlabel != null) {
                    pageNames.put(order, orderlabel);
                }
            }
        }
    }

    /*************************************************************************************
     * Retrieves a FileType object for the appropriate div. The TileType object must be child of the a filegroup with a given value for its USE
     * attribute. If no value is given, it will return the first FileType object
     * 
     * @param inDiv the in div
     * @param filegroupuse the filegroupuse
     * 
     * @return the file for div
     * 
     * @throws MetsException
     ************************************************************************************/
    private FileType getFileForDIV(DivType inDiv, String filegroupuse) throws MetsException {
        LOGGER.debug("getFileForDIV: div with ID=" + inDiv.getID());

        List<Fptr> allfilepointers = inDiv.getFptrList();

        LOGGER.debug("getFileForDIV: " + allfilepointers.size() + " <fptr> elements found");

        // iterate over all file pointers
        for (Fptr filepointer : allfilepointers) {
            String fileid = filepointer.getFILEID(); // get the FILEID
            // attribute, which links to the <file> element

            // get the <file> element
            FileType file = getFILEbyID(fileid);
            LOGGER.debug("getFileForDIV: Referenced <file> with ID=" + fileid + " had been found");

            if (file == null) {
                // no <file> element available with this ID value
                LOGGER.error("getFileForDIV: No file element found with ID '" + fileid + "' in " + this.url);
                continue; // next iteration
            } else {
                // file found, need to check it's file group
                FileGrp filesfilegroup = getFileGroupForFile(file);
                LOGGER.debug("getFileForDIV: Checking file with ID=\"" + file.getID() + "\" in group " + filesfilegroup.getID()
                        + " with USE attribute set to " + filesfilegroup.getUSE());
                String useattr = filesfilegroup.getUSE(); // get use attribue

                if ((useattr != null) && (!useattr.equals("")) && (useattr.equals(filegroupuse))) {
                    // the use attribute is right, so we have the right
                    // file as it is stored in the right filegroup
                    LOGGER.debug("getFileForDIV: Found the right file in the right file group.");
                    return file;
                } else if (useattr == null) {
                    // no value for the USE attribute was given
                    LOGGER.debug("getFileForDIV: No value for filegroup's USE attribute given, return first FileType object");
                    return file;
                } else {
                    // it is NOT the right file, as it stored in a wrong file
                    // group
                    LOGGER.debug("getFileForDIV: File is stored in the wrong file group (" + useattr + ")");
                }
                // iterate over next fptr for this div
            } // end of iteration
        }
        return null;
    }

    /**************************************************************************************
     * Reads all fileGroups and adds them to some HashMaps which are used to cache information about the file-fileGrp relationship.
     * 
     * @throws MetsException the mets exception
     **************************************************************************************/
    private void readAllFileGroups() throws MetsException {
        // first search for the group
        String queryExpression = "//mets:fileGrp";

        List<XmlObject> objects = readQueryList(queryExpression);

        // iterate over objects
        for (XmlObject xObj : objects) {
            FileGrp result_group = null;
            try {
                result_group = (FileGrp) xObj;
            } catch (ClassCastException e) {
                // the found element is NOT a div
                // the METS is not according to standard
                LOGGER.error("Type of element is not a <fileGrp>.", e);
                throw new MetsException("Type of element is not a <fielGrp>.", e);
            }

            // store filegroups in HashMap with USE attribute value as a key
            String use = result_group.getUSE();
            if (use != null) {
                allFileGroups.put(use, result_group);
            }

            // get all the files for the group
            List<FileType> files = result_group.getFileList();
            for (FileType f : files) {
                String id = f.getID();
                if (id != null) {
                    allFiles.put(f, result_group); // add to HashMap
                }
            }
        }
    }

    /*************************************************************************************
     * Retrieves the filegroup under which the <file> is stored. The filegroup is always the direct parent
     * 
     * @param inFile the in file
     * 
     * @return the file group for file
     * 
     ************************************************************************************/
    private FileGrp getFileGroupForFile(FileType inFile) {
        FileGrp filegroup;
        boolean notcached = false;

        // file not in a cached group; delete
        if (allFiles.size() == 0) {
            // files aren't cached yet, so cache them
            try {
                readAllFileGroups();
            } catch (MetsException e) {
                notcached = true;
            }
        }

        if (!notcached) {
            filegroup = allFiles.get(inFile);
            if (filegroup != null) {
                return filegroup;
            }
        }

        // no in there, so try to find the file and
        // use the cursor to look up
        XmlCursor cursor = inFile.newCursor();
        cursor.toParent(); // cursor to parent, which should be the FileGroup
        XmlObject parentXbean = cursor.getObject();
        cursor.dispose(); // important: dispose the cursor

        filegroup = (FileGrp) parentXbean;
        return filegroup;
    }

    /**************************************************************************************
     * Retrieves all files belonging to a certain fileGrp and stores those files in a HashMap. The file's ID attribute value is the key for the
     * HashMap. This will allow easy access and check. The appropriate file groups is selected by its USE attribute value. If there are several
     * fileGrp elements with the same USE attribute values, files from all fileGrp's are added to the HashMap
     * 
     * @param usevalue the usevalue
     * 
     * @return the HashMap containing &lt;file&gt; elements.
     * 
     * @throws MetsException the mets exception
     *************************************************************************************/
    public Map<String, FileType> getFilesFromGroup(String usevalue) throws MetsException {

        HashMap<String, FileType> groupsfiles = new HashMap<String, FileType>();

        // first search for the group
        String queryExpression = "//mets:fileGrp[@USE='" + usevalue + "']";

        List<XmlObject> objects = readQueryList(queryExpression);

        // iterate over objects
        for (XmlObject xObj : objects) {
            FileGrp result_group = null;
            try {
                result_group = (FileGrp) xObj;
            } catch (ClassCastException e) {
                // the found element is NOT a div
                // the METS is not according to standard
                LOGGER.error("Type of element with USE=" + usevalue + " is not a <fileGrp>.", e);
                throw new MetsException("Type of element with USE=" + usevalue + " is not a <fielGrp>.", e);
            }
            // get all the files for the group
            List<FileType> files = result_group.getFileList();
            for (FileType f : files) {
                String id = f.getID();
                if (id != null) {
                    groupsfiles.put(id, f); // add to HashMap
                }
            }
        }

        return groupsfiles;
    }

    /**************************************************************************************
     * retrieves the parent of the DIV. Return null, if no &lt;div&gt; element is available as a parent; e.g. in case this is the uppermost
     * &lt;div&gt; in a structMap.
     * 
     * @param inDiv the in div
     * 
     * @return the parent div
     *************************************************************************************/
    public DivType getParentDiv(DivType inDiv) {
        DivType resultDiv = null;

        XmlCursor cursor = inDiv.newCursor();
        cursor.toParent(); // cursor to parent, which should be the FileGroup
        XmlObject parentXbean = cursor.getObject();
        cursor.dispose(); // important: dispose the cursor
        try {
            resultDiv = (DivType) parentXbean;
        } catch (Exception e) {
            LOGGER.debug("getParent has reached the uppermost div-Element.");
            return null;
        }
        return resultDiv;
    }

    /**************************************************************************************
     * get parent {@link DivType} of given {@link DivType}.
     * 
     * @param inDiv the given {@link DivType}
     * @param inType the given type as {@link String}
     * 
     * @return the requested parent {@link DivType}
     *************************************************************************************/
    public DivType getParentDiv(DivType inDiv, String inType) {
        DivType resultDiv = null;
        String type = null;

        DivType div = getParentDiv(inDiv);
        type = div.getTYPE();

        if (type == null) {
            // no type given, error in METS file
            LOGGER.warn("Warning: div-Element has no TYPE attribute!");
            return null;
        } else if (type.equals(inType)) {
            return div;
        } else {
            resultDiv = getParentDiv(div, inType);
        }

        return resultDiv;
    }

    /**************************************************************************************
     * reads all DIV elements which havea an ID attribute and cashes those elements in the divIDs hashMap.
     *************************************************************************************/
    private void readAllDIVwithID() {
        String queryExpression = "//mets:div[@ID]";

        List<XmlObject> objects = readQueryList(queryExpression);
        // iterate over objects
        for (XmlObject xObj : objects) {
            DivType div = (DivType) xObj;
            String id = div.getID();

            // add to HashMap
            divIDs.put(id, div);
        }
    }

    /**************************************************************************************
     * reads all FILE elements which have an ID attribute and cashes those elements in the fileIDs hashMap.
     *************************************************************************************/
    private void readAllFILEwithID() {
        String queryExpression = "//mets:file[@ID]";

        List<XmlObject> objects = readQueryList(queryExpression);
        // iterate over objects
        for (XmlObject xObj : objects) {
            FileType file = (FileType) xObj;
            String id = file.getID();
            // add to HashMap
            fileIDs.put(id, file);
        }
    }

    /**************************************************************************************
     * Read list of XML Objects returned by a query.
     * 
     * @param query the query
     * @return
     * 
     * @return the list< xml object>
     *************************************************************************************/
    protected List<XmlObject> readQueryList(String query) {
        String path = METS_NAMESPACEDECLARATION + " " + query;
        List<XmlObject> objects = Arrays.asList(mets.selectPath(path));

        LOGGER.trace("XPath retrieved " + objects.size() + " objects");
        LOGGER.trace("XPath was set to:\n" + path);

        return objects;
    }

    /*************************************************************************************
     * Retrieves the appropriate <div> element by ID.
     * 
     * @param dividvalue the dividvalue
     * 
     * @return the DI vby id
     * 
     * @throws MetsException
     * ***********************************************************************************/
    public DivType getDIVbyID(String dividvalue) throws MetsException {
        LOGGER.debug("get <div> element by id: ID=" + dividvalue);
        DivType result_div = null;

        if (divIDs.containsKey(dividvalue)) {
            // the divType element is cached
            result_div = divIDs.get(dividvalue);
            return result_div;
        }

        String queryExpression = "//mets:div[@ID='" + dividvalue + "']";

        XmlObject xObj = getXmlObjectFromIDQuery(queryExpression);

        try {
            result_div = (DivType) xObj;
        } catch (ClassCastException e) {
            // the found element is NOT a div
            // the METS is not according to standard
            LOGGER.error("Type of element with ID=" + dividvalue + " is not a <div>.", e);
            throw new MetsException("Type of element with ID=" + dividvalue + " is not a <div>.", e);
        }

        return result_div;
    }

    /*************************************************************************************
     * Gets the &lt;file&gt; element by its ID attribute.
     * 
     * @param fileidvalue the fileidvalue
     * 
     * @return the FIL eby id
     * 
     * @throws MetsException
     * ***********************************************************************************/
    private FileType getFILEbyID(String fileidvalue) throws MetsException {
        FileType result_file = null;

        if (fileIDs.containsKey(fileidvalue)) {
            // the divType element is cached
            result_file = fileIDs.get(fileidvalue);
            return result_file;
        }

        // do queries by iterating of all MatchingMetadata elements
        // String queryExpression =
        // "declare namespace
        // xq='http://xmlbeans.apache.org/samples/xquery/employees';" +
        // "$this/xq:employees/xq:employee/xq:phone[contains(., '(206)')]";

        String queryExpression = "//mets:file[@ID='" + fileidvalue + "']";

        XmlObject xObj = getXmlObjectFromIDQuery(queryExpression);

        try {
            result_file = (FileType) xObj;
        } catch (ClassCastException e) {
            LOGGER.error("Type of element with ID=" + fileidvalue + " is not a <file>.", e);
            throw new MetsException("Type of element with ID=" + fileidvalue + " is not a <file>.", e);
        }

        return result_file;
    }

    /*************************************************************************************
     * Retrieves any related &lt;div&gt; elements from the physical structure. It uses the structLink section to retrieve all ID attributes which are
     * linked to the &lt;div&gt;. The given &lt;div&gt; must be member of the logical structMap
     * 
     * @param inDiv value of ID attribute of &lt;div&gt;
     * 
     * @return the related divs for div
     * 
     * @throws MetsException
     ************************************************************************************/
    public LinkedList<DivType> getRelatedDivsForDiv(DivType inDiv) throws MetsException {
        LinkedList<DivType> resultlist = new LinkedList<DivType>();
        String dividvalue = inDiv.getID();
        LOGGER.debug("getRelatedDivsForDiv: get all related div elements for <div ID=\"" + dividvalue + "\">");

        // get all related ID from the appropriate smLink elements
        // the other div elements must be pages

        // do queries by iterating of all MatchingMetadata elements
        // String queryExpression =
        // "declare namespace
        // xq='http://xmlbeans.apache.org/samples/xquery/employees';" +
        // "$this/xq:employees/xq:employee/xq:phone[contains(., '(206)')]";

        String queryExpression = "//mets:smLink[@xlink:from='" + dividvalue + "']";
        String path = METS_NAMESPACEDECLARATION + " " + XLINK_NAMESPACEDECLARATION + " " + queryExpression;

        List<XmlObject> objects = Arrays.asList(mets.selectPath(path));

        // iterate over objects - there should only be a single
        // one as the ID attribute must contain a unique value

        LOGGER.debug("XPath retrieved " + objects.size() + " objects");
        LOGGER.debug("XPath was set to:\n" + path);

        for (XmlObject xObj : objects) {
            try {
                SmLink result_smLink = (SmLink) xObj;
                // get the xlink to attribute
                String to_id = result_smLink.getTo();

                DivType result_div = this.getDIVbyID(to_id); // get the div
                if (result_div == null) {
                    LOGGER.error("No <div> element with ID=\"to_id\" found");
                    throw new MetsException("No <div> element with ID=\"to_id\" found");
                }
                LOGGER.debug("Added div with ID=\"" + result_div.getID() + "\" and TYPE=\"" + result_div.getTYPE() + "\" to list");
                resultlist.add(result_div);

            } catch (ClassCastException e) {
                // the found element is NOT a div
                // the METS is not according to standard
                LOGGER.error("Type of element with ID=" + dividvalue + " is not a <div>.", e);
                throw new MetsException("Type of element with ID=" + dividvalue + " is not a <div>.", e);
            }
        } // end of for

        LOGGER.debug("");
        return resultlist;
    }

    /*************************************************************************************
     * retrieves the &lt;div&gt; of the start page.
     * 
     * @param divid of a &lt;div&gt; element from the logical structMap
     * 
     * @return DivType or null, if no page was found
     * 
     * @throws MetsException
     ************************************************************************************/
    public DivType getStartPageDiv(String divid) throws MetsException {
        // get Div by ID
        DivType mydiv = this.getDIVbyID(divid);
        // retrieves all pages for the given ID
        List<DivType> divList = getRelatedDivsForDiv(mydiv);
        Collections.sort(divList, new DivComparatorIncreasing());
        // iterate over all to find the first div, which is a page
        for (DivType div : divList) {
            if (div.getTYPE().equalsIgnoreCase("page")) {
                return div;
            }
        }
        return null;
    }

    /*************************************************************************************
     * retrieves the &lt;div&gt; of the end page.
     * 
     * @param divid of a &lt;div&gt; element from the logical structMap
     * 
     * @return DivType of null if no page was found
     * 
     * @throws MetsException
     ************************************************************************************/
    public DivType getEndPageDiv(String divid) throws MetsException {
        // get Div by ID
        DivType mydiv = this.getDIVbyID(divid);
        // retrieves all pages for the given ID
        LinkedList<DivType> divList = getRelatedDivsForDiv(mydiv);
        Collections.sort(divList, new DivComparatorDecreasing());
        // iterate over all to find the first div, which is a page
        for (DivType div : divList) {
            if (div.getTYPE().equalsIgnoreCase("page")) {
                return div;
            }
        }
        return null;
    }

    /*************************************************************************************
     * retrieves the start page number for a logical &lt;div&gt; element.
     * 
     * @param divid the divid
     * 
     * @return a String containing the page number, or NULL if no page was found
     * 
     * @throws MetsException
     ************************************************************************************/
    public String getStartpageNumber(String divid) throws MetsException {
        DivType div = getStartPageDiv(divid);
        if (div == null) {
            // no page found
            LOGGER.warn("getStartpageNumber: No page attached to logical <div ID=\"" + divid + "\">!");
            return "";
        }
        // found the page, retrieve page number
        // which is stored in ORDERLABEL
        String orderlabel = div.getORDERLABEL();
        LOGGER.debug("getStartpageNumber: " + orderlabel + " for <div ID=\"" + divid + "\">");
        if (orderlabel == null) {
            return "unknown";
        } else {
            return orderlabel;
        }
    }

    /*************************************************************************************
     * retrieves the end page number for a logical &lt;div&gt; element.
     * 
     * @param divid the divid
     * 
     * @return a String containing the page number, or NULL if no page was found
     * 
     * @throws MetsException
     ************************************************************************************/
    public String getEndpageNumber(String divid) throws MetsException {
        DivType div = getEndPageDiv(divid);
        if (div == null) {
            // no page found
            LOGGER.warn("getEndpageNumber: No page attached to logical <div ID=\"" + divid + "\">!");
            return "";
        }
        // found the page, retrieve page number
        // which is stored in ORDERLABEL
        String orderlabel = div.getORDERLABEL();
        LOGGER.debug("getStartEndNumber: " + orderlabel + " for <div ID=\"" + divid + "\">");
        if (orderlabel == null) {
            return "unknown";
        } else {
            return orderlabel;
        }
    }

    /*************************************************************************************
     * Fills the rootStructureList with a Structure, which is retrieved from the logical structMap of the METS file.
     * 
     * @param divid the divid
     * 
     * @throws MetsException
     ************************************************************************************/
    private void createStructureList(String divid) throws MetsException {
        SimpleStructure struct = createStructure(divid, 0);
        if (struct != null) {
            rootStructureList = new LinkedList<SimpleStructure>();
            rootStructureList.add(struct);
        }
    }

    /*************************************************************************************
     * Creates a Structure; A Structure does not only create the pagenumber and content, but also a list (optional) of children's Structure. This list
     * is created as well.
     * 
     * @param divid the divid
     * @param iteration the iteration
     * @return the simple structure
     * @throws MetsException
     ************************************************************************************/
    private SimpleStructure createStructure(String divid, int iteration) throws MetsException {
        int pagename = 0;
        String content = "";

        // retrieve DIV
        DivType div = this.getDIVbyID(divid);

        // retrieve the pagename of the start page for this <div>
        DivType startpage = this.getStartPageDiv(div.getID());

        if ((startpage == null) && (iteration != 0)) {
            // it is not an anchor
            LOGGER.warn("<div ID=" + divid + " doesn't have a startpage, but should have one!");
            return null;
        } else if (startpage == null) {
            // it is an anchor, so we are NOT creating a structure for it
            LOGGER.debug("<div ID=" + divid + " doesn't have a startpage, but it seems it is an anchor anyhow!");
            pagename = 1; // set structure to first page
        } else {
            String pagename_str = startpage.getORDER().toString();

            try {
                pagename = Integer.parseInt(pagename_str);
            } catch (NumberFormatException e) {
                LOGGER.error("createStructure: Invalid ORDER value (" + pagename_str + ") for start page of logical <div ID=\"divid\">", e);
                throw new MetsException("createStructure: Invalid ORDER value (" + pagename_str + ") for start page of logical <div ID=\"divid\">", e);
            }
        }

        // retrieve the content for this div
        if (sme != null) {
            content = sme.getStructureMetadata(div, this);
        } else {
            // no StructureMetadataExtractor set
            content = "Structure - check configuration";
            LOGGER.warn("No StructureMetadataExtractor set; can't create meaningful bookmark for PDF");
        }

        // this div is the first Structure
        SimpleStructure struct = new SimpleStructure();
        struct.setImageNumber(pagename);
        struct.setContent(content);

        // get all children, create Structure for them
        // and add them to a new List of Structures
        List<DivType> children = div.getDivList();
        if ((children != null) && (children.size() > 0)) {
            // create new Structure List
            List<SimpleStructure> childrenslist = new LinkedList<SimpleStructure>();

            for (DivType divTemp : children) {
                SimpleStructure childStruct = createStructure(divTemp.getID(), iteration + 1);
                childrenslist.add(childStruct);
            }

            // add list to Structure
            struct.setChildren(childrenslist);
        }
        return struct;
    }

    /*************************************************************************************
     * Set the class which is responsible to extract the metadata from a &gt;div&gt; to be put into a Structure's content
     * 
     * @param sme the StructureMetadataExtractor to set
     * ***********************************************************************************/
    public void setStructureMetadataExtractor(StructureMetadataExtractor sme) {
        this.sme = sme;
    }

    // TODO: GDZ: Change this, there shouldn't be any dependecies to iText or
    // ContentLib in this Project
    /*************************************************************************************
     * .
     * 
     * @return the pageUrls
     * ***********************************************************************************/
    @Override
    public Map<Integer, UrlImage> getImageMap() {
        Map<Integer, UrlImage> allPages = new HashMap<Integer, UrlImage>();
        // iterate over the keyset = pagenumbers
        for (Integer pagenumber : pageUrls.keySet()) {
            UrlImage page = new SimpleUrlImage();
            page.setURL(pageUrls.get(pagenumber));
            allPages.put(pagenumber, page);
        }
        return allPages;
    }

    /*************************************************************************************
     * .
     * 
     * @return the pageNames
     ************************************************************************************/
    public Map<Integer, String> getPageNames() {
        return pageNames;
    }

    /*************************************************************************************
     * .
     * 
     * @return the rootStructureList
     * ***********************************************************************************/
    @Override
    public List<? extends Structure> getStructureList() {
        return rootStructureList;
    }

    /*************************************************************************************
     * Sets the value of the filegroup's USE attribute which files are used for PDF generation.
     * 
     * @param filegroupsuseattributevalue the filegroupsuseattributevalue to set
     ************************************************************************************/
    public void setFilegroupsuseattributevalue(String filegroupsuseattributevalue) {
        this.filegroupsuseattributevalue = filegroupsuseattributevalue;
    }

    /*************************************************************************************
     * .
     * 
     * @param metadataextractor the metadataextractor to set
     ************************************************************************************/
    public void setMetadataextractor(MetadataExtractor metadataextractor) {
        this.metadataextractor = metadataextractor;
    }

    // TODO: Move this to the tests
    /*************************************************************************************
     * This is just a testclass which outputs all parsed metadata for the PDF as well as for the cover page.
     * 
     * @param dividvalue the dividvalue
     * 
     * @throws MetsException
     ************************************************************************************/
    public void metadataTest(String dividvalue) throws MetsException {
        if (metadataextractor == null) {
            LOGGER.error("No metadata extractor class defined!");
            return;
        }
        LOGGER.debug("Extracting metadata from METS file:");
        DivType div = this.getDIVbyID(dividvalue);

        metadataextractor.calculateMetadata(div, this);
        LOGGER.debug("title: " + metadataextractor.getPdftitle());
        LOGGER.debug("creator:" + metadataextractor.getPdfcreator());
        LOGGER.debug("keywords:" + metadataextractor.getPdfkeywords());
    }

    /**************************************************************************************
     * Gets the url.
     * 
     * @return the url of the METSfile
     *************************************************************************************/
    @Override
    public URL getUrl() {
        return url;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.unigoettingen.commons.util.datasource.DataSource#close()
     */
    @Override
    public void close() throws IOException {
        // Neded for the DataSource interface
    }

    /**
     * Gets the number of pages.
     * 
     * @return the number of pages
     */
    @Override
    public Integer getNumberOfPages() {
        return pageUrls.size();
    }

    /**
     * Gets the image list.
     * 
     * @return the image list
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    // TODO: Review
    @Override
    public List<UrlImage> getImageList() throws IOException {
        List<UrlImage> imgList = new ArrayList<UrlImage>();
        for (Integer i : pageUrls.keySet()) {
            UrlImage page = new SimpleUrlImage();
            page.setURL(pageUrls.get(i));
            imgList.add(page);
        }

        return imgList;
    }

    // TODO: Review
    @Override
    public UrlImage getImage(Integer pageNr) throws IOException {
        UrlImage page = new SimpleUrlImage();
        page.setURL(pageUrls.get(pageNr));
        return page;
    }

}
