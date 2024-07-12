<#include "iq-for-scm-common.ftl">

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


<#macro imageComponent mdImage="" imgWidth=0 imgHeight=0>
  <#compress>
<a href="#;"> <img title="${mdImage.title}" alt="${mdImage.alt}" src="${mdImage.src}" <#if (imgWidth > 0)>width="${imgWidth}"</#if> <#if (imgHeight > 0)>height="${imgHeight}"</#if>/></a>
  </#compress>
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
<br><br><@imageComponent mdImage=data.severityInfo.verificationImage imgWidth=900/>
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

<#macro issueComponent data>
    <#compress>
<#if data.severityInfo?hasContent>[${data.severityInfo.refId}]<#else>None</#if> <#if data.description?hasContent>${data.description}</#if>
    </#compress>
</#macro>
# <@threatLevelIndicatorComponent/> <@dependencyIndicatorComponent/> Sonatype IQ found issues introduced by ${componentDisplayName}<#if provider == "github" || provider == "gitlab"><br /><@imageComponent mdImage=previewImage imgWidth=70 imgHeight=20/></#if>
<details>
<br/>

Threat Level: <strong>${threatLevelDisplay.image.alt} (${threatLevelDisplay.value})</strong> \| [View Component Details in Sonatype Lifecycle](${componentDetailLink})

<#if (hasSecurityIssues)>
## :shield: Recommendation
<#if suggestedVersion?hasContent>
  **Bumping to version ${suggestedVersion}** will resolve all policy violations for this component<#if ( hasRemediationForDependencies )> and its dependencies</#if> (as of _${formattedDate}_)<#lt>
    <@breakingChangesComponent count=breakingChangesCount/><#lt>
<#else>
  No recommended versions are available for this component (as of _${formattedDate}_)<#lt>
</#if>

## :page_facing_up: Security Issue Details

<details>
  <summary title="View all (${securityIssues?size})">
    View all (${securityIssues?size})
  </summary>
  <p></p>

| **Severity** | **Issue** | **Organization Policy Violation** |
| --- | --- | --- |
<#list securityIssues as securityIssue>
| <@severityComponent data=securityIssue/> | <@issueComponent data=securityIssue/> | [View Details](${securityIssue.policyViolationDetailsLink}) <br /><img src="https://cdn.sonatype.com/iq-for-scm/1.0/Filler.svg" width="600" height="0" display="hidden">|
</#list>
</details>
</#if>
</details>

<#if codeSuggestion?hasContent>
```suggestion
${codeSuggestion}
```
</#if>

