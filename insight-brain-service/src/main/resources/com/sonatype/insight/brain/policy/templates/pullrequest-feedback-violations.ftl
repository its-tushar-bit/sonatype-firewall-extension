<#if ( policiesViolatedCount > 0 )>
  ###  🤔 Nexus IQ found <#if ( policiesViolatedCount > 1 )>multiple policy violations<#else>a policy violation</#if><#lt>

<#list componentList as component>
  #### ${component.componentNameAndVersion}
  *${component.highestThreatLevel} of 10* Threat Level
  <details>
    <summary>Details</summary>
<p>

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
</p>
</details>

</#list>
<#else>
  ### 😃✨All Clear! Nexus IQ didn't find any new policy violations.<#lt>
  Well done. The committed code does not violate any of your organization's Nexus IQ policies.<#lt>

</#if>
<#if ( fixedPolicyViolationsCount > 0 )>
  #### 😃🏆 Nice work! You fixed <#if ( fixedPolicyViolationsCount > 1 )>multiple outstanding Nexus IQ policy violations<#else>an outstanding Nexus IQ policy violation</#if><#lt>

</#if>
----
### Nexus IQ Report Detail
**Application**: ${applicationName}
**Organization**: ${organizationName}
**Date**: ${date}
**Stage**: ${stage}

**[See full feature branch report](${detailedFeatureBranchReportUrl})**
**[See full default branch report](${detailedDefaultBranchReportUrl})**
