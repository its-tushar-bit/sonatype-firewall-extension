/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.collect.Table;

/**
 * An event in the lifecycle of evaluations and violations that PolicyViolationAggregationService
 * is interested in. This class is the root of a hierarchy: one branch of the hierarchy contains "processable"
 * events - those which can be directly processed to compute statistics. This includes events representing the
 * appearance and resolution of a given violation across all stages. Other types of events are not directly
 * processable for statistics but instead represent an intermediate step in the creation of events that are. These
 * consist of events representing the appearance and resolution of a given violation in a _particular_ stage
 */
abstract class EvaluationEvent
    implements Comparable<EvaluationEvent>
{
  final Date time;

  EvaluationEvent(Date time) {
    this.time = time;
  }

  @Override
  public int compareTo(EvaluationEvent other) {
    return time.compareTo(other.time);
  }
}

abstract class ProcessableEvaluationEvent
    extends EvaluationEvent
{
  ProcessableEvaluationEvent(Date time) {
    super(time);
  }

  abstract void process(ResultsWrapper results, TimePeriod timePeriod);
}

class EvaluationPerformedEvent
    extends ProcessableEvaluationEvent
{
  public EvaluationPerformedEvent(PolicyEvaluation evaluation) {
    super(evaluation.getTime());
  }

  @Override
  void process(ResultsWrapper results, TimePeriod timePeriod) {
    results.evaluationCount++;
  }
}

abstract class ProcessableViolationEvent
    extends ProcessableEvaluationEvent
{
  protected final PolicyViolation violation;

  protected final PolicyThreatCategory threatCategory;

  protected final ThreatLevel threatLevel;

  ProcessableViolationEvent(PolicyViolation violation, Date time) {
    super(time);
    this.violation = violation;
    this.threatCategory = violation.getThreatCategory();
    this.threatLevel = ThreatLevel.from(violation.getThreatLevel());
  }

  ProcessableViolationEvent(PolicyViolation violation) {
    this(violation, violation.getOpenTime());
  }
}

/**
 * Represents a violation the first time it appears in any stage
 */
class ViolationDiscoveredEvent
    extends ProcessableViolationEvent
{
  ViolationDiscoveredEvent(PolicyViolation violation) {
    super(violation);
  }

  @Override
  void process(ResultsWrapper results, TimePeriod timePeriod) {
    results.discoveredCounts
        .put(threatCategory, threatLevel, results.discoveredCounts.get(threatCategory, threatLevel) + 1);
    results.openCounts.put(threatCategory, threatLevel, results.openCounts.get(threatCategory, threatLevel) + 1);
  }
}

/**
 * Represents a violation being resolved in all stages. The `time` field stores the resolution time while the
 * `openTime` field stores the openTime
 */
abstract class ViolationResolvedEvent
    extends ProcessableViolationEvent
{
  private final Date openTime;

  ViolationResolvedEvent(PolicyViolation violation, Date openTime, Date resolveTime) {
    super(violation, resolveTime);
    this.openTime = openTime;
  }

  protected void processResolved(
      MttrStats mttrStats,
      Table<PolicyThreatCategory, ThreatLevel, Integer> resolvedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts)
  {
    mttrStats.addViolation(violation, openTime, time);
    resolvedCounts.put(threatCategory, threatLevel, resolvedCounts.get(threatCategory, threatLevel) + 1);
    Integer openCountValue = openCounts.get(threatCategory, threatLevel);
    openCounts.put(threatCategory, threatLevel, openCountValue < 1 ? 0 : openCountValue - 1);
  }
}

class ViolationWaivedEvent
    extends ViolationResolvedEvent
{
  ViolationWaivedEvent(PolicyViolation violation, Date openTime, Date waiveTime) {
    super(violation, openTime, waiveTime);
  }

  @Override
  void process(ResultsWrapper results, TimePeriod timePeriod) {
    super.processResolved(results.mttrStats, results.waivedCounts, results.openCounts);
  }
}

class ViolationFixedEvent
    extends ViolationResolvedEvent
{
  ViolationFixedEvent(PolicyViolation violation, Date openTime, Date fixTime) {
    super(violation, openTime, fixTime);
  }

  @Override
  void process(ResultsWrapper results, TimePeriod timePeriod) {
    super.processResolved(results.mttrStats, results.fixedCounts, results.openCounts);
  }
}

class ViolationDiscoveredInStageEvent
    extends EvaluationEvent
{
  public final PolicyViolation violation;

  ViolationDiscoveredInStageEvent(PolicyViolation violation) {
    super(violation.getOpenTime());
    this.violation = violation;
  }
}

class ViolationResolvedInStageEvent
    extends EvaluationEvent
{
  public final PolicyViolation violation;

  // true if the violation was waived, false if it was fixed
  public final boolean isWaived;

  ViolationResolvedInStageEvent(PolicyViolation violation, Date time, boolean isWaived) {
    super(time);
    this.violation = violation;
    this.isWaived = isWaived;
  }
}
