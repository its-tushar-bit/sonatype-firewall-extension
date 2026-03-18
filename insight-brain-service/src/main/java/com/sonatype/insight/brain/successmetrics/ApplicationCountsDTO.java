/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

public class ApplicationCountsDTO
{
  public int totalApplications;

  public int activeApplications;

  public ThreatCategoryApplicationCount total;

  public ThreatCategoryApplicationCount security;

  public ThreatCategoryApplicationCount license;

  public ThreatCategoryApplicationCount quality;

  public ThreatCategoryApplicationCount other;

  static class ThreatCategoryApplicationCount
  {
    public int applicationsWithViolations;

    public int applicationsWithCriticalViolations;

    public ThreatCategoryApplicationCount() {
    }

    public ThreatCategoryApplicationCount(
        int applicationsWithViolations,
        int applicationsWithCriticalViolations)
    {
      this.applicationsWithViolations = applicationsWithViolations;
      this.applicationsWithCriticalViolations = applicationsWithCriticalViolations;
    }
  }

  public ApplicationCountsDTO() {
    this.total = new ThreatCategoryApplicationCount();
    this.security = new ThreatCategoryApplicationCount();
    this.license = new ThreatCategoryApplicationCount();
    this.quality = new ThreatCategoryApplicationCount();
    this.other = new ThreatCategoryApplicationCount();
  }

  public ApplicationCountsDTO(
      int totalApplications,
      int activeApplications,
      ThreatCategoryApplicationCount total,
      ThreatCategoryApplicationCount security,
      ThreatCategoryApplicationCount license,
      ThreatCategoryApplicationCount quality,
      ThreatCategoryApplicationCount other)
  {
    this.totalApplications = totalApplications;
    this.activeApplications = activeApplications;
    this.total = total;
    this.security = security;
    this.license = license;
    this.quality = quality;
    this.other = other;
  }
}
