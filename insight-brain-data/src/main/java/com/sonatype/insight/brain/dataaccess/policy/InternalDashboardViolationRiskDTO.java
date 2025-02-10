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
  public final String applicationId;

  public final String applicationName;

  public final String organizationName;

  public final String policyViolationId;

  public final String policyName;

  public final int threatLevel;

  public final String hash;

  public final String filename;

  public final String componentIdFormat;

  public final String componentIdCoordinatesJson;

  public final String constraintFactsJson;

  public final long firstOccurrenceTime;

  public final String autoPolicyWaiverId;

  InternalDashboardViolationRiskDTO(
      String applicationId,
      String applicationName,
      String organizationName,
      String policyViolationId,
      String policyName,
      int threatLevel,
      String hash,
      String filename,
      String componentIdFormat,
      String componentIdCoordinatesJson,
      String constraintFactsJson,
      long firstOccurrenceTime,
      String autoPolicyWaiverId)
  {
    this.applicationId = applicationId;
    this.applicationName = applicationName;
    this.organizationName = organizationName;
    this.policyViolationId = policyViolationId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.hash = hash;
    this.filename = filename;
    this.componentIdFormat = componentIdFormat;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.constraintFactsJson = constraintFactsJson;
    this.firstOccurrenceTime = firstOccurrenceTime;
    this.autoPolicyWaiverId = autoPolicyWaiverId;
  }
}
