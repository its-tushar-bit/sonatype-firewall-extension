/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.SecurityThreatLevel;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueAnalysisDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.BaseUrl;

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

  public ApiSecurityDataDTO convertToDTO(final Component component) {
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

      ThirdPartyVulnerabilityExploitabilityExchange analysis = vuln.getAnalysis();
      if (analysis != null) {
        ApiSecurityIssueAnalysisDTO apiSecurityIssueAnalysisDTO = new ApiSecurityIssueAnalysisDTO();
        apiSecurityIssueAnalysisDTO.state = analysis.getState();
        apiSecurityIssueAnalysisDTO.justification = analysis.getJustification();
        apiSecurityIssueAnalysisDTO.response = analysis.getResponse();
        apiSecurityIssueAnalysisDTO.detail = analysis.getDetail();
        sv.analysis = apiSecurityIssueAnalysisDTO;
      }

      securityData.securityIssues.add(sv);
    }

    return securityData;
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
