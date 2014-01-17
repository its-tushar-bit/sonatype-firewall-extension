/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

import java.util.List;
import java.util.Map;

/**
 * Application policy violations trending report
 * 
 * @since 1.7
 */
public class TrendingReport
{
  private TrendingReportMetadata meta;
  private TrendingReportGenerationMetadata generation;
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
   * Returns general metadata about this report
   * 
   * @since 1.7
   */
  public TrendingReportMetadata getMeta() {
    return meta;
  }

  /**
   * Returns summary information about components in all applications at the end of reporting period.
   * 
   * @since 1.7
   */
  public ComponentsSummary getComponents() {
    return components;
  }

  /**
   * Returns summary information about applications at the end of reporting period.
   * 
   * @since 1.7
   */
  public Applications getApplications() {
    return applications;
  }

  /**
   * Returns information about policy violations detected during reporting period.
   * 
   * @since 1.7
   */
  public List<PolicyViolation> getViolations() {
    return violations;
  }

  /**
   * Returns information about partially matched component at the end of reporting period.
   * 
   * @since 1.7
   */
  public List<PartialMatch> getPartialMatches() {
    return partialMatches;
  }

  /**
   * Returns before/after number of violations grouped by threat category.
   * 
   * @see com.sonatype.insight.brain.trending.TrendingReportProcessor#CATEGORIES
   * @since 1.7
   */
  public Map<String, List<DiffData>> getDiffData() {
    return diffData;
  }

  /**
   * Returns component risk summary grouped by threat category, within each group component with
   * highest risk first, lowest risk last.
   * 
   * @see com.sonatype.insight.brain.trending.TrendingReportProcessor#CATEGORIES
   * @since 1.7
   */
  public Map<String, List<ComponentRiskSummary>> getTopPolicyViolations() {
    return topPolicyViolations;
  }

  /**
   * Returns policy violations summary at the end of reporting period.
   * 
   * @since 1.7
   */
  public PoliciesSummary getPolicies() {
    return policies;
  }

  public void setGeneration(TrendingReportGenerationMetadata generation) {
    this.generation = generation;
  }

  public TrendingReportGenerationMetadata getGeneration() {
    return generation;
  }
}
