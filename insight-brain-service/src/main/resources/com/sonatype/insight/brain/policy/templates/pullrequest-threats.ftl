<#include "iq-for-scm-common.ftl">
<#if provider.name() == "GITLAB"><#assign width=14><#else><#assign width=12></#if>
## :shield: Automated <#if provider.name() == "GITLAB">merge<#else>pull</#if> request: Nexus IQ found ${threatList?size} Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**
<@breakingChanges count=breakingChangesCount minimalMarkdown=false width=width/>

### Policy
Threat (of 10) | Policy | Violation Details
--- | --- | ---
<#list threatList as threat>
    ${threat.threat} | ${threat.policy} | <#list threat.constraints as constraint><#t>
  <b>${constraint.constraintName}:</b><#t>
  <ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition?replace("*", "\\*")}</li><#t>
    </#list>
  </ul><#t>
</#list>

</#list>

### Nexus IQ Scan Detail
**Application**: ${applicationName}<#if provider.name() == "GITLAB">\</#if>
**Organization**: ${organizationName}<#if provider.name() == "GITLAB">\</#if>
**Date**: ${date}<#if provider.name() == "GITLAB">\</#if>
**Stage**: ${stage}

[Review full report](${detailedReportUrl})

_This <#if provider.name() == "GITLAB">MR<#else>PR</#if> was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
