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
<h2>Flight</h2>

<ul>

    <li>
        Flight id : ${currentNode.properties['flight_id'].string}
    </li>
    <li>
        Aircraft : ${currentNode.properties['aircraft'].string}
    </li>
    <li>
        Origin : ${currentNode.properties['orig_airport'].string}

        <jcr:jqom var="rs"
                  statement="select * from [jtestnt:city] where airport='${currentNode.properties['orig_airport'].string}'"/>

        <c:forEach items="${rs.nodes}" var="city">
             - City = <a href="<c:url value='${url.base}${city.path}.html'/>">${city.properties['city_id'].string} , ${city.properties['city_name'].string}</a>
        </c:forEach>

    </li>
    <li>
        Destination : ${currentNode.properties['dest_airport'].string}

        <jcr:jqom var="rs"
                  statement="select * from [jtestnt:city] where airport='${currentNode.properties['dest_airport'].string}'"/>

        <c:forEach items="${rs.nodes}" var="city">
            - City = <a href="<c:url value='${url.base}${city.path}.html'/>">${city.properties['city_id'].string} , ${city.properties['city_name'].string}</a>
        </c:forEach>
    </li>
</ul>
