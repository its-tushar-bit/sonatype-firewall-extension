### :thinking_face: Nexus IQ found <#if ( policiesViolatedCount > 1 )>policy violations<#else>a policy violation</#if> introduced by this change.<#lt>

<#if suggestedVersion?has_content>
  :shield: **Bumping to version ${suggestedVersion}** will resolve <#if ( policiesViolatedCount > 1 )>these violations<#else>this violation</#if><#t>
<#else>
  :warning: No recommended versions are available for this component<#t>
</#if> (as of _${date}_)

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
