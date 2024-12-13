/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

/**
 * Carries the data backing the Dashboard Violations tab.
 */
public class InternalDashboardViolationRiskDTO
{
  public final String applicationName;

  public final String organizationName;

  public final String policyViolationId;

  public final String policyName;

  public final int threatLevel;

  public final String hash;

  public final String filename;

  public final String componentIdFormat;

  public final String componentIdCoordinatesJson;

  public final String constraintFactsId;

  public final long firstOccurrenceTime;

  InternalDashboardViolationRiskDTO(
      String applicationName,
      String organizationName,
      String policyViolationId,
      String policyName,
      int threatLevel,
      String hash,
      String filename,
      String componentIdFormat,
      String componentIdCoordinatesJson,
      String constraintFactsId,
      long firstOccurrenceTime)
  {
    this.applicationName = applicationName;
    this.organizationName = organizationName;
    this.policyViolationId = policyViolationId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.hash = hash;
    this.filename = filename;
    this.componentIdFormat = componentIdFormat;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.constraintFactsId = constraintFactsId;
    this.firstOccurrenceTime = firstOccurrenceTime;
  }
}
