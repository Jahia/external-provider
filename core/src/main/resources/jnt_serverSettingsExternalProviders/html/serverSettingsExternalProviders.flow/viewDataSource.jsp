<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<h2><fmt:message key="label.dataSourceInfo"/></h2>

<h3><fmt:message key="label.capabilities"/></h3>
<ul>
    <li> <fmt:message key="label.canWrite"/> : <c:if test="${datasourceInfo.writeable}"><span class="label label-success"><fmt:message
            key="label.yes"/></span></c:if><c:if test="${not datasourceInfo.writeable}"><span
            class="label label-warning"><fmt:message key="label.no"/></span></c:if></li>
    <li> <fmt:message key="label.canSearch"/> : <c:if test="${datasourceInfo.searchable}"><span class="label label-success"><fmt:message
            key="label.yes"/></span></c:if><c:if test="${not datasourceInfo.searchable}"><span
            class="label label-warning"><fmt:message key="label.no"/></span></c:if></li>
    <li> <fmt:message key="label.supportsUuid"/> : <c:if test="${datasourceInfo.supportsUuid}"><span class="label label-success"><fmt:message
            key="label.yes"/></span></c:if><c:if test="${not datasourceInfo.supportsUuid}"><span
            class="label label-warning"><fmt:message key="label.no"/></span></c:if></li>
    <li> <fmt:message key="label.canBeExtended"/> : <c:if test="${datasourceInfo.extendable}"><span class="label label-success"><fmt:message
            key="label.yes"/></span></c:if><c:if test="${not datasourceInfo.extendable}"><span
            class="label label-warning"><fmt:message key="label.no"/></span></c:if></li>
</ul>

<h3><fmt:message key="label.nodeInfos"/></h3>
<ul>
    <li><fmt:message key="label.supportedTypes"/> : ${datasourceInfo.supportedTypes}</li>
    <li><fmt:message key="label.rootNodeType"/> : ${datasourceInfo.rootNodeType}</li>
</ul>

<c:if test="${datasourceInfo.extendable}">
    <h3><fmt:message key="label.extensions"/></h3>
    <ul>
        <li><fmt:message key="label.overridableItems"/> : ${datasourceInfo.overridableItems}</li>
        <li><fmt:message key="label.extendableTypes"/>s : ${datasourceInfo.extendableTypes}</li>
    </ul>
</c:if>
<c:if test="${datasourceInfo.searchable}">
    <h3><fmt:message key="label.searchCapabilities"/></h3>
    <ul>
        <c:forEach items="${datasourceInfo.supportedQueries}" var="entry">
            <li>${entry.key} : <c:if test="${entry.value}"><span class="label label-success"><fmt:message
                    key="label.yes"/></span></c:if><c:if test="${not entry.value}"><span
                    class="label label-warning"><fmt:message key="label.no"/></span></c:if></li>
        </c:forEach>
    </ul>
</c:if>
