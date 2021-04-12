<#include "iq-for-scm-common.ftl">
<#if ( policiesViolatedCount > 0 )><#t>
### :thinking_face: Nexus IQ found <#if ( policiesViolatedCount > 1 )>multiple policy violations<#else>a policy violation</#if> introduced by this PR:

&#8192;<#-- spacer -->

<#list componentList as component>
  <#switch component.dependencyLogo>
      <#case "d-logo.png">
        \[Direct\] <#t>
          <#break>
      <#case "t-logo.png">
        \[Transitive\] <#t>
  </#switch>
**${component.componentNameAndVersion}**<#if component.lineCommentLink?has_content> - [line comment](${component.lineCommentLink})</#if><#lt>

  <#if component.suggestedVersion?has_content>
    :shield: **Bumping to version ${component.suggestedVersion}** will resolve all policy violations for this component<#if ( component.remediationForDependencies )> and its dependencies</#if><#lt>
    <@breakingChanges count=component.breakingChangesCount minimalMarkdown=true />
  <#else>
    :warning: No recommended versions are available for this component<#lt>
  </#if>
  <#lt>

  | **Threat (of 10)** | **Policy** | **Violation Details** |<#lt>
  | --- | --- | --- |<#lt>
  <#list component.policiesViolated as policy>
      | ${policy.threatLevel} | ${policy.name} | <#t>
      <#list policy.constraints as constraint><#t>
        **${constraint.constraintName}:** <#t>
        <#list constraint.conditions as condition>${condition}. </#list><#t>
      </#list> |<#lt>
  </#list><#t>

  &#8192;<#-- spacer --><#lt>

</#list><#t>
<#else>
  ### :smiley: All Clear! Nexus IQ didn't find any policy violations introduced by this PR<#lt>

  Well done. The committed code does not violate any of your organization's Nexus IQ policies.<#lt>
</#if>

&#8192;<#-- spacer -->

<#if ( fixedPolicyViolationsCount > 0 )>
---<#lt>
### :sunglasses: Nexus IQ determined that you fixed <#if ( fixedPolicyViolationsCount > 1 )>outstanding policy violations<#else>an outstanding policy violation</#if>:

<#list fixedComponentList as component>
  :white_check_mark: **${component.componentNameAndVersion}**<#lt>

  | **Threat (of 10)** | **Policy** | **Violation Details** |<#lt>
  | --- | --- | --- |<#lt>
  <#list component.policiesViolated as policy><#t>
    | ${policy.threatLevel} | ${policy.name} | <#t>
      <#list policy.constraints as constraint><#t>
        **${constraint.constraintName}:** <#t>
          <#list constraint.conditions as condition>${condition}. </#list><#t>
      </#list> |<#lt>
  </#list>

  &#8192;<#-- spacer --><#lt>

</#list>
</#if>
### Nexus IQ Report Details<#lt>
**Application**: ${applicationName}   <#lt><#-- 3 spaces before a newline renders as a br -->
**Organization**: ${organizationName}   <#lt>
**Date**: ${date}<#lt>

**PR Branch**: ${featureBranchStage} Stage - [Full Report](${detailedFeatureBranchReportUrl})   <#lt>
**Default Branch**: ${defaultBranchStage} Stage - [Full Report](${detailedDefaultBranchReportUrl})<#lt>

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)<#t>
