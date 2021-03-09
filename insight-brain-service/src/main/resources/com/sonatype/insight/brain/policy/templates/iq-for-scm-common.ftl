<#--
  Thresholds similar to the ones in version graph:
  ref: https://github.com/sonatype/version-graph/blob/38946c80fe6a7805579f56a850902c9c45f67d4b/src/index.js#L153
-->
<#assign criticalBreakingChangesThreshold = 5 >
<#assign moderateBreakingChangesThreshold = 2 >
<#assign fewBreakingChangesThreshold = 0 >

<#--
  Breaking Changes macro
  Parameters:
  - count - breaking changes count: non-negative integer, or null
  - minimalMarkdown - boolean indicating if the template supports only minimal markdown (i.e. no inline images) or not
-->
<#macro breakingChanges count minimalMarkdown>
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
  - <img alt="Multiple breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/red-bar.png" width="12" height="12">&nbsp; Multiple breaking changes - This version upgrade may require significant effort.
    <#elseif (count > moderateBreakingChangesThreshold)>
  - <img alt="Few breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/orange-bar.png" width="12" height="12">&nbsp; Few breaking changes - This version upgrade may require moderate effort.
    <#elseif (count > fewBreakingChangesThreshold)>
  - <img alt="Few breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/yellow-bar.png" width="12" height="12">&nbsp; Few breaking changes - This version upgrade may require moderate effort.
    <#elseif (count == 0)>
  - <img alt="No breaking changes" src="https://cdn.sonatype.com/iq-for-scm/1.0/dark-blue-bar.png" width="12" height="12">&nbsp; No breaking changes - This version upgrade requires minimal effort.
    </#if>
  </#if>
</#macro>
