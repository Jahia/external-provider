<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<h2>Country</h2>
<ul>

    <li>
        ISO = ${currentNode.properties['country_iso_code'].string}
    </li>
    <li>
        Name : ${currentNode.properties['country'].string}
    </li>
    <li>
        Region : ${currentNode.properties['region'].string}
    </li>

    <li>
        Cities :

        <jcr:jqom var="rs"
                  statement="select * from [jtestnt:city] where country_iso_code='${currentNode.name}'"/>

        <ul>
            <c:forEach items="${rs.nodes}" var="city">
                <li>
                    <c:url value="${url.base}${fn:replace(city.path,'#','%23')}.html" var="link" />
                    <a href="${link}">${city.properties['city_name'].string}</a>
                </li>
            </c:forEach>
        </ul>
    </li>
</ul>