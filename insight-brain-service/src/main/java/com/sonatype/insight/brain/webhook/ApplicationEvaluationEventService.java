/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.security.CurrentUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.25.0
 */
@Named
@Singleton
public class ApplicationEvaluationEventService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationEvaluationEventService.class);

  private final AsyncEventBus asyncEventBus;

  private final CurrentUser currentUser;

  @Inject
  public ApplicationEvaluationEventService(final AsyncEventBus asyncEventBus, final CurrentUser currentUser) {
    this.asyncEventBus = asyncEventBus;
    this.currentUser = currentUser;
  }

  public void postEvent(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult,
      final Application application)
  {
    try {
      ApplicationEvaluationEvent event =
          buildEvent(policyEvaluation, policyEvaluationResult, currentUser, application);
      asyncEventBus.post(event);
    }
    catch (RuntimeException e) {
      log.error("Webhook not posted due to exception.", e);
    }
  }

  static ApplicationEvaluationEvent buildEvent(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult,
      final CurrentUser currentUser,
      final Application application)
  {
    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    event.policyEvaluationId = policyEvaluation.getId();
    event.stageTypeId = policyEvaluation.getStageTypeId();
    event.ownerId = policyEvaluation.getApplicationId();
    event.evaluationDate = policyEvaluation.getTime();
    event.initiator = currentUser.getUsernameOrSystem();
    event.reportId = policyEvaluation.getScanId();
    event.commitHash = policyEvaluation.getCommitHash();
    event.isForLatestScan = !policyEvaluation.isForObsoleteScan();
    event.branchName = policyEvaluation.getBranchName();

    event.affectedComponentCount = policyEvaluationResult.getAffectedComponentCount();
    event.criticalComponentCount = policyEvaluationResult.getCriticalComponentCount();
    event.severeComponentCount = policyEvaluationResult.getSevereComponentCount();
    event.moderateComponentCount = policyEvaluationResult.getModerateComponentCount();

    event.application.id = application.getId();
    event.application.publicId = application.getPublicId();
    event.application.name = application.getName();
    event.application.organizationId = application.getOrganizationId();

    String outcome = ApplicationEvaluationEvent.ACTION_ID_NONE;
    for (PolicyAlert alert : policyEvaluationResult.getAlerts()) {
      for (final Action action : alert.getActions()) {
        final String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          outcome = actionTypeId;
        }
        else if (Action.ID_WARN.equals(actionTypeId) && !outcome.equals(Action.ID_FAIL)) {
          outcome = actionTypeId;
        }
      }
    }
    event.outcome = outcome;

    return event;
  }
}
