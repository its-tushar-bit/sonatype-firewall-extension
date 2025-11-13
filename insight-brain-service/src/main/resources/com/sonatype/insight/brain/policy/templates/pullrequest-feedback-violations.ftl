<#include "iq-for-scm-common.ftl">
<#if provider.name() == "GITLAB"><#assign width=14><#else><#assign width=12></#if>
<#if ( policiesViolatedCount > 0 )>
### :thinking: Sonatype Lifecycle found <#if ( policiesViolatedCount > 1 )>multiple policy violations<#else>a policy violation</#if> introduced by this <#if provider.name() == "GITLAB">MR<#else>PR</#if>:<#lt>

<#list componentList as component>
<details>
  <#assign threatImage="${threatImageArray[component.highestThreatLevel]}">
  <summary title="Threat Level: ${component.highestThreatLevel} of 10"><#t>
    <a href="#;"><img alt="T${component.highestThreatLevel}" src="https://cdn.sonatype.com/iq-for-scm/1.0/${threatImage}" width="4" <#if provider.name() == "GITLAB">height="16"<#else>height="14"</#if>></a> <#lt>
    <b>${component.highestThreatLevel}<#if ( component.highestThreatLevel < 10 )>&nbsp;&nbsp;</#if>&nbsp;<#lt>
    <a href="#;"><img alt="dependency logo" src="https://cdn.sonatype.com/iq-for-scm/1.0/${component.dependencyLogo}" <#if provider.name() == "GITLAB">height="16"<#else>height="14"</#if>></a>&nbsp;<#lt>
    ${component.componentNameAndVersion}</b><#t>
    <#if component.lineCommentLink?has_content> - <a href="${component.lineCommentLink}">line comment</a></#if><#t>
  </summary><#lt>
  <p></p><#lt>

<#if component.suggestedVersion?has_content>
  :shield: **Bumping to version ${component.suggestedVersion}** will resolve all policy violations for this component<#if ( component.remediationForDependencies )> and its dependencies</#if><#lt><#if provider.name() == "GITLAB"><br /></#if>
  <#if component.remediationTypeDisplayName == recommendedNonBreakingWithDependencies>
  <@breakingChangesWithRemediationType remediationType=component.remediationTypeDisplayName minimalMarkdown=false width=width/>
  <#else>
  <@breakingChanges count=component.breakingChangesCount minimalMarkdown=false width=width/> 
  </#if>
<#else>
  :warning: No recommended versions are available for this component<#lt>
</#if>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list component.policiesViolated as policy>
${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
  <b>${constraint.constraintName}:</b><ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition?replace("*", "\\*")}</li><#t>
    </#list>
    </ul><#t>
  </#list>

</#list>

<#-- Remove provider check if possible. Depends on completion of
https://sonatype.atlassian.net/browse/SDEV-208
https://sonatype.atlassian.net/browse/SDEV-209
https://sonatype.atlassian.net/browse/SDEV-210
https://sonatype.atlassian.net/browse/SDEV-154
-->
<#if (provider.name() == "GITHUB" || provider.name() == "GITLAB") && component.componentScanHash?has_content>
  [Component detail 🔍](${baseFeatureBranchURL}/componentDetails/${component.componentScanHash}?source=pr-commenting)
</#if>
</details>

</#list>
<#else>
  ### :smiley: All Clear! Sonatype Lifecycle didn't find any policy violations introduced by this <#if provider.name() == "GITLAB">MR<#else>PR</#if><#lt>
  <#if hasNoViolationsInPR>
  Well done. The committed code does not violate any of your organization's Sonatype Lifecycle policies.<#lt>

  </#if>
</#if>
<#if ( fixedPolicyViolationsCount > 0 )>
---
  ### :sunglasses: Sonatype Lifecycle determined that you fixed <#if ( fixedPolicyViolationsCount > 1 )>outstanding policy violations<#else>an outstanding policy violation</#if>:<#lt>

<#list fixedComponentList as component>
<#assign threatImage="${threatImageArray[component.highestThreatLevel]}">
<details>
  <summary title="Threat Level: ${component.highestThreatLevel} of 10"><#t>
    <img alt="T${component.highestThreatLevel}" src="https://cdn.sonatype.com/iq-for-scm/1.0/${threatImage}" width="4" <#if provider.name() == "GITLAB">height="16"<#else>height="14"</#if>> <#lt>
    <b>${component.highestThreatLevel}<#if ( component.highestThreatLevel < 10 )>&nbsp;&nbsp;</#if>&nbsp;${component.componentNameAndVersion}</b>&nbsp;&nbsp; :white_check_mark:</summary><#lt>
  <p></p><#lt>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list component.policiesViolated as policy>
${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
  <b>${constraint.constraintName}:</b><ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition?replace("*", "\\*")}</li><#t>
    </#list>
    </ul><#t>
  </#list>

</#list>

</details>

</#list>
</#if>
----
### Sonatype Lifecycle Report Detail
**Application**: ${applicationName}<#if provider.name() == "GITLAB">\</#if>
**Organization**: ${organizationName}<#if provider.name() == "GITLAB">\</#if>
**Date**: ${date}<#if provider.name() == "GITLAB">\</#if>
**<#if provider.name() == "GITLAB">Source<#else>PR</#if> Branch**: ${featureBranchStage} Stage - [Full Report](${detailedFeatureBranchReportUrl})<#if provider.name() == "GITLAB">\</#if>
**<#if provider.name() == "GITLAB">Target<#else>Base</#if> Branch**: ${baseBranchStage} Stage - [Full Report](${detailedBaseBranchReportUrl})<#if shouldIncludePrioritiesReport && provider.name() == "GITLAB">\</#if>
<#if shouldIncludePrioritiesReport>
**Application Priorities** - [View](${featureBranchPrioritiesUrl})
</#if>

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)
