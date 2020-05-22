<#if ( policiesViolatedCount > 0 )>
### :thinking: Nexus IQ found <#if ( policiesViolatedCount > 1 )>multiple policy violations<#else>a policy violation</#if> introduced by this PR:\n\n<#t>
<#list componentList as component>
  #### ${component.componentNameAndVersion}\n\n<#t>
  <#if component.suggestedVersion?has_content>
    :shield: **Bumping to version ${component.suggestedVersion}** will resolve <#if ( component.policiesViolated?size > 1 )>these violations<#else>this violation</#if>\n<#t>
  <#else>
    :warning: No recommended versions are available for this component\n<#t>
  </#if>
\n<#t>
| **Threat (of 10)** | **Policy** | **Violation Details** |\n<#t>
| --- | --- | --- |\n<#t>
<#list component.policiesViolated as policy>
    | ${policy.threatLevel} | ${policy.name} | <#t>
    <#list policy.constraints as constraint><#t>
      **${constraint.constraintName}:** <#t>
      <#list constraint.conditions as condition>${condition}. </#list><#t>
    </#list> |\n<#t>
</#list><#t>
\n\n  ‌\n\n<#t>
</#list><#t>
<#else>
  ### :smiley: All Clear! Nexus IQ didn't find any policy violations introduced by this PR   \n<#t>
  Well done. The committed code does not violate any of your organization's Nexus IQ policies.   \n<#t>
  \n<#t>
</#if>
<#t>
<#if ( fixedPolicyViolationsCount > 0 )>
---\n<#t>
### :sunglasses: Nexus IQ determined that you fixed <#if ( fixedPolicyViolationsCount > 1 )>outstanding policy violations<#else>an outstanding policy violation</#if>:\n<#t>
\n<#t>
<#list fixedComponentList as component>
#### :white_check_mark: ${component.componentNameAndVersion}\n\n<#t>
| **Threat (of 10)** | **Policy** | **Violation Details** |\n<#t>
| --- | --- | --- |\n<#t>
<#list component.policiesViolated as policy><#t>
  | ${policy.threatLevel} | ${policy.name} | <#t>
    <#list policy.constraints as constraint><#t>
      **${constraint.constraintName}:** <#t>
        <#list constraint.conditions as condition>${condition}. </#list><#t>
    </#list> |\n<#t>
</#list><#t>
\n\n  ‌\n\n<#t>
</#list><#t>
</#if>
\n\n<#t>
### Nexus IQ Report Details\n<#t>
**Application**: ${applicationName}   \n<#t><#-- 3 spaces with a \n renders as a br -->
**Organization**: ${organizationName}   \n<#t>
**Date**: ${date}\n<#t>
\n<#t>
**PR Branch**: ${featureBranchStage} Stage - [Full Report](${detailedFeatureBranchReportUrl})   \n<#t>
**Default Branch**: ${defaultBranchStage} Stage - [Full Report](${detailedDefaultBranchReportUrl})\n<#t>
\n<#t>
[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)<#t>
