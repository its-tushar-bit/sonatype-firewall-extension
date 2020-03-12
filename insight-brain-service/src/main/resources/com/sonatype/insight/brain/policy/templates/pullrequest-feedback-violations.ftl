## New Nexus IQ Policy Violation<#if (policiesViolatedCount > 1)>s</#if> found

<#list componentList as component>
  ### ${component.componentNameAndVersion}
  - **${component.highestThreatLevel} of 10** Threat Level
  <details>
<summary>See violations</summary>
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

### Nexus IQ Scan Detail
**Application**: ${applicationName}
**Organization**: ${organizationName}
**Date**: ${date}
**Stage**: ${stage}

**[Review full feature branch report](${detailedFeatureBranchReportUrl})**
**[Review full default branch report](${detailedDefaultBranchReportUrl})**
