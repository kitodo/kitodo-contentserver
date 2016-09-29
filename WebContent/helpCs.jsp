<%@ page language="java" contentType="text/html; charset=UTF8"
	pageEncoding="UTF8"%>

<%
	request.setAttribute("page", "helpCs");

	String completePath = request.getRequestURL().toString();
	int lastSlash = completePath.lastIndexOf("/");
	String lastPart = completePath.substring(0, lastSlash + 1);
	//out.print(lastPart);
	// http://localhost:8080/gcs/cs/cs?action=image&sourcepath=00000001.tif&scale=30&rotate=90&format=jpeg&resolution=200&
%> 
<jsp:include page="inc/header.jsp" />

<div id="main">

<h2>How to use the embedded ContentServer</h2>

<p>At this place you can find descriptions for the embedded ContentServer which can be used as standalone servlet or embedded in the
GoobiContentServer.</p>
<p>The embedded ContentServer is reachable under the following address <b>"<%
	out.print(lastPart + "cs");
%>"</b>, where automatically the Action "echo" gets called.</p>

<h3 id="image">image</h3>
<p>By using the action "image" you can process some image
manipulations like converting, scaling, rotating, etc. Simply call the
URL: <b>"<%
	out.print(lastPart + "cs?action=image");
%>"</b> and combine it with the necessary parameters. The following URL
shows as example, how to convert a tif image into a jpeg image, scale it
to 30% of its size, rotate it 90° and add two coloured highlighting
areas: <br />
<br />
<a
	href="<%out.print(lastPart + "cs?action=image");%>&sourcepath=00000001.tif&scale=30&rotate=0&format=jpeg&resolution=200&highlight=10,50,80,150$60,80,160,200">
<%
	out.print(lastPart + "cs?action=image");
%>&sourcepath=00000001.tif&scale=30&rotate=0&format=jpeg&resolution=200&highlight=10,50,80,150$60,80,160,200</a></p>

<table>
	<tr>
		<th>Command</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>sourcepath</td>
		<td>path and name of source image (repository path is configured
		for ContentServer)</td>
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


<h3 id="pdf">pdf</h3>
<p>By calling the action "pdf" you can merge multiple image files
together with some general metadata and bookmarks to one pdf file.
Simply call the following URL: <b>"<%
	out.print(lastPart + "cs?action=pdf");
%>"</b> and combine it with some parameters to generate your pdf file. The
following URL is an example on how you can process three images together
with some author and title metadata and some bookmarks to generate one
pdf file: <br />
<br />
<a
	href="<%out.print(lastPart);%>cs?action=pdf&images=00000001.tif$00000002.tif$00000003.tif$00000004.tif$00000005.tif$00000006.tif$00000007.tif$00000008.tif&imageNames=I$II$unnamed$1$2$3$4$5&bookmarks=1,0,0,Frontpage$2,0,1,First Act$3,2,1,Chapter One$4,2,2,Chapter Two$5,0,5,Second Act$6,5,6,Chapter Three$7,5,7,Chapter Four&metadataAuthor=Johann Wolfgang von Goethe&metadataTitle=Faust: The First Part of the Tragedy&metadataCreator=Mr. Goethes Hidden Creator&metadataSubject=classical literature&metadataKeyword=faust, first part&targetFileName=Goethe_Faust_FirstPart.pdf">
<%
	out.print(lastPart);
%>cs?action=pdf&images=00000001.tif$00000002.tif$00000003.tif$00000004.tif$00000005.tif$00000006.tif$00000007.tif$00000008.tif&imageNames=I$II$unnamed$1$2$3$4$5&bookmarks=1,0,0,Frontpage$2,0,1,First
Act$3,2,1,Chapter One$4,2,2,Chapter Two$5,0,5,Second Act$6,5,6,Chapter
Three$7,5,7,Chapter Four&metadataAuthor=Johann Wolfgang von
Goethe&metadataTitle=Faust: The First Part of the
Tragedy&metadataCreator=Mr. Goethes Hidden
Creator&metadataSubject=classical literature&metadataKeyword=faust,
first part&targetFileName=Goethe_Faust_FirstPart.pdf</a></p>

<table>
	<tr>
		<th>Command</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>images</td>
		<td>define the list of images to be transformed to one pdf file
		by using the path and name of each image file (repository path is
		configured for ContentServer) and separate all files by a dollar sign "$" <br />
		sample for three images:
		"images=00000001.tif$00000002.tif$00000003.tif"</td>
	</tr>
	<tr>
		<td>imageNames</td>
		<td>define the list of names for all images to be set for the pdf
		file by using the name for each image file of the defined images and
		separate all names by a dollar sign "$" <br />
		sample for three image names: "imageNames=I$a$3"</td>
	</tr>
	<tr>
		<td>bookmarks</td>
		<td>define the bookmarks as list using dollar signs "$" as separator.
		Each bookmark consists of four properties which are separated by a
		comma: "id, parentId, index of image in image list starting by 0,
		title of bookmark". By choosing 0 as parentId, the bookmark is set to
		the top level. Each ID has to be unique. All referenced images index
		numbers have to exist in the list of images. <br />
		sample for four bookmarks: "bookmarks=1,0,0,Frontpage$2,0,1,"First
		Act$3,1,1,Chapter One$4,1,14,Chapter Two"</td>
	</tr>
	<tr>
		<td>metadataAuthor</td>
		<td>define the metadata for the author<br />
		sample: "metadataAuthor=Johann Wolfgang von Goethe"</td>
	</tr>
	<tr>
		<td>metadataCreator</td>
		<td>define the metadata for the creator<br />
		sample: "metadataCreator=Mr. Goethes Hidden Creator"</td>
	</tr>
	<tr>
		<td>metadataTitle</td>
		<td>define the metadata for the title<br />
		sample: "metadataTitle=Faust: The First Part of the Tragedy"</td>
	</tr>
	<tr>
		<td>metadataSubject</td>
		<td>define the metadata for the subject<br />
		sample: "metadataSubject=classical literature"</td>
	</tr>
	<tr>
		<td>metadataKeyword</td>
		<td>define the metadata for the keyword<br />
		sample: "metadataKeyword=faust, first part"</td>
	</tr>
	<tr>
		<td>targetFileName</td>
		<td>define the name of the target pdf file<br />
		sample: "targetFileName=Goethe_Faust_FirstPart.pdf"</td>
	</tr>
    <tr>
        <td>alwaysUseRenderedImage</td>
        <td>convert all images always to rendered image before sending it to iText; 
        if parameter is not defined the value ist taken from config file<br />
        sample: "alwaysUseRenderedImage:false"</td>
    </tr>
    <tr>
		<td>alwaysCompressToJPEG</td>
		<td>compress all images always to jpeg image before sending it to iText; 
		if parameter is not defined the value ist taken from config file<br />
		sample: "alwaysCompressToJPEG:false"</td>
	</tr>
	<tr>
        <td>errorReport</td>
        <td>format of the error report; possible values "image" and
        "jsp"; if parameter is not defined "jsp" is taken<br />
        sample: "errorReport:image"</td>
    </tr>

</table>

<!-- main ends --></div>

<div id="sidebar">

<h3>Content</h3>
<ul class="sidemenu">
	<li><a href="#image">image</a></li>
	<li><a href="#pdf">pdf</a></li>
</ul>

<!-- sidebar ends --></div>

<jsp:include page="inc/footer.jsp" />
