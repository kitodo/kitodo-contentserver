<%@ page language="java" contentType="text/html; charset=UTF8"
    pageEncoding="UTF8"%>
    
<%
   request.setAttribute("page","about");
%>

<jsp:include page="inc/header.jsp" />

<div id="main">

<h2 id="about">About ContentServer - version 2</h2>

<p>The GoobiContentServer analyses METS files via simple HTTP requests per GET or POST and processes their images. 
    The whole GoobiContentServer API was programmed by <a
	href="http://www.intranda.com">intranda software</a> on behalf of the <a
	href="http://gdz.sub.uni-goettingen.de">Center for Retrospective Digitization, Göttingen (GDZ)</a>.
	</p>
<p>A detailed description on how to use the GoobiContentServer can be found under <a
	href="help.jsp">Help</a>.</p>

<!-- main ends --></div>

<div id="sidebar">

<h3>Content</h3>
<ul class="sidemenu">
	<li><a href="#about">About GoobiContentServer - version 1</a></li>
</ul>

<!-- sidebar ends --></div>

<jsp:include page="inc/footer.jsp" />