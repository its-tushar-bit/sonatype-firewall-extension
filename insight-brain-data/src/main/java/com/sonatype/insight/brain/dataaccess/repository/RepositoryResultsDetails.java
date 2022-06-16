/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

public class RepositoryResultsDetails
{
  public Integer policyThreatLevel;

  public String policyName;

  public String componentIdFormat;

  public String pathname;

  public String componentIdCoordinatesJson;

  public Date quarantineTime;

  public Boolean waived;

  public RepositoryResultsDetails(
      final Integer policyThreatLevel,
      final String policyName,
      final String componentIdFormat,
      final String pathname,
      final String componentIdCoordinatesJson,
      final Date quarantineTime,
      final Boolean waived)
  {
    this.policyThreatLevel = policyThreatLevel;
    this.policyName = policyName;
    this.componentIdFormat = componentIdFormat;
    this.pathname = pathname;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.quarantineTime = quarantineTime;
    this.waived = waived;
  }
}
