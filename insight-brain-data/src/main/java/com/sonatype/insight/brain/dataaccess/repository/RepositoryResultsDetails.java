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

  public String repositoryManagerId;

  public String repositoryId;

  public String componentIdFormat;

  public String pathname;

  public String componentIdCoordinatesJson;

  public String componentDisplayName;

  public String hash;

  public String matchStateId;

  public Date quarantineTime;

  public Boolean waived;

  /**
   * The raw constraint facts JSON for converting to PolicyConstraint in the service layer.
   *
   * @since 1.202
   */
  public String constraintFactsJson;

  /**
   * The policy violation ID for enabling direct waiver operations.
   *
   * @since 1.203
   */
  public String policyViolationId;

  public RepositoryResultsDetails(
      final Integer policyThreatLevel,
      final String policyName,
      final String repositoryManagerId,
      final String repositoryId,
      final String componentIdFormat,
      final String pathname,
      final String componentIdCoordinatesJson,
      final String componentDisplayName,
      final String hash,
      final String matchStateId,
      final Date quarantineTime,
      final Boolean waived)
  {
    this(policyThreatLevel, policyName, repositoryManagerId, repositoryId, componentIdFormat, pathname,
        componentIdCoordinatesJson, componentDisplayName, hash, matchStateId, quarantineTime, waived, null, null);
  }

  public RepositoryResultsDetails(
      final Integer policyThreatLevel,
      final String policyName,
      final String repositoryManagerId,
      final String repositoryId,
      final String componentIdFormat,
      final String pathname,
      final String componentIdCoordinatesJson,
      final String componentDisplayName,
      final String hash,
      final String matchStateId,
      final Date quarantineTime,
      final Boolean waived,
      final String constraintFactsJson,
      final String policyViolationId)
  {
    this.policyThreatLevel = policyThreatLevel;
    this.policyName = policyName;
    this.repositoryManagerId = repositoryManagerId;
    this.repositoryId = repositoryId;
    this.componentIdFormat = componentIdFormat;
    this.pathname = pathname;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.componentDisplayName = componentDisplayName;
    this.hash = hash;
    this.matchStateId = matchStateId;
    this.quarantineTime = quarantineTime;
    this.waived = waived;
    this.constraintFactsJson = constraintFactsJson;
    this.policyViolationId = policyViolationId;
  }
}
