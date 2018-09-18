/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.utils.ThreatLevel;

/**
 * @since 1.52
 */
public class ApiMetricsReportingAggregationDTOV2
{
  // ISO 8601 date (without time)
  public String timePeriodStart;

  public Long mttrLowThreat;

  public Long mttrModerateThreat;

  public Long mttrSevereThreat;

  public Long mttrCriticalThreat;

  public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discoveredCounts;

  public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixedCounts;

  public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waivedCounts;

  public int evaluationCount;

  public int openCountSecurity;

  public int openCountLicense;

  public int openCountQuality;

  public int openCountOther;

  public ApiMetricsReportingAggregationDTOV2(String timePeriodStart,
                                             Long mttrLowThreat,
                                             Long mttrModerateThreat,
                                             Long mttrSevereThreat,
                                             Long mttrCriticalThreat,
                                             Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discoveredCounts,
                                             Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixedCounts,
                                             Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waivedCounts,
                                             int evaluationCount,
                                             int openCountSecurity,
                                             int openCountLicense,
                                             int openCountQuality,
                                             int openCountOther)
  {
    this.timePeriodStart = timePeriodStart;
    this.mttrLowThreat = mttrLowThreat;
    this.mttrModerateThreat = mttrModerateThreat;
    this.mttrSevereThreat = mttrSevereThreat;
    this.mttrCriticalThreat = mttrCriticalThreat;
    this.discoveredCounts = discoveredCounts;
    this.fixedCounts = fixedCounts;
    this.waivedCounts = waivedCounts;
    this.evaluationCount = evaluationCount;
    this.openCountSecurity = openCountSecurity;
    this.openCountLicense = openCountLicense;
    this.openCountQuality = openCountQuality;
    this.openCountOther = openCountOther;
  }
}
