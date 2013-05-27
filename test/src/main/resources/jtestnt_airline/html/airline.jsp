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
<h2>Airline</h2>
<ul>
	<li>  
  Airline : ${currentNode.properties['airline'].string}
  </li>
  <li>
  Full name : ${currentNode.properties['airline_full'].string}
  </li>
  <li>
  Basic rate : ${currentNode.properties['basic_rate'].double}
  </li>
  <li>
  Distance discount : ${currentNode.properties['distance_discount'].double}
  </li>
  <li>
  Economy seats : ${currentNode.properties['economy_seats'].long}
  </li>
  <li>
  Business level factor : ${currentNode.properties['business_level_factor'].double}
  </li>
  <li>
  Business seats : ${currentNode.properties['business_seats'].long}
  </li>
  <li>
  First class level factor : ${currentNode.properties['firstclass_level_factor'].double}
  </li>
  <li>
  First class seats : ${currentNode.properties['firstclass_seats'].long}
  </li>
</ul>

  