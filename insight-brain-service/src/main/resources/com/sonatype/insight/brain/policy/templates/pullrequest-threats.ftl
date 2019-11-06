:shield:  **This automated pull request fixes a Nexus IQ [policy violation](${detailedReportUrl})**

### Description
Bump component [${initialCoordinates}](${initialSearchUrl}) to version [${targetVersion}](${targetSearchUrl}) to remediate the following policy violations

### Policy
Policy | Threat | Constraint | Conditions
-- | -- | -- | --
<#list threatList as threat>
${threat.policy} | ${threat.threat} | ${threat.constraint} | ${threat.conditions}
</#list>

### Source
**Application**: ${applicationName}
**Organization**: ${organizationName}
**Scan**: ${scanId} [view detailed report](${detailedReportUrl})
**Stage**: ${stage}

_This PR was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
