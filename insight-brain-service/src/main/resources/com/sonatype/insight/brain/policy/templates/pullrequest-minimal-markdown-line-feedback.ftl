<#include "iq-for-scm-common.ftl">
### :thinking: Nexus IQ found <#if ( policiesViolatedCount > 1 )>policy violations<#else>a policy violation</#if> introduced by this change.<#lt>

<#if suggestedVersion?has_content>
  :shield: **Bumping to version ${suggestedVersion}** will resolve all policy violations for this component<#if ( remediationForDependencies )> and its dependencies</#if> (as of _${date}_)<#lt>
  <@breakingChanges count=breakingChangesCount minimalMarkdown=true /><#lt>
<#else>
  :warning: No recommended versions are available for this component (as of _${date}_)<#lt>
</#if>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list policiesViolated as policy>
  ${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
    **${constraint.constraintName}:** <#t>
    <#list constraint.conditions as condition>
      ${condition} <#t>
    </#list>
    <#t>
  </#list>

</#list>
