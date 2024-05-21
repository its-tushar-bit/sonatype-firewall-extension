/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

public class FirewallQuarantinedComponentDetails
{
  public Integer threatLevel;

  public String policyName;

  public boolean quarantined;

  public String componentIdFormat;

  public String componentIdCoordinatesJson;

  public String pathname;

  public String displayName;

  public String repositoryId;

  public String repositoryName;

  public String hash;

  public String matchState;

  public Date quarantineDate;

  public FirewallQuarantinedComponentDetails(
      final Integer threatLevel,
      final String policyName,
      final boolean quarantined,
      final String componentIdFormat,
      final String componentIdCoordinatesJson,
      final String pathname,
      final String displayName,
      final String repositoryId,
      final String repositoryName,
      final String hash,
      final String matchState,
      final Date quarantineDate)
  {
    this.threatLevel = threatLevel;
    this.policyName = policyName;
    this.quarantined = quarantined;
    this.componentIdFormat = componentIdFormat;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.pathname = pathname;
    this.displayName = displayName;
    this.repositoryId = repositoryId;
    this.repositoryName = repositoryName;
    this.hash = hash;
    this.matchState = matchState;
    this.quarantineDate = quarantineDate;
  }
}
