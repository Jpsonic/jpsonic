<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--
  ~ This file is part of Airsonic.
  ~
  ~  Airsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Airsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2015 (C) Sindre Mehus
  --%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>

<body class="mainframe settings sonosSettings">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="sonos"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="useRadio" value="${model.useRadio}"/>
    <c:param name="useSonos" value="${model.useSonos}"/>
</c:import>

<form method="post" action="sonosSettings.view">
    <sec:csrfInput />

    <div>
        <input type="checkbox" name="sonosEnabled" id="sonosEnabled" ${model.sonosEnabled ? "checked" : ""}/>
        <label for="sonosEnabled"><fmt:message key="sonossettings.enabled"/></label>
    </div>

    <p class="detail">
        <fmt:message key="sonossettings.description"/>
    </p>
    <div>
        <fmt:message key="sonossettings.servicename"/>
        <input name="sonosServiceName" id="sonosServiceName" value="<c:out value='${model.sonosServiceName}' escapeXml='true'/>"/>
    </div>
    <p class="detail">
        <fmt:message key="sonossettings.servicename.description"/>
    </p>

    <p>
        <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
    </p>

</form>

</body></html>
