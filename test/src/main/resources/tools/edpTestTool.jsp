<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.jahia.modules.external.ExternalContentStoreProvider" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.jahia.modules.external.test.activators.ExternalContentStoreProviderActivator" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    ExternalContentStoreProvider tatatititotoProvider = BundleUtils.getOsgiService(ExternalContentStoreProviderActivator.class, null).getExternalContentStoreProvider("tatatititoto");

    if (StringUtils.equals(request.getParameter("action"), "start") && StringUtils.isNotBlank(request.getParameter("path"))) {
        tatatititotoProvider.setMountPoint(request.getParameter("path") + "/tatatititoto");
        tatatititotoProvider.start();
    } else if (StringUtils.equals(request.getParameter("action"), "stop")) {
        tatatititotoProvider.stop();
    }

    pageContext.setAttribute("provider", tatatititotoProvider.isInitialized());
%>

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <style type="text/css" title="currentStyle">
        @import "<c:url value="/modules/tools/css/demo_page.css"/>";
        @import "<c:url value="/modules/tools/css/demo_table_jui.css"/>";
    </style>
    <script type="text/javascript" src="<c:url value="/modules/jquery/javascript/jquery.js"/>"></script>
    <script type="text/javascript"
            src="<c:url value="/modules/assets/javascript/datatables/jquery.dataTables.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/modules/assets/javascript/jquery.cuteTime.js"/>"></script>

    <title>External data provider - test provider</title>
</head>

<body>
<h1>External data provider - test provider</h1>
<p>
    This tool is used to mount a test provider, for manual testing purpose<br>
    the provider contains some bigText nodes that can be used in DX sites.
</p>

<c:if test="${not empty provider and provider}">

</c:if>

<form id="form" method="post">
<c:choose>
    <c:when test="${not empty provider and provider}">
        <button type="submit">Unmount</button>
        <input type="hidden" name="action" value="stop"/>
    </c:when>
    <c:otherwise>
        path: <input type="text" name="path" placeholder="/tools"/>
        <button type="submit">Mount</button>
        <input type="hidden" name="action" value="start"/>
    </c:otherwise>
</c:choose>

<p>
    <img src="<c:url value='/engines/images/icons/home_on.gif'/>" height="16" width="16" alt=" " align="top"/>&nbsp;
    <a href="<c:url value='/modules/tools/index.jsp'/>">to Support Tools overview</a>
</p>
</body>

</html>
