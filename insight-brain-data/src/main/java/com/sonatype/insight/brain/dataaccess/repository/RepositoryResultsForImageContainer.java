/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

public class RepositoryResultsForImageContainer
{
  public Integer policyThreatLevel;

  public String policyName;

  public Integer violationCount;

  public Date quarantineTime;

  public String objectName;

  public String scanId;

  public String applicationPublicId;

  public RepositoryResultsForImageContainer(
      final Integer policyThreatLevel,
      final String policyName,
      final Integer violationCount,
      final String objectName,
      final Date quarantineTime,
      final String scanId,
      final String applicationPublicId)
  {
    this.policyThreatLevel = policyThreatLevel;
    this.policyName = policyName;
    this.violationCount = violationCount;
    this.objectName = objectName;
    this.quarantineTime = quarantineTime;
    this.scanId = scanId;
    this.applicationPublicId = applicationPublicId;
  }
}
