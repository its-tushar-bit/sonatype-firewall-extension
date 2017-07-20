/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

public class ApplicationCountsDTO
{
  public int totalApplications;
  public int activeApplications;

  public ThreatCategoryApplicationCount total = new ThreatCategoryApplicationCount();
  public ThreatCategoryApplicationCount security = new ThreatCategoryApplicationCount();
  public ThreatCategoryApplicationCount license = new ThreatCategoryApplicationCount();
  public ThreatCategoryApplicationCount quality = new ThreatCategoryApplicationCount();
  public ThreatCategoryApplicationCount other = new ThreatCategoryApplicationCount();

  static class ThreatCategoryApplicationCount
  {
    public int applicationsWithViolations;
    public int applicationsWithCriticalViolations;

    public ThreatCategoryApplicationCount() {
    }

    public ThreatCategoryApplicationCount(int applicationsWithViolations,
                                          int applicationsWithCriticalViolations)
    {
      this.applicationsWithViolations = applicationsWithViolations;
      this.applicationsWithCriticalViolations = applicationsWithCriticalViolations;
    }
  }
}
