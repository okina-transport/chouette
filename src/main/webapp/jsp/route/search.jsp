<%@ taglib prefix="s" uri="/struts-tags" %>
<title><s:text name="text.itineraire.recherche.title"/></title>
<s:url id="urlItineraires" action="search" namespace="/route" includeParams="none"/>
<s:property value="filAriane.addElementFilAriane(getText('text.itineraire.recherche.title'), '', #urlItineraires)"/>
<div class="panelData">
	<s:property value="filAriane.texteFilAriane" escape="false"/>
</div>
<br>
<s:text name="text.itineraire.recherche.indication"/>