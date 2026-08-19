/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.helper;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;

public class ApplicationEvaluationEventBuilder
{
  private final ApplicationEvaluationEvent applicationEvaluationEvent;

  public ApplicationEvaluationEventBuilder() {
    applicationEvaluationEvent = new ApplicationEvaluationEvent();
  }

  public ApplicationEvaluationEventBuilder withApplicationId(String applicationId) {
    applicationEvaluationEvent.ownerId = applicationId;
    return this;
  }

  public ApplicationEvaluationEventBuilder withPolicyEvaluationId(String policyEvaluationId) {
    applicationEvaluationEvent.policyEvaluationId = policyEvaluationId;
    return this;
  }

  public ApplicationEvaluationEventBuilder withCommitHash(String commitHash) {
    applicationEvaluationEvent.commitHash = commitHash;
    return this;
  }

  public ApplicationEvaluationEventBuilder withComponentCounts(int critical, int severe, int moderate) {
    applicationEvaluationEvent.criticalComponentCount = critical;
    applicationEvaluationEvent.severeComponentCount = severe;
    applicationEvaluationEvent.moderateComponentCount = moderate;
    applicationEvaluationEvent.affectedComponentCount = critical + severe + moderate;
    return this;
  }

  public ApplicationEvaluationEventBuilder withScanId(String scanId) {
    applicationEvaluationEvent.reportId = scanId;
    return this;
  }

  public ApplicationEvaluationEventBuilder forBuildStage() {
    applicationEvaluationEvent.stageTypeId = StageTypes.BUILD.getId();
    return this;
  }

  public ApplicationEvaluationEventBuilder withSuccessOutcome() {
    applicationEvaluationEvent.outcome = ApplicationEvaluationEvent.ACTION_ID_NONE;
    return this;
  }

  public ApplicationEvaluationEventBuilder withFailureOutcome() {
    applicationEvaluationEvent.outcome = Action.ID_FAIL;
    return this;
  }

  public ApplicationEvaluationEvent build() {
    return applicationEvaluationEvent;
  }
}
