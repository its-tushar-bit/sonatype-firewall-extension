<#include "iq-for-scm-common.ftl">
<#if provider.name() == "GITLAB"><#assign width=14><#else><#assign width=12></#if>
### :thinking: Nexus IQ found <#if ( policiesViolatedCount > 1 )>policy violations<#else>a policy violation</#if> introduced by:<#lt>

<details open>
  <summary title="Threat Level: ${threatLevel} of 10"><img alt="T${threatLevel}" src="https://cdn.sonatype.com/iq-for-scm/1.0/${threatImage}"<#if provider.name() == "GITLAB"> width="4" height="16"</#if>>
    <b>${threatLevel}&nbsp;&nbsp;&nbsp; ${componentNameAndVersion}</b></summary>
<p></p>

<#if suggestedVersion?has_content>
  :shield: **Bumping to version ${suggestedVersion}** will resolve all policy violations for this component<#if ( remediationForDependencies )> and its dependencies</#if> (as of _${date}_)<#lt>
  <@breakingChanges count=breakingChangesCount minimalMarkdown=false width=width/><#lt>
<#else>
  :warning: No recommended versions are available for this component (as of _${date}_)<#lt>
</#if>

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

<#-- Remove provider check if possible. Depends on completion of
https://sonatype.atlassian.net/browse/SDEV-147
https://sonatype.atlassian.net/browse/SDEV-211
https://sonatype.atlassian.net/browse/SDEV-207
https://sonatype.atlassian.net/browse/SDEV-213
-->
<#if provider.name() == "GITHUB" && componentDetailsReportUrl?has_content>
[Component detail 🔍](${componentDetailsReportUrl})
</#if>
</details>
