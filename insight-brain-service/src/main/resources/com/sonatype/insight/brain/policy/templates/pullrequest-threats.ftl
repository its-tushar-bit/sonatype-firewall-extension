<#include "iq-for-scm-common.ftl">
<#assign recommendedNonBreaking = "recommended-non-breaking" >
<#assign recommendedNonBreakingWithDependencies = "recommended-non-breaking-with-dependencies" >
<#if provider.name() == "GITLAB"><#assign width=14><#else><#assign width=12></#if>
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
## <img src="https://cdn.sonatype.com/iq-for-scm/1.0/golden-pr.png" width="34" height="22" alt="golden PR icon"> <#if !isManualPullRequest>Auto</#if><#if provider.name() == "GITLAB">M<#else>P</#if>R: Bump ${componentName} to resolve ${threatList?size} policy violation<#if (threatList?size > 1)>s</#if>

**Component: ${componentName}**
- **Suggested version: ${targetVersionDisplay}**<#if provider.name() == "GITLAB">\</#if>
&nbsp;<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; No breaking changes<#if provider.name() == "GITLAB">\</#if>
&nbsp;<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; No policy violations for this component<#if provider.name() == "GITLAB">\</#if>
&nbsp;<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; No policy violations for dependencies
- Current version (with violations): **${initialVersionDisplay}**

**Violations resolved by new version:**
<#elseif targetVersionType == recommendedNonBreaking>
## <#if !isManualPullRequest>Auto</#if><#if provider.name() == "GITLAB">M<#else>P</#if>R: Bump ${componentName} to resolve ${threatList?size} policy violation<#if (threatList?size > 1)>s</#if>

**Component: ${componentName}**
- **Suggested version: ${targetVersionDisplay}**<#if provider.name() == "GITLAB">\</#if>
&nbsp;<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; No breaking changes<#if provider.name() == "GITLAB">\</#if>
&nbsp;<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; No policy violations for this component
- Current version (with violations): **${initialVersionDisplay}**

**Violations resolved by new version:**
<#else>
## :shield: <#if !isManualPullRequest>Automated p<#else>P</#if>ull request: Sonatype Lifecycle found ${threatList?size} Policy Violation<#if (threatList?size > 1)>s</#if>

### Description

- Component: **${componentName}**
- Current version (with violations): **${initialVersionDisplay}**
- New version (for remediation): **${targetVersionDisplay}**
<@breakingChanges count=breakingChangesCount minimalMarkdown=false width=width/>

### Policy
</#if>
<#if !isInnerSource>
Threat (of 10) | Policy | Violation Details
--- | --- | ---
<#list threatList as threat>
<#if (threat?index < maxThreats)>
    ${threat.threat} | ${threat.policy} | <#list threat.constraints as constraint><#t>
  <b>${constraint.constraintName}:</b><#t>
  <ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition?replace("*", "\\*")}</li><#t>
    </#list>
  </ul><#t>
</#list>

</#if>
</#list>
<#if (threatList?size > maxThreats)>

...and ${threatList?size - maxThreats} more policy violation(s).

</#if>
</#if>

### Sonatype Lifecycle Scan Detail
**Application**: ${applicationName}<#if provider.name() == "GITLAB">\</#if>
**Organization**: ${organizationName}<#if provider.name() == "GITLAB">\</#if>
**Date**: ${date}<#if provider.name() == "GITLAB">\</#if>
**Stage**: ${stage}

[Review full report](${detailedReportUrl})

<#if isManualPullRequest>
**Created by:** ${displayNameOrUsername}
<#else>
_This <#if provider.name() == "GITLAB">MR<#else>PR</#if> was automatically created by your friendly neighbourhood [IQ Server](${baseIqUrl})_
</#if>
