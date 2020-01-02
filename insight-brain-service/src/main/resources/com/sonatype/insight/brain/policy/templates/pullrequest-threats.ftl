## :shield: Automated pull request to fix ${threatList?size} Nexus IQ Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**

### Policy
Threat (of 10) | Policy | Constraint | Violation Details
-- | -- | -- | --
<#list threatList as threat>
${threat.threat} | ${threat.policy} | ${threat.constraint} | ${threat.conditions}
</#list>

### Nexus IQ Scan Detail
**Application**: ${applicationName}
**Organization**: ${organizationName}
**Date**: ${date}
**Stage**: ${stage}

[Review full report](${detailedReportUrl})

_This PR was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
