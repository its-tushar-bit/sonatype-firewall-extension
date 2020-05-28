### :thinking: Nexus IQ found <#if ( policiesViolatedCount > 1 )>policy violations<#else>a policy violation</#if> introduced by:<#lt>

<details open>
  <summary title="Threat Level: ${threatLevel} of 10"><img alt="T${threatLevel}" src="http://cdn.sonatype.com/iq-for-scm/1.0/${threatImage}">
    <b>${threatLevel}&nbsp;&nbsp;&nbsp; ${componentNameAndVersion}</b></summary>
<p></p>

<#if suggestedVersion?has_content>
  :shield: **Bumping to version ${suggestedVersion}** will resolve <#if ( policiesViolatedCount > 1 )>these violations<#else>this violation</#if><#t>
<#else>
  :warning: No recommended versions are available for this component<#t>
</#if> (as of _${date}_)

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
<#list policiesViolated as policy>
  ${policy.threatLevel} | ${policy.name} | <#list policy.constraints as constraint><#t>
    <b>${constraint.constraintName}:</b><ul><#t>
    <#list constraint.conditions as condition>
      <li>${condition}</li><#t>
    </#list>
    </ul><#lt>
  </#list>
</#list>

</details>
