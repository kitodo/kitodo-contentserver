<%@ page language="java" contentType="text/html; charset=UTF8"
	pageEncoding="UTF8"%>

<%
	request.setAttribute("page", "help");

	String completePath = request.getRequestURL().toString();
	int lastSlash = completePath.lastIndexOf("/");
	String lastPart = completePath.substring(0, lastSlash + 1);
	//out.print(lastPart);
%> 
<jsp:include page="inc/header.jsp" />

<div id="main">

<h2>How to use the GoobiContentServer</h2>

<p>At this place you can find descriptions for all parameters that are needed to control the
GoobiContentServer.</p>
<p>The GoobiContentServer is reachable under the following address <b>"<%
	out.print(lastPart + "gcs");
%>"</b>, where automatically the Action "echo" gets called.</p>

<h3 id="echo">echo</h3>
<p>By requesting the URL <b>"<%
	out.print(lastPart + "gcs?action=echo");
%>"</b> the action "echo" gets executed. As a result you get an overview of all request parameters which were used in the request.</p>

<h3 id="help">help</h3>
<p>By requesting the URL  <b>"<%
	out.print(lastPart + "gcs?action=help");
%>"</b> you get this help page with descriptions for all parameters on how to control the GoobiContentServer.</p>

<h3 id="about">about</h3>
<p>By requesting the URL  <b>"<%
	out.print(lastPart + "cs?action=about");
%>"</b> you will get a page with some basic information about the GoobiContentServer project.</p>

<h3 id="pdf">pdf</h3>
<p>By calling the action "pdf" you receive one pdf file on basis of a given mets file, including page names, bookmarks, watermarks and title pages. 
Simply call the following URL: <b>"<%
	out.print(lastPart + "gcs?action=pdf");
%>"</b> and combine it with some parameters to generate your pdf file. The following URL is an example on how you can process one mets file and generate one pdf file from it: <br />
<br />
<a
	href="<%
	out.print(lastPart); %>gcs?action=pdf&metsFile=FaustTragedy.xml&targetFileName=Goethe_Faust_FirstPart.pdf">
<%
	out.print(lastPart); %>gcs?action=pdf&metsFile=FaustTragedy.xml&targetFileName=Goethe_Faust_FirstPart.pdf</a></p>

<table>
	<tr>
		<th>Command</th>
		<th>Description</th>
	</tr>
	<tr>
        <td>metsFile</td>
        <td>Define the name of the METS file. Use here only the relative path coming from the defined repository.<br />
        sample: "metsFile=FaustTragedy.xml"</td>
    </tr>
	<tr>
        <td>targetFileName</td>
        <td>define the name of the target pdf file<br />
        sample: "targetFileName=Goethe_Faust_FirstPart.pdf"</td>
    </tr>
    <tr>
        <td>divID</td>
        <td>define the mets div by id which should be generated as pdf file<br />
        sample: "divID=log9"</td>
    </tr>
    <tr>
        <td>writeAsPdfA</td>
        <td>define if the pdf should be written as Pdf/A file<br />
        sample: "writeAsPdfA=false"</td>
    </tr>
    <tr>
        <td>metsFileGroup</td>
        <td>define which metsFileGroup should be used<br />
        sample: "metsFileGroup=PRESENTATION"</td>
    </tr>
    <tr>
        <td>pagesize</td>
        <td>define which pagesize should be used ('A4', 'original', 'A4Box <br />
        sample: "pagesize=A4"</td>
    </tr>
    <tr>
        <td>pdftitlepage</td>
        <td>if the content of the metsfile should not be parsed automatically you can define here a title page for the PDF-file. Its content will be used directly without any change. Be sure to use an url as parameter here.<br />
        sample: "pdftitlepage=http://intranda.com/GDZ/samplepdftitlepage.xml"</td>
    </tr>    
    <tr>
		<td>ignoreCache</td>
		<td>define here if a pdf file from cache should be ignored<br />
		sample: "ignoreCache=true"</td>
	</tr>
</table>

<h3 id="metsImage">metsImage</h3>
<p>By calling the action "metsImage" you receive one image file from a given mets file with the given divID which can be processed by the embedded contentSever image library. 
Simply call the following URL: <b>"<%
	out.print(lastPart + "gcs?action=metsImage");
%>"</b> and combine it with some parameters to process the requested image file. The following URL is an example on how you can process one image from a mets file: <br />
<br />
<a
	href="<%
	out.print(lastPart); %>gcs?action=metsImage&metsFile=FaustTragedy.xml&divID=phys2&scale=30&rotate=90&format=jpeg&resolution=200">
<%
	out.print(lastPart); %>gcs?action=metsImage&metsFile=FaustTragedy.xml&divID=phys2&scale=30&rotate=90&format=jpeg&resolution=200</a></p>

<table>
	<tr>
		<th>Command</th>
		<th>Description</th>
	</tr>
	<tr>
        <td>metsFile</td>
        <td>Define the name of the METS file. Use here only the relative path coming from the defined repository.<br />
        sample: "metsFile=FaustTragedy.xml"</td>
    </tr>
    <tr>
        <td>divID</td>
        <td>define the mets div id of the requested image which should be processed, the id have to be a physical one<br />
        sample: "divID=phys2"</td>
    </tr>
    <tr>
        <td>metsFileGroup</td>
        <td>define which metsFileGroup should be used; the image is taken from this file group; if not defined, the value from configuration file will be used<br />
        sample: "metsFileGroup=PRESENTATION"</td>
    </tr>

	<tr>
		<td>rotate</td>
		<td>value of the rotation angle as number between 0 and 360<br />
        sample: "rotate=90"</td>
	</tr>
	<tr>
        <td>scale</td>
        <td>value of zoom factor (for example: 50 represents 50% and 100
        represents 100%)<br />
        sample: "scale=50"</td>
    </tr>
    <tr>
        <td>width</td>
        <td>pixel for zoom to fixed width (for example: 800 represents 800px width)<br />
        sample: "width=800"</td>
    </tr>
    <tr>
		<td>height</td>
		<td>pixel for zoom to fixed height (for example: 1000 represents 1000px height)<br />
        sample: "height=1000"</td>
	</tr>
	<tr>
		<td>format</td>
		<td>format of target file, possible formats are: jpeg $ png $ jp2
		$ tiff</td>
	</tr>
	<tr>
		<td>resolution</td>
		<td>define resolution of target image in dpi (default is 600 dpi)</td>
	</tr>
	<tr>
		<td>highlight</td>
		<td>define coordinates for highlighting inside the image; define
		4 coordinates for each highlighting area; for multiple highlighting
		areas separate the coordinates of each area using a dollar sign "$"<br />
		sample for one area: "highlight=10,50,80,150"<br />
		sample for two areas: "highlight=10,50,80,150$60,80,160,200"</td>
	</tr>
	<tr>
		<td>targetFileName</td>
		<td>define the name of the target image file<br />
		sample: "targetFileName=myGeneratedImage.jpg"</td>
	</tr>
	<tr>
		<td>errorReport</td>
		<td>format of the error report; possible values "image" and
		"jsp"; if parameter is not defined "jsp" is taken<br />
		sample: "errorReport=image"</td>
	</tr>

</table>

<h3 id="multiPdf">multiPdf</h3>
<p>By calling the action "multiPdf" you receive one pdf file from a given list of mets and pdf files. 
Simply call the following URL: <b>"<%
    out.print(lastPart + "gcs?action=multiPdf");
%>"</b> and combine it with some parameters to process the given files. The following URL is an example on how you can process one mets file and one pdf file into one merged pdf file: <br />
<br />
<a
    href="<%
    out.print(lastPart); %>gcs?action=multiPdf&files=mets$someMetsFile.xml$$pdf$somePdfFile.pdf&metsFileGroup=DEFAULT">
<%
    out.print(lastPart); %>gcs?action=multiPdf&files=mets$someMetsFile.xml$$pdf$somePdfFile.pdf&metsFileGroup=DEFAULT</a></p>

<table>
    <tr>
        <th>Command</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>files</td>
        <td>Define the list of files to process into one large pdf file. Each file section is devided by a double dollar sign '$$' from each other and consists of two or three parameters devided by a single dollar sign '$'.
            The parameters are: filetype, url and divid. The filetype and the url parameter always have to be present. The divid parameter is optional and only valid for metsfiles.<br/>
            As filetype you can use 'pdf' and 'mets'.<br/>
            The url parameter defines the name of the PDF or METS file. Use here only the relative path coming from the defined repository.<br />
            As divid you can insert a physical mets div id.<br />
            sample: "files=mets$someMetsFile.xml$$pdf$somePdfFile.pdf$$mets$someMetsFileWithDivId.xml$phys2"</td>
    </tr>
    <tr>
        <td>metsFileGroup</td>
        <td>define which metsFileGroup should be used; the image is taken from this file group; if not defined, the value from configuration file will be used<br />
        sample: "metsFileGroup=PRESENTATION"</td>
    </tr>
    <tr>
        <td>targetFileName</td>
        <td>define the name of the target image file<br />
        sample: "targetFileName=myGeneratedImage.jpg"</td>
    </tr>
    <tr>
        <td>writeAsPdfA</td>
        <td>define if the pdf should be written as Pdf/A file<br />
        sample: "writeAsPdfA=false"</td>
    </tr>
    <tr>
        <td>pagesize</td>
        <td>define which pagesize should be used ('A4', 'original', 'A4Box <br />
        sample: "pagesize=A4"</td>
    </tr>
</table>
<!-- main ends --></div>

<div id="sidebar">

<h3>Content</h3>
<ul class="sidemenu">
	<li><a href="#echo">echo</a></li>
	<li><a href="#help">help</a></li>
	<li><a href="#about">about</a></li>
	<li><a href="#pdf">pdf</a></li>
	<li><a href="#metsImage">metsImage</a></li>
	<li><a href="#multiPdf">multiPdf</a></li>
</ul>

<!-- sidebar ends --></div>

<jsp:include page="inc/footer.jsp" />
