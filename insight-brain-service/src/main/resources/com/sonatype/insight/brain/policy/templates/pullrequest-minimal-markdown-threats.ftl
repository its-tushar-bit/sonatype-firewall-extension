<#include "iq-for-scm-common.ftl">
<#assign recommendedNonBreaking = "recommended-non-breaking" >
<#assign recommendedNonBreakingWithDependencies = "recommended-non-breaking-with-dependencies" >
<#if targetVersionType == recommendedNonBreakingWithDependencies>
## :star: <#if !isManualPullRequest>Auto</#if>PR: Bump ${componentName} to resolve ${threatList?size} policy violation<#if (threatList?size > 1)>s</#if>

**Component: ${componentName}**
- **Suggested version: ${targetVersionDisplay}**
&nbsp; :white_check_mark: &nbsp; No breaking changes
&nbsp; :white_check_mark: &nbsp; No policy violations for this component
&nbsp; :white_check_mark: &nbsp; No policy violations for dependencies
- Current version (with violations): **${initialVersionDisplay}**

**Violations resolved by new version:**

<#elseif targetVersionType == recommendedNonBreaking>
## <#if !isManualPullRequest>Auto</#if>PR: Bump ${componentName} to resolve ${threatList?size} policy violation<#if (threatList?size > 1)>s</#if>

**Component: ${componentName}**
- **Suggested version: ${targetVersionDisplay}**
&nbsp; :white_check_mark: &nbsp; No breaking changes
&nbsp; :white_check_mark: &nbsp; No policy violations for this component
- Current version (with violations): **${initialVersionDisplay}**

**Violations resolved by new version:**

<#else>
## :shield: <#if !isManualPullRequest>Automated p<#else>P</#if>ull request: Nexus IQ found ${threatList?size} Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**
<@breakingChanges count=breakingChangesCount minimalMarkdown=true />

### Policy Violations
</#if>
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

_This PR was <#if !isManualPullRequest>automatically </#if>created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
