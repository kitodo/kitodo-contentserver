<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>GoobiContentServer</title>
<link rel="stylesheet" href="images/TechJunkie.css" type="text/css" />
</head>

<body>
<!-- wrap starts here -->
<div id="wrap"><!--header -->
<div id="header">

<h1 id="logo-text"><a href="index.jsp" title="">GoobiContentServer</a></h1>
<p id="slogan">version 1</p>

<!--header ends--></div>

<!-- navigation starts-->
<div id="nav">
<ul>
	<li
		<%out.println(request.getAttribute("page").equals("echo") ? " id=\"current\"" : "");%>><a
		href="echo.jsp">Echo</a></li>
	<li
		<%out.println(request.getAttribute("page").equals("help") ? " id=\"current\"" : "");%>><a
		href="help.jsp">Help</a></li>
	<li
		<%out.println(request.getAttribute("page").equals("helpCs") ? " id=\"current\"" : "");%>><a
		href="helpCs.jsp">Help CS</a></li>
	<li
		<%out.println(request.getAttribute("page").equals("about") ? " id=\"current\"" : "");%>><a
		href="about.jsp">About</a></li>
</ul>
<!-- navigation ends--></div>

<!-- content-wrap starts -->
<div id="content-wrap">
