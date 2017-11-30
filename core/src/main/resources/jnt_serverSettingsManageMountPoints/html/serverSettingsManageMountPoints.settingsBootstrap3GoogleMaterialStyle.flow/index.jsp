<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="mailSettings" type="org.jahia.services.mail.MailSettings"--%>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>
<%--@elvariable id="mountPointManager" type="org.jahia.modules.external.admin.mount.model.MountPointManager"--%>

<template:addResources type="javascript"
                       resources="jquery.min.js,jquery-ui.min.js,jquery.metadata.js,jquery.tablesorter.js,jquery.tablecloth.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tablecloth.css"/>
<template:addResources type="javascript"
                       resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js,settings/dataTables.initializer.js"/>

<script type="text/javascript" charset="utf-8">
    $(document).ready(function () {

        dataTablesSettings.init('mountsTable', 10, [], null, null);

    });

    function submitEvent(action, mountPoint) {
        $("#MPFormValue").val(mountPoint);
        $("#MPFormAction").val(action);
        $("#MPForm").submit();
    }

    function submitEventWithConfirm(action, mountPoint, message) {
        if (confirm(message)) {
            submitEvent(action, mountPoint);
        }
    }

    function goToFactory() {
        var url = $("#mountPointFactory").val();
        if (url) {
            window.parent.goToUrl(url);
        }
    }
</script>

<form style="margin: 0; display: none;" action="${flowExecutionUrl}" method="post" id="MPForm">
    <input type="hidden" name="name" id="MPFormValue"/>
    <input type="hidden" name="action" id="MPFormAction"/>
    <input type="hidden" name="_eventId" value="doAction">
</form>
<div class="page-header">
    <h2><fmt:message key="serverSettings.mountPointsManagement"/></h2>
</div>

<div class="panel panel-default">
    <div class="panel-body">

        <div>
            <h3><fmt:message key="serverSettings.mountPointsManagement.add"/></h3>
            <form style="margin: 0;">
                <div class="input-group">
                    <select class="form-control" id="mountPointFactory">
                        <c:forEach var="providerFactory" items="${mountPointManager.mountPointFactories}">
                            <c:if test="${not empty providerFactory.value.endOfURL}">
                                <option value="<c:url value='${url.base}${providerFactory.value.endOfURL}'/>">${providerFactory.value.displayableName}</option>
                            </c:if>
                        </c:forEach>
                    </select>
                    <span class="input-group-btn">
                        <a class="btn btn-default btn-sm btn-primary" href="#" onclick="goToFactory()">
                            <span><fmt:message key="label.add"/></span>
                         </a>
                    </span>
                </div>
            </form>
        </div>

        <p>
            <c:forEach items="${flowRequestContext.messageContext.allMessages}" var="message">
            <c:if test="${message.severity eq 'INFO'}">
        <div class="alert alert-success">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
                ${message.text}
        </div>
        </c:if>
        <c:if test="${message.severity eq 'WARNING'}">
            <div class="alert alert-warning">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                    ${message.text}
            </div>
        </c:if>
        <c:if test="${message.severity eq 'ERROR'}">
            <div class="alert alert-error">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                    ${message.text}
            </div>
        </c:if>
        </c:forEach>
        </p>

        <table id="mountsTable" class="table table-bordered table-striped table-hover">
            <thead>
            <tr>
                <th>
                    <fmt:message key="label.name"/>
                </th>
                <th>
                    <fmt:message key="label.mountPoint"/>
                </th>
                <th>
                    <fmt:message key="label.MountPointProperties"/>
                </th>
                <th width="100px">
                    <fmt:message key="label.status"/>
                </th>
                <th class="{sorter: false}" width="250px">
                    <fmt:message key="label.actions"/>
                </th>
            </tr>
            </thead>

            <tbody>
            <c:forEach items="${mountPointManager.mountPoints}" var="mountPoint" varStatus="loopStatus">
                <tr>
                    <td>
                            ${mountPoint.name}
                    </td>
                    <td>
                            ${mountPoint.path}
                    </td>
                    <td>
                        <c:forEach items="${mountPoint.remoteProperties}" var="prop">
                            <ul>
                                <li>${prop.key}: ${prop.value}</li>
                            </ul>
                        </c:forEach>
                    </td>
                    <td>
                <span class="label ${mountPoint.displayStatusClass}">
                    <fmt:message key="serverSettings.mountPointsManagement.mountStatus.${mountPoint.status}"/>
                </span>
                    </td>
                    <td>
                        <c:if test="${mountPoint.showMountAction}">
                            <button class="btn btn-info" type="button" onclick="submitEvent('mount', '${mountPoint.realName}')">
                                <i class="material-icons">file_download</i>&nbsp;<fmt:message
                                    key="serverSettings.mountPointsManagement.action.mount"/>
                            </button>
                        </c:if>
                        <c:if test="${mountPoint.showUnmountAction}">
                            <button class="btn btn-info" type="button" onclick="submitEvent('unmount', '${mountPoint.realName}')">
                                <i class="material-icons">file_upload</i>&nbsp;<fmt:message
                                    key="serverSettings.mountPointsManagement.action.unmount"/>
                            </button>
                        </c:if>
                        <fmt:message var="confirmDelete" key="serverSettings.mountPointsManagement.action.confirmDelete">
                            <fmt:param value="${mountPoint.name}"/>
                        </fmt:message>
                        <c:if test="${not empty mountPointManager.mountPointFactories[mountPoint.nodetype]}">
                            <c:url var="editURL" value="${url.base}${mountPointManager.mountPointFactories[mountPoint.nodetype].endOfURL}"/>
                            <a class="btn btn-sm btn-primary" href="#"
                               onclick="window.parent.goToUrl('${editURL}?edit=${mountPoint.identifier}')">
                                <i class="material-icons">edit</i>
                            </a>
                        </c:if>
                        <button class="btn btn-sm btn-danger" type="button"
                                onclick="submitEventWithConfirm('delete', '${mountPoint.realName}', '${functions:escapeJavaScript(confirmDelete)}')">
                            <i class="material-icons">delete</i>
                        </button>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>
