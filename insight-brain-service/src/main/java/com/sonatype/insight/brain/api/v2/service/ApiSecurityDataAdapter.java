/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.SecurityThreatLevel;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueAnalysisDTO;
import com.sonatype.insight.brain.api.v2.dto.SecurityVulnerabilityCustomDataDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.BaseUrl;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @since 1.13.0
 */
@Named
public class ApiSecurityDataAdapter
{
  private BaseUrl baseUrl;

  @Inject
  public ApiSecurityDataAdapter(BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Convert a {@link Component}'s security vulnerabilities to the raw-report DTO. Overload that
   * retains pre-CLM-32049 semantics: no {@code customData} emitted. Retained so existing callers
   * (e.g. {@code ApiComponentDetailsAdapter}) compile unchanged.
   */
  public ApiSecurityDataDTO convertToDTO(final Component component) {
    return convertToDTO(component, false);
  }

  /**
   * Convert a {@link Component}'s security vulnerabilities to the raw-report DTO. When
   * {@code includeCustomSecurityVulnerabilityData} is {@code true} AND the underlying
   * {@link SecurityVulnerability} has any non-null {@link SecurityVulnerabilityCustomData} field,
   * populate {@link ApiSecurityIssueDTO#customData}; otherwise leave it {@code null}.
   *
   * @since 1.204.0
   */
  public ApiSecurityDataDTO convertToDTO(
      final Component component,
      final boolean includeCustomSecurityVulnerabilityData)
  {
    ApiSecurityDataDTO securityData = new ApiSecurityDataDTO();
    for (SecurityVulnerability vuln : component.getSecurityVulnerabilities()) {
      ApiSecurityIssueDTO sv = new ApiSecurityIssueDTO();
      sv.source = vuln.getSource();
      sv.reference = vuln.getRefId();
      sv.severity = vuln.getSeverity();
      sv.status = vuln.getStatus().getName();
      sv.url = getSecurityUrl(vuln.getUrl(), vuln.getSource(), vuln.getRefId());
      sv.threatCategory = SecurityThreatLevel.getBySeverity(vuln.getSeverity()).getName();
      sv.cwe = vuln.getCwe();
      sv.cvssVectorSource = vuln.getVectorSource();
      sv.cvssVector = vuln.getVector();
      if (CollectionUtils.isNotEmpty(vuln.getAliases())) {
        sv.vulnIds = new ArrayList<>(vuln.getAliases());
      }

      ThirdPartyVulnerabilityExploitabilityExchange analysis = vuln.getAnalysis();
      if (analysis != null) {
        ApiSecurityIssueAnalysisDTO apiSecurityIssueAnalysisDTO = new ApiSecurityIssueAnalysisDTO();
        apiSecurityIssueAnalysisDTO.state = analysis.getState();
        apiSecurityIssueAnalysisDTO.justification = analysis.getJustification();
        apiSecurityIssueAnalysisDTO.response = analysis.getResponse();
        apiSecurityIssueAnalysisDTO.detail = analysis.getDetail();
        sv.analysis = apiSecurityIssueAnalysisDTO;
      }

      if (includeCustomSecurityVulnerabilityData) {
        SecurityVulnerabilityCustomDataDTO customDataDto = buildCustomDataDto(
            vuln.getSecurityVulnerabilityCustomData());
        if (customDataDto != null) {
          sv.customData = customDataDto;
        }
      }

      securityData.securityIssues.add(sv);
    }

    return securityData;
  }

  /**
   * @return a populated DTO when at least one field is non-null; {@code null} otherwise so the
   *         caller can leave {@code customData} absent and rely on {@code @JsonInclude(NON_NULL)}
   *         to omit the JSON key.
   */
  private SecurityVulnerabilityCustomDataDTO buildCustomDataDto(SecurityVulnerabilityCustomData cd) {
    if (cd == null) {
      return null;
    }
    if (cd.getRemediation() == null
        && cd.getCweId() == null
        && cd.getCvssVector() == null
        && cd.getCvssSeverity() == null)
    {
      return null;
    }
    SecurityVulnerabilityCustomDataDTO dto = new SecurityVulnerabilityCustomDataDTO();
    dto.remediation = cd.getRemediation();
    dto.cweId = cd.getCweId();
    dto.cvssVector = cd.getCvssVector();
    dto.cvssSeverity = cd.getCvssSeverity();
    return dto;
  }

  /**
   * @since 1.16.0
   */
  public ApiSecurityDataDTO convertToDTO(ComponentEvaluationData componentDetailsFromHds) {
    ApiSecurityDataDTO securityData = new ApiSecurityDataDTO();
    for (com.sonatype.clm.dto.model.SecurityVulnerability vuln : componentDetailsFromHds.securityVulnerabilities) {
      ApiSecurityIssueDTO sv = new ApiSecurityIssueDTO();
      sv.source = vuln.getSource();
      sv.reference = vuln.getRefId();
      sv.severity = vuln.getSeverity();
      sv.status = SecurityVulnerabilityOverrideStatus.OPEN.getName();
      sv.url = getSecurityUrl(vuln.getUrl(), vuln.getSource(), vuln.getRefId());
      sv.threatCategory = SecurityThreatLevel.getBySeverity(vuln.getSeverity()).getName();
      if (CollectionUtils.isNotEmpty(vuln.getAliases())) {
        sv.vulnIds = new ArrayList<>(vuln.getAliases());
      }
      securityData.securityIssues.add(sv);
    }

    return securityData;
  }

  private String getSecurityUrl(String url, String source, String refId) {
    if (url != null) {
      return url;
    }
    if ("sonatype".equals(source)) {
      return baseUrl.get() + UserInterfaceLinksHelper.getVulnerabilityDetailsUrl(refId);
    }
    return null;
  }
}
