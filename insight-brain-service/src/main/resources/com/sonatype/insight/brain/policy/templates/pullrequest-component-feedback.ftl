<#include "iq-for-scm-common.ftl">
<#assign recommendedNonBreaking = "recommended-non-breaking" >
<#assign recommendedNonBreakingWithDependencies = "recommended-non-breaking-with-dependencies" >

<#if provider == "gitlab"><#assign width=14><#else><#assign width=20></#if>

<#macro breakingChangesComponent count>
      <#if (count > criticalBreakingChangesThreshold)>
There are multiple **breaking changes**. This version upgrade may require significant effort.
      <#elseIf (count > fewBreakingChangesThreshold)>
There are a few breaking changes - This version upgrade may require moderate effort.
      <#elseIf (count == 0)>
There are no breaking changes. This version upgrade requires minimal effort.
      </#if>
</#macro>

<#macro dependencyIndicatorComponent>
  <#compress>
<@imageComponent mdImage=dependencyImage imgWidth=width imgHeight=width/>
  </#compress>
</#macro>

<#macro threatLevelIndicatorComponent>
  <#compress>
<@imageComponent mdImage=threatLevelDisplay.image imgWidth=width imgHeight=width/>
  </#compress>
</#macro>

<#macro verificationIndicatorComponent data>
  <#compress>
      <#if data.severityInfo?hasContent && data.severityInfo.verificationImage?hasContent>
<br><br><@imageComponent mdImage=data.severityInfo.verificationImage imgWidth=150/>
      </#if>
  </#compress>
</#macro>

<#macro cvssScoreComponent data>
    <#compress>
        <#if data.severityInfo?hasContent && data.severityInfo.cvssScore?hasContent>
<b>CVSS Score:</b> ${data.severityInfo.cvssScore}
        <#else >
<b>CVSS Score:</b> N/A
        </#if>
    </#compress>
</#macro>

<#macro severityComponent data>
  <#compress>
<b>Threat level:</b> ${data.threatLevel}<br><@cvssScoreComponent data=data/><@verificationIndicatorComponent data=data/>
  </#compress>
</#macro>

<#macro insertBreaks text x>
    <#assign content = text?replace("\n","<br>")>
    <#assign words = content?split(" ")>
    <#assign result = []>
    <#list words as word>
        <#if (word?index + 1) % x == 0>
            <#assign result += [word, "<br>"]>
        <#else>
            <#assign result += [word]>
        </#if>
    </#list>
    ${result?join(" ")}
</#macro>

<#macro issueComponent data>
    <#compress>
<p><#if data.severityInfo?hasContent>[${data.severityInfo.refId}]<#else>None</#if></p><#if data.description?hasContent><@insertBreaks text=data.description x=10/></#if>
    </#compress>
</#macro>

<#macro imageComponent mdImage="" imgWidth=0 imgHeight=0>
  <#compress>
<a href="#;"> <img title="${mdImage.title}" alt="${mdImage.alt}" src="${mdImage.src}" <#if (imgWidth > 0)>width="${imgWidth}"</#if> <#if (imgHeight > 0)>height="${imgHeight}"</#if>/></a>
  </#compress>
</#macro>

<#macro breakingChangesComponent count>
      <#if (count > criticalBreakingChangesThreshold)>
There are multiple **breaking changes**. This version upgrade may require significant effort.
      <#elseIf (count > fewBreakingChangesThreshold)>
There are a few breaking changes - This version upgrade may require moderate effort.
      <#elseIf (count == 0)>
There are no breaking changes. This version upgrade requires minimal effort.
      </#if>
</#macro>

<#if suggestedVersionType == recommendedNonBreakingWithDependencies>
### Sonatype Lifecycle found critical issues introduced by ${componentDisplayName}

Direct dependency | **Threat level: ${threatLevelDisplay.value}** \| [View Component Details in Sonatype Lifecycle](${componentDetailLink})

<img src="https://cdn.sonatype.com/iq-for-scm/1.0/golden-pr-inline.png" width="16" height="16" alt="star icon"> <strong>Golden Version: ${suggestedVersion}</strong>

<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; Non-breaking upgrade resolves issues for this component and its dependencies

<details>
  <summary>View Security Details</summary>
  
<#elseIf suggestedVersionType == recommendedNonBreaking>
### Sonatype Lifecycle found critical issues introduced by ${componentDisplayName}

Direct dependency | **Threat level: ${threatLevelDisplay.value}** \| [View Component Details in Sonatype Lifecycle](${componentDetailLink})

:shield: **Recommended Version: ${suggestedVersion}**

<img src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="12" height="12" alt="green checkmark icon">&nbsp; Non-breaking upgrade resolves issues for this component

<details>
  <summary>View Security Details</summary>

<#else>
# <@threatLevelIndicatorComponent/> <@dependencyIndicatorComponent/> Sonatype Lifecycle found issues introduced by ${componentDisplayName}
<details>
<br/>

Threat Level: <strong>${threatLevelDisplay.image.alt} (${threatLevelDisplay.value})</strong> \| [View Component Details in Sonatype Lifecycle](${componentDetailLink})

</#if>
<#if (hasSecurityIssues)>
## :shield: Recommendation
<#if suggestedVersion?hasContent>
  **Bumping to version ${suggestedVersion}** will resolve all policy violations for this component<#if ( hasRemediationForDependencies )> and its dependencies</#if> (as of _${formattedDate}_)<#lt>
    <@breakingChangesComponent count=breakingChangesCount/><#lt>
<#else>
  No recommended versions are available for this component (as of _${formattedDate}_)<#lt>
</#if>

## :page_facing_up: Security Issue Details

<#if (hasReducedSecurityData)>
Found ${securityIssues?size} security vulnerabilities. [View Details](${componentDetailLink}).
<#else>
<details>
  <summary title="View all (${securityIssues?size})">
    View all (${securityIssues?size})
  </summary>
  <p></p>

| **Severity** | **Issue** | **Organization Policy Violation** |
| --- | --- | --- |
<#list securityIssues as securityIssue>
| <@severityComponent data=securityIssue/> | <@issueComponent data=securityIssue/> | [View Details](${securityIssue.policyViolationDetailsLink})|
</#list>
</details>
</#if>
</#if>
</details>

<#if codeSuggestion?hasContent>
```suggestion
${codeSuggestion}
```
</#if>
