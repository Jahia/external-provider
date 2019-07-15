<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="vfsFactory" type="org.jahia.modules.external.vfs.factory.VFSMountPointFactory"--%>
<template:addResources type="javascript" resources="admin/angular.min.js"/>
<template:addResources type="javascript" resources="admin/app/folderPicker.js"/>
<template:addResources type="css" resources="admin/app/folderPicker.css"/>

<div class="page-header">
    <h2><fmt:message key="vfsFactory"/></h2>
</div>

<%@ include file="errors.jspf" %>

<div class="folderPickerApp" ng-app="folderPicker">
    <div class="panel panel-default">
        <div class="panel-body">

            <fmt:message var="selectTarget" key="vfsFactory.selectTarget"/>
            <c:set var="i18NSelectTarget" value="${functions:escapeJavaScript(selectTarget)}"/>
            <div class="box-1" ng-controller="folderPickerCtrl"
                 ng-init='init(${localFolders}, "${fn:escapeXml(vfsFactory.localPath)}", "localPath", true, "${i18NSelectTarget}")'>
                <form:form modelAttribute="vfsFactory" method="post" cssClass="form">
                    <fieldset title="local">
                        <div>
                            <div class="form-group label-floating">
                                <form:label path="name" cssClass="control-label"><fmt:message key="label.name"/> <span style="color:
                                red">*</span></form:label>
                                <form:input path="name" cssClass="form-control"/>
                            </div>
                            <div class="form-group label-floating">
                                <form:label path="root" cssClass="control-label"><fmt:message key="vfsFactory.root"/> <span style="color: red">*</span></form:label>
                                <form:input path="root" cssClass="form-control"/>
                            </div>
                            <div class="form-group">
                                <jsp:include page="/modules/external-provider/angular/folderPicker.settingsBootstrap3GoogleMaterialStyle.jsp"/>
                            </div>
                        </div>
                    </fieldset>
                    <fieldset>
                        <div class="col-md-12">
                            <button class="btn btn-primary btn-raised pull-right" type="submit" name="_eventId_save">
                                <span><fmt:message key="label.save"/></span>
                            </button>
                            <button class="btn btn-default pull-right" type="submit" name="_eventId_cancel">
                                <span><fmt:message key="label.cancel"/></span>
                            </button>
                        </div>
                    </fieldset>
                </form:form>
            </div>
        </div>
    </div>
</div>
