<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<html:xhtml/>

<h1><bean:message bundle="RESEARCHER_RESOURCES" key="researcher.PrizeAssociation.title.label"/></h1>

<bean:define id="sotisURL">
    <%= pt.ist.fenix.FenixIstConfiguration.getConfiguration().sotisURL() %>
</bean:define>

<bean:define id="lang">
    <%= org.fenixedu.commons.i18n.I18N.getLocale().toLanguageTag() %>
</bean:define>

<bean:define id="researchers" name="researchers" />

<script src="<%= sotisURL %>/js/sotis-embedded.js" data-sotis-use="prizes" data-sotis-users="<%= researchers %>" data-sotis-links="yes" data-sotis-lang="<%= lang %>"></script>