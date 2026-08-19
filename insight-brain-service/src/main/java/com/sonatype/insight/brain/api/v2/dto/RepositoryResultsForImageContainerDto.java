/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainer;

public class RepositoryResultsForImageContainerDto
{
  public Integer threatLevel;

  public String policyName;

  public Integer violationCount;

  public String scanId;

  public String objectName;

  public Date quarantineTime;

  public String applicationPublicId;

  public RepositoryResultsForImageContainerDto() {
  }

  public RepositoryResultsForImageContainerDto(final RepositoryResultsForImageContainer details) {
    this.threatLevel = details.policyThreatLevel;
    this.policyName = details.policyName;
    this.violationCount = details.violationCount == null ? 0 : details.violationCount;
    this.objectName = details.objectName;
    this.quarantineTime = details.quarantineTime;
    this.scanId = details.scanId;
    this.applicationPublicId = details.applicationPublicId;
  }
}
