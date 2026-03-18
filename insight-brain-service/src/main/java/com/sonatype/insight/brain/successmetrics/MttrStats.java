/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * A container for DescriptiveStatistics objects for MTTRs at each threat level category
 */
class MttrStats
{
  final DescriptiveStatistics mttrLowThreatStats = new DescriptiveStatistics();

  final DescriptiveStatistics mttrModerateThreatStats = new DescriptiveStatistics();

  final DescriptiveStatistics mttrSevereThreatStats = new DescriptiveStatistics();

  final DescriptiveStatistics mttrCriticalThreatStats = new DescriptiveStatistics();

  void addViolation(
      PolicyViolationComparable violation,
      Date violationFirstOccurrenceTimestamp,
      Date violationResolutionTimestamp)
  {
    long timeToResolve = violationResolutionTimestamp.getTime() - violationFirstOccurrenceTimestamp.getTime();
    int threatLevel = violation.getThreatLevel();
    DescriptiveStatistics statsToUpdate;

    // NOTE: the thresholds between the different threat level categories are codified in at least four different
    // places throughout the app and ought to be centralized
    if (threatLevel >= 8) {
      statsToUpdate = mttrCriticalThreatStats;
    }
    else if (threatLevel >= 4) {
      statsToUpdate = mttrSevereThreatStats;
    }
    else if (threatLevel >= 2) {
      statsToUpdate = mttrModerateThreatStats;
    }
    else {
      statsToUpdate = mttrLowThreatStats;
    }

    statsToUpdate.addValue(timeToResolve);
  }
}
