<#include "iq-for-scm-common.ftl">
<#assign recommendedNonBreaking = "recommended-non-breaking" >
<#assign recommendedNonBreakingWithDependencies = "recommended-non-breaking-with-dependencies" >
<#if suggestedVersionType == recommendedNonBreakingWithDependencies>
### Sonatype Lifecycle found critical issues introduced by ${componentNameAndVersion}

Direct dependency | **Threat level: ${threatLevel}** | [View Component Details in Sonatype Lifecycle](${componentDetailsReportUrl})

:star: **Golden Version: ${suggestedVersion}**

:white_check_mark: Non-breaking upgrade resolves issues for this component and its dependencies

<#elseif suggestedVersionType == recommendedNonBreaking>
### Sonatype Lifecycle found critical issues introduced by ${componentNameAndVersion}

Direct dependency | **Threat level: ${threatLevel}** | [View Component Details in Sonatype Lifecycle](${componentDetailsReportUrl})

:shield: **Recommended Version: ${suggestedVersion}**

:white_check_mark: Non-breaking upgrade resolves issues for this component

<#else>
### :thinking: Sonatype Lifecycle found <#if ( policiesViolatedCount > 1 )>policy violations<#else>a policy violation</#if> introduced by this change.<#lt>

<#if suggestedVersion?has_content>
  :shield: **Bumping to version ${suggestedVersion}** will resolve all policy violations for this component<#if ( remediationForDependencies )> and its dependencies</#if> (as of _${date}_)<#lt>
  <@breakingChanges count=breakingChangesCount minimalMarkdown=true /><#lt>
<#else>
  :warning: No recommended versions are available for this component (as of _${date}_)<#lt>
</#if>
</#if>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list policiesViolated as policy>
  ${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
    **${constraint.constraintName}:** <#t>
    <#list constraint.conditions as condition>
      ${condition?replace("*", "\\*")} <#t>
    </#list>
    <#t>
  </#list>

</#list>
