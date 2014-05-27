<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
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

<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,admin-bootstrap.js,bootstrap-filestyle.min.js,jquery.metadata.js,jquery.tablesorter.js,jquery.tablecloth.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tablecloth.css"/>
<template:addResources type="javascript" resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js"/>

<script type="text/javascript" charset="utf-8">
    $(document).ready(function() {
        var providersTable = $('#providersTable');

        providersTable.dataTable({
            "sDom": "<'row-fluid'<'span6'l><'span6 text-right'f>r>t<'row-fluid'<'span6'i><'span6 text-right'p>>",
            "iDisplayLength": 10,
            "sPaginationType": "bootstrap",
            "aaSorting": [] //this option disable sort by default, the user steal can use column names to sort the table
        });
    });
</script>


<h2><fmt:message key="label.externalProviderAdmin"/></h2>
<table id="providersTable" class="table table-bordered table-striped table-hover">
    <thead>
    <tr>
        <%--<th class="{sorter: false}">&nbsp;</th>--%>
        <%--<th>#</th>--%>
        <th>
            <fmt:message key="label.key"/>
        </th>
        <th>
            <fmt:message key="mountpoint.label"/>
        </th>
        <th>
            <fmt:message key="label.dataSource"/>
        </th>
        <th class="{sorter: false}">
            <fmt:message key="label.actions"/>
        </th>
    </tr>
    </thead>

    <tbody>

    <c:forEach items="${mountedProviders}" var="mount" varStatus="loopStatus">
        <tr>
            <%--<td><input name="selectedProviders" type="checkbox" value="${mount.id}"/></td>--%>
            <%--<td>--%>
                 <%--${mount.id}--%>
            <%--</td>--%>
            <td>
                ${mount.key}
            </td>
            <td>
                ${mount.mountPoint}
            </td>
            <td>
                ${mount.dataSource.clazz}

            </td>
            <td>
                <form style="margin: 0;" action="${flowExecutionUrl}" method="post">
                    <input type="hidden" name="mountpoint" value="${mount.mountPoint}"/>

                    <button class="btn btn-info" type="submit" name="_eventId_viewDataSource" onclick="">
                        <i class=" "></i>&nbsp;<fmt:message key="label.info"/>
                    </button>
                </form>
                <%--<c:if test="${mount.dynamic}">--%>
                <%--<form style="margin: 0;" action="${flowExecutionUrl}" method="post">--%>
                    <%--<input type="hidden" name="mountpoint" value="${mount.mountPoint}"/>--%>

                    <%--<button class="btn btn-danger" type="submit" name="_eventId_unmountProvider" onclick="">--%>
                        <%--<i class=" icon-stop icon-white"></i>&nbsp;<fmt:message key="label.unmount"/>--%>
                    <%--</button>--%>
                <%--</form>--%>
                <%--</c:if>--%>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
