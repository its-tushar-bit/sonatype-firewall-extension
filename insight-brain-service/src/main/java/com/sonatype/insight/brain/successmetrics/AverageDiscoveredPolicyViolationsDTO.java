/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

/**
 * @since 1.33
 */
public class AverageDiscoveredPolicyViolationsDTO
{
  public double evaluationCount;

  public ThreatCategoryPolicyViolationsDTO totalViolations;

  public ThreatCategoryPolicyViolationsDTO securityViolations;

  public ThreatCategoryPolicyViolationsDTO licenseViolations;

  public ThreatCategoryPolicyViolationsDTO qualityViolations;

  public ThreatCategoryPolicyViolationsDTO otherViolations;

  static class ThreatCategoryPolicyViolationsDTO
  {
    public double averageDiscovered;

    public double averageDiscoveredCritical;

    public ThreatCategoryPolicyViolationsDTO() {
    }

    public ThreatCategoryPolicyViolationsDTO(double averageDiscovered, double averageDiscoveredCritical) {
      this.averageDiscovered = averageDiscovered;
      this.averageDiscoveredCritical = averageDiscoveredCritical;
    }
  }

  public AverageDiscoveredPolicyViolationsDTO() {
    this.totalViolations = new ThreatCategoryPolicyViolationsDTO();
    this.securityViolations = new ThreatCategoryPolicyViolationsDTO();
    this.licenseViolations = new ThreatCategoryPolicyViolationsDTO();
    this.qualityViolations = new ThreatCategoryPolicyViolationsDTO();
    this.otherViolations = new ThreatCategoryPolicyViolationsDTO();
  }

  public AverageDiscoveredPolicyViolationsDTO(
      double evaluationCount,
      ThreatCategoryPolicyViolationsDTO totalViolations,
      ThreatCategoryPolicyViolationsDTO securityViolations,
      ThreatCategoryPolicyViolationsDTO licenseViolations,
      ThreatCategoryPolicyViolationsDTO qualityViolations,
      ThreatCategoryPolicyViolationsDTO otherViolations)
  {
    this.evaluationCount = evaluationCount;

    this.totalViolations = totalViolations;
    this.securityViolations = securityViolations;
    this.licenseViolations = licenseViolations;
    this.qualityViolations = qualityViolations;
    this.otherViolations = otherViolations;
  }
}
