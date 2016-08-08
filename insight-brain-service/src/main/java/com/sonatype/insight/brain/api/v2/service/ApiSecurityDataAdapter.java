/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Named;

import com.sonatype.clm.dto.model.SecurityThreatLevel;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;

/**
 * @since 1.13.0
 */
@Named
public class ApiSecurityDataAdapter
{
  public ApiSecurityDataDTO convertToDTO(final Component component) {
    ApiSecurityDataDTO securityData = new ApiSecurityDataDTO();
    for (SecurityVulnerability vuln : component.getSecurityVulnerabilities()) {
      ApiSecurityIssueDTO sv = new ApiSecurityIssueDTO();
      sv.source = vuln.getSource();
      sv.reference = vuln.getRefId();
      sv.severity = vuln.getSeverity();
      sv.status = vuln.getStatus().getName();
      sv.url = vuln.getUrl();
      sv.threatCategory = SecurityThreatLevel.getBySeverity(vuln.getSeverity()).getName();
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
      sv.url = vuln.getUrl();
      sv.threatCategory = SecurityThreatLevel.getBySeverity(vuln.getSeverity()).getName();
      securityData.securityIssues.add(sv);
    }

    return securityData;
  }
}
