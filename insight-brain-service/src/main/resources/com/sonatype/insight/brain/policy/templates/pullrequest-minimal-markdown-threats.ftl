<#include "iq-for-scm-common.ftl">
<#assign recommendedNonBreaking = "recommended-non-breaking" >
<#assign recommendedNonBreakingWithDependencies = "recommended-non-breaking-with-dependencies" >
<#if isInnerSource>
<#if !isManualPullRequest>
## AutoPR: A new version of ${componentName} is available

**Component: ${componentName}**

**Suggested version: ${targetVersionDisplay}**

Current version: ${initialVersionDisplay}
<#else>
## A new version of ${componentName} is available

### Description

* Component: **${componentName}**
* Current version: **${initialVersionDisplay}**
* New version: **${targetVersionDisplay}**
</#if>

<#elseif targetVersionType == recommendedNonBreakingWithDependencies>
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
## :shield: <#if !isManualPullRequest>Automated p<#else>P</#if>ull request: Sonatype Lifecycle found ${threatList?size} Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**
<@breakingChanges count=breakingChangesCount minimalMarkdown=true />

### Policy Violations
</#if>
<#if !isInnerSource>
| Threat (of 10) | Policy | Violation Details
| --- | --- | ---
<#list threatList as threat>
| ${threat.threat} | ${threat.policy} | <#list threat.constraints as constraint><#t>
  **${constraint.constraintName}:** <#list constraint.conditions as condition>${condition?replace("*", "\\*")}. </#list><#t>
</#list>

</#list>
</#if>

### Sonatype Lifecycle Scan Detail
**Application**: ${applicationName}  <#-- leave 2 trailing spaces as a line break -->
**Organization**: ${organizationName}  <#-- leave 2 trailing spaces as a line break -->
**Date**: ${date}  <#-- leave 2 trailing spaces as a line break -->
**Stage**: ${stage}  

[Review full report](${detailedReportUrl})

<#if isManualPullRequest>
**Created by:** ${displayNameOrUsername}
<#else>
_This PR was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
</#if>
