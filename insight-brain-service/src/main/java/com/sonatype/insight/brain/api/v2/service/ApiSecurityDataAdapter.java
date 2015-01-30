/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Named;

import com.sonatype.insight.brain.api.v1.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;

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
      securityData.securityIssues.add(sv);
    }

    return securityData;
  }
}
