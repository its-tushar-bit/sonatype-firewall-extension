/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

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

  abstract void process(MttrStats mttrStats, DiscoveredStats discoveredStats);
}

class EvaluationPerformedEvent
    extends ProcessableEvaluationEvent
{
  public EvaluationPerformedEvent(PolicyEvaluation evaluation) {
    super(evaluation.getTime());
  }

  @Override
  void process(MttrStats mttrStats, DiscoveredStats discoveredStats) {
    discoveredStats.incrementEvaluationCount();
  }
}

/**
 * Represents a violation the first time it appears in any stage
 */
class ViolationDiscoveredEvent
    extends ProcessableEvaluationEvent
{
  private final PolicyViolation violation;

  ViolationDiscoveredEvent(PolicyViolation violation) {
    super(violation.getOpenTime());
    this.violation = violation;
  }

  @Override
  void process(MttrStats mttrStats, DiscoveredStats discoveredStats) {
    discoveredStats.addViolation(violation);
  }
}

/**
 * Represents a violation being resolved in all stages
 */
class ViolationResolvedEvent
    extends ProcessableEvaluationEvent
{
  private final PolicyViolation violation;

  private final Date openTime;

  ViolationResolvedEvent(PolicyViolation violation, Date openTime, Date resolveTime) {
    super(resolveTime);
    this.violation = violation;
    this.openTime = openTime;
  }

  @Override
  void process(MttrStats mttrStats, DiscoveredStats discoveredStats) {
    mttrStats.addViolation(violation, openTime, time);
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

  ViolationResolvedInStageEvent(PolicyViolation violation, Date time) {
    super(time);
    this.violation = violation;
  }
}
