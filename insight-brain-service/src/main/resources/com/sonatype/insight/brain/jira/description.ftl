h1. Nexus IQ Notification

<#-- Generate a chiclet panel -->
<#macro chiclet bgColor count>
{panel:borderStyle=solid|bgColor=${bgColor}}{color:#FFFFFF}${count}{color}{panel}<#t>
</#macro>

<#-- Whitespace is important to render table with panels as chiclets -->
|<@chiclet bgColor="#ED1C24" count="${policyAlertCounts.red}"/><#t>
|<@chiclet bgColor="#F7931D" count="${policyAlertCounts.orange}"/><#t>
|<@chiclet bgColor="#FFDD17" count="${policyAlertCounts.yellow}"/><#t>
|<@chiclet bgColor="#006bbf" count="${policyAlertCounts.darkBlue}"/><#t>
|<@chiclet bgColor="#6D98CF" count="${policyAlertCounts.blue}"/>|<#lt>

<#-- Whitespace here is important to keep seperate from chiclets above -->
|*Application*|${app.name}|
<#-- Include reference to the detailed report -->
|*Scan*       |${scanId}; [View detailed report|${detailedReportUrl}]|
|*Stage*      |${stage}|
<#-- Include contact if present -->
<#if contact??>
|*Contact*    |[${contact.displayName}|<#if contact.email??>mailto:${contact.email}]|</#if>
</#if>

<#-- Returns the background color for alert section based on threat-level -->
<#macro threat_color threatLevel>
  <#if (threatLevel > 7)>
  #ED1C24<#t>
  <#elseif (threatLevel > 3)>
  #F7941D<#t>
  <#elseif (threatLevel > 1)>
  #FEDF15<#t>
  <#elseif (threatLevel > 0)>
  #006bbf<#t>
  <#else>
  #6D98CF<#t>
  </#if>
</#macro>

<#-- Renders an alert section panel with component details -->
<#macro fact_section section>
{panel:title=${section.threatLevel} - ${section.policyName}|titleColor=#FFFFFF|titleBGColor=<@threat_color threatLevel=section.threatLevel/>|bgColor=#FFFFFF}
<#list section.componentViolationCountMap as key, value>
* ${key} <#if value gt 1>{color:#808080}(${value} violations){color}</#if>
</#list>
{panel}
</#macro>

h2. Policy Alerts

<#-- Render all sections -->
<#list policyAlertSections.sections as section>
  <@fact_section section=section/>
</#list>
