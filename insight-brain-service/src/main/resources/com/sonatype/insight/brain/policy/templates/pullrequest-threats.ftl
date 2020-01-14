## :shield: Automated pull request to fix ${threatList?size} Nexus IQ Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**

### Policy
Threat (of 10) | Policy | Violation Details
--- | --- | ---
<#list threatList as threat>
${threat.threat} | ${threat.policy} | ${threat.details}
</#list>

### Nexus IQ Scan Detail
**Application**: ${applicationName}
**Organization**: ${organizationName}
**Date**: ${date}
**Stage**: ${stage}

[Review full report](${detailedReportUrl})

_This PR was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
