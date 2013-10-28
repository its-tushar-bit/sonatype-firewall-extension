/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

import java.util.List;
import java.util.Map;

public class TrendingReport
{
  private TrendingReportMetadata meta;
  private ComponentsSummary components;
  private Applications applications;
  private List<PolicyViolation> violations;
  private List<PartialMatch> partialMatches;
  private Map<String, List<DiffData>> diffData;
  private Map<String, List<ComponentRiskSummary>> topPolicyViolations;
  private PoliciesSummary policies;

  public TrendingReport() {
  }

  public TrendingReport(TrendingReportMetadata meta, ComponentsSummary components, Applications applications,
      List<PolicyViolation> violations, List<PartialMatch> partialMatches, Map<String, List<DiffData>> diffData,
      Map<String, List<ComponentRiskSummary>> topPolicyViolations, PoliciesSummary policies)
  {
    this.meta = meta;
    this.components = components;
    this.applications = applications;
    this.violations = violations;
    this.partialMatches = partialMatches;
    this.diffData = diffData;
    this.topPolicyViolations = topPolicyViolations;
    this.policies = policies;
  }

  /**
   * General metadata about this report
   */
  public TrendingReportMetadata getMeta() {
    return meta;
  }

  /**
   * Number of components in the most recent application report. Total for all applications.
   */
  public ComponentsSummary getComponents() {
    return components;
  }

  /**
   * Per application per threat level alert counts
   */
  public Applications getApplications() {
    return applications;
  }

  public List<PolicyViolation> getViolations() {
    return violations;
  }

  public List<PartialMatch> getPartialMatches() {
    return partialMatches;
  }

  public Map<String, List<DiffData>> getDiffData() {
    return diffData;
  }

  public Map<String, List<ComponentRiskSummary>> getTopPolicyViolations() {
    return topPolicyViolations;
  }

  public PoliciesSummary getPolicies() {
    return policies;
  }
}
