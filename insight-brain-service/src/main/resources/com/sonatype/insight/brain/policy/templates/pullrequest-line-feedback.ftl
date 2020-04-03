###  🤔 Nexus IQ found <#if ( policiesViolatedCount > 1 )>policy violations<#else>a policy violation</#if> introduced by<#lt>
### ${componentNameAndVersion}

<#if suggestedVersion?has_content>
  :shield: **Bumping to version ${suggestedVersion}** will resolve <#if ( policiesViolatedCount > 1 )>these violations<#else>this violation</#if><#t>
<#else>
  ⚠️ **No recommended versions** are available for this component<#t>
</#if> (as of _${date}_)
<p>


Threat (of 10) | Policy | Violation Details
--- | --- | --- |
  <#list policiesViolated as policy>
  ${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
<b>${constraint.constraintName}:</b><ul><#t>
  <#list constraint.conditions as condition>
    <li>${condition}</li><#t>
  </#list>
</ul><#t>
  </#list>

</#list>
</p>
