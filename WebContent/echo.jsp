<%@ page language="java" contentType="text/html; charset=UTF8"
    pageEncoding="UTF8"%>
<%@ page import="java.util.Enumeration"%>

<%
    request.setAttribute("page", "echo");
%>


<%@page import="java.util.Collection"%><jsp:include page="inc/header.jsp" />

<div id="main">
<%
    Enumeration<String> errors = request.getAttributeNames();
    while (errors.hasMoreElements()) {
        String label = (String) errors.nextElement();
        Object value = request.getAttribute(label);
        if (label.equals("error")) {
            out.print("<p style=\"color:red;font-weight:bold\">" + value + "</p>");
        }
    }
%>

<h2 id="echo">Action: Echo</h2>

<p>At this place are all parameters listed which were given via GET or POST request.</p>

<h2 id="table">Parameter</h2>

<table>
    <tr>
        <th>type</th>
        <th>content</th>
    </tr>

    <%
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String label = (String) params.nextElement();
            Object value = request.getParameter(label);
            out.print("<tr class=\"row-b\"><td>" + label + "</td><td>" + value + "</td></tr>");
        }
    %>

</table>

<!-- main ends --></div>

<div id="sidebar">

<h3>Content</h3>
<ul class="sidemenu">
    <li><a href="#echo">Action: Echo</a></li>
    <li><a href="#table">Parameter</a></li>
</ul>

<!-- sidebar ends --></div>

<jsp:include page="inc/footer.jsp" />
