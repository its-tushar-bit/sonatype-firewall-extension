<#if ( policiesViolatedCount > 0 )>
  ### :thinking: Nexus IQ found <#if ( policiesViolatedCount > 1 )>multiple policy violations<#else>a policy violation</#if> introduced by this PR:<#lt>

<#list componentList as component>
<details>
  <#assign threatColor="${threatColorArray[component.highestThreatLevel]}">
  <summary title="Threat Level: ${component.highestThreatLevel} of 10"><#t>
    <img alt="T${component.highestThreatLevel}" src="https://placehold.it/4x12/${threatColor}/000000?text=+"> <#lt>
    <b>${component.highestThreatLevel}<#if ( component.highestThreatLevel < 10 )>&nbsp;</#if>&nbsp;&nbsp; ${component.componentNameAndVersion}</b><#t>
    <#if component.lineCommentLink?has_content> - <a href="${component.lineCommentLink}">line comment</a></#if><#t>
  </summary><#lt>
  <p></p><#lt>

<#if component.suggestedVersion?has_content>
  :shield: **Bumping to version ${component.suggestedVersion}** will resolve <#if ( component.policiesViolated?size > 1 )>these violations<#else>this violation</#if><#lt>
<#else>
  :warning: No recommended versions are available for this component<#lt>
</#if>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list component.policiesViolated as policy>
${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
  <b>${constraint.constraintName}:</b><ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition}</li><#t>
    </#list>
    </ul><#t>
  </#list>

</#list>

</details>

</#list>
<#else>
  ### :smiley: All Clear! Nexus IQ didn't find any policy violations introduced by this PR<#lt>
  Well done. The committed code does not violate any of your organization's Nexus IQ policies.<#lt>

</#if>
<#if ( fixedPolicyViolationsCount > 0 )>
---
  ### :sunglasses: Nexus IQ determined that you fixed <#if ( fixedPolicyViolationsCount > 1 )>outstanding policy violations<#else>an outstanding policy violation</#if>:<#lt>

<#list fixedComponentList as component>
<details>
  <#assign threatColor="${threatColorArray[component.highestThreatLevel]}">
  <summary title="Threat Level: ${component.highestThreatLevel} of 10">:white_check_mark: <#t>
    <b>&nbsp; ${component.componentNameAndVersion}</b></summary><#lt>
  <p></p><#lt>
  
Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list component.policiesViolated as policy>
${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
  <b>${constraint.constraintName}:</b><ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition}</li><#t>
    </#list>
    </ul><#t>
  </#list>

</#list>

</details>

</#list>
</#if>
----
### Nexus IQ Report Detail
**Application**: ${applicationName}
**Organization**: ${organizationName}
**Date**: ${date}<#t>

**PR Branch**: ${featureBranchStage} Stage - [Full Report](${detailedFeatureBranchReportUrl})
**Default Branch**: ${defaultBranchStage} Stage - [Full Report](${detailedDefaultBranchReportUrl})

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)
