/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.EnumIntegerTable;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.collect.Table;

class ResultsWrapper
{
  final MttrStats mttrStats;

  int evaluationCount;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts;

  public ResultsWrapper(Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts) {
    mttrStats = new MttrStats();
    evaluationCount = 0;
    discoveredCounts = new EnumIntegerTable<>(PolicyThreatCategory.class, ThreatLevel.class);
    fixedCounts = new EnumIntegerTable<>(PolicyThreatCategory.class, ThreatLevel.class);
    waivedCounts = new EnumIntegerTable<>(PolicyThreatCategory.class, ThreatLevel.class);

    this.openCounts = openCounts;
  }

  public ResultsWrapper(
      MttrStats mttrStats,
      int evaluationCount,
      Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts)
  {
    this.mttrStats = mttrStats;
    this.evaluationCount = evaluationCount;
    this.discoveredCounts = discoveredCounts;
    this.fixedCounts = fixedCounts;
    this.waivedCounts = waivedCounts;
    this.openCounts = openCounts;
  }
}
