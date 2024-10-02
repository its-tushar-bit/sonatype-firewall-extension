<#--
  Thresholds similar to the ones in version graph:
  ref: https://github.com/sonatype/version-graph/blob/38946c80fe6a7805579f56a850902c9c45f67d4b/src/index.js#L153
-->
<#assign criticalBreakingChangesThreshold = 5 >
<#assign moderateBreakingChangesThreshold = 2 >
<#assign fewBreakingChangesThreshold = 0 >

<#assign recommendedNonBreakingWithDependencies = "recommended-non-breaking-with-dependencies" >

<#--
  Breaking Changes macro
  Parameters:
  - count - breaking changes count: non-negative integer, or null
  - minimalMarkdown - boolean indicating if the template supports only minimal markdown (i.e. no inline images) or not
-->
<#macro breakingChanges count minimalMarkdown width=12>
  <#if minimalMarkdown>
    <#if (count > criticalBreakingChangesThreshold)>
  - Multiple breaking changes - This version upgrade may require significant effort.
    <#elseif (count > fewBreakingChangesThreshold)>
  - Few breaking changes - This version upgrade may require moderate effort.
    <#elseif (count == 0)>
  - No breaking changes - This version upgrade requires minimal effort.
    </#if>
  <#else>
    <#if (count > criticalBreakingChangesThreshold)>
  - <img alt="Multiple breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/red-bar.png" width="${width}" height="${width}">&nbsp; Multiple breaking changes - This version upgrade may require significant effort.
    <#elseif (count > moderateBreakingChangesThreshold)>
  - <img alt="Few breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/orange-bar.png" width="${width}" height="${width}">&nbsp; Few breaking changes - This version upgrade may require moderate effort.
    <#elseif (count > fewBreakingChangesThreshold)>
  - <img alt="Few breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/yellow-bar.png" width="${width}" height="${width}">&nbsp; Few breaking changes - This version upgrade may require moderate effort.
    <#elseif (count == 0)>
  - <img alt="No breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/dark-blue-bar.png" width="${width}" height="${width}">&nbsp; No breaking changes - This version upgrade requires minimal effort.
    </#if>
  </#if>
</#macro>

<#macro breakingChangesWithRemediationType remediationType minimalMarkdown width=12>
  <#if minimalMarkdown>
    <#if (remediationType == recommendedNonBreakingWithDependencies)>
      <#lt>:white_check_mark: No breaking changes - This version upgrade requires minimal effort.
    </#if>
  <#else>
    <#if (remediationType == recommendedNonBreakingWithDependencies)>
      <#lt>&nbsp;<img alt="green_checkmark" src="https://cdn.sonatype.com/iq-for-scm/1.0/green-check-mark.png" width="${width}" height="${width}">&nbsp; No breaking changes - This version upgrade requires minimal effort.
    </#if>
  </#if>
</#macro>
