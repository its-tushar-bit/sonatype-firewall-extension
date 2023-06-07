<#include "iq-for-scm-common.ftl">
## :shield: Automated pull request: Nexus IQ found ${threatList?size} Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**
<@breakingChanges count=breakingChangesCount minimalMarkdown=true />

### Policy Violations
| Threat (of 10) | Policy | Violation Details
| --- | --- | ---
<#list threatList as threat>
| ${threat.threat} | ${threat.policy} | <#list threat.constraints as constraint><#t>
  **${constraint.constraintName}:** <#list constraint.conditions as condition>${condition?replace("*", "\\*")}. </#list><#t>
</#list>

</#list>

### Nexus IQ Scan Detail
**Application**: ${applicationName}  <#-- leave 2 trailing spaces as a line break -->
**Organization**: ${organizationName}  <#-- leave 2 trailing spaces as a line break -->
**Date**: ${date}  <#-- leave 2 trailing spaces as a line break -->
**Stage**: ${stage}  

[Review full report](${detailedReportUrl})

_This PR was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
