/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Collection;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClient.StateType;
import com.sonatype.nexus.scm.api.model.StatusRequest;

/**
 * This class handles all the logic to create a status requests, including the information of a valid state for
 * the SCM, and a proper description of the IQ policy evaluation result.
 * <p>
 * This is a helper class, that is used when creating a new commit status and when creating a
 * new pull request status. Check the next classes:
 * <lu>
 *   <li>For commit status creation {@link com.sonatype.insight.brain.git.GitCommitStatusService},</li>
 *   <li>For pull request status creation {@link com.sonatype.insight.brain.git.PullRequestStatusService},</li>
 * </lu>
 */
@Named
@Singleton
public class ScmStatusHelper
{
  static final String IQ_POLICY_EVALUATION = "IQ Policy Evaluation";

  static final String DEFAULT_OUTCOME = "none";

  private final ApplicationDAO applicationDAO;

  private final BaseUrl baseUrl;

  private final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  @Inject
  public ScmStatusHelper(
      final ApplicationDAO applicationDAO,
      final BaseUrl baseUrl,
      final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService)
  {
    this.applicationDAO = applicationDAO;
    this.baseUrl = baseUrl;
    this.developmentPrioritiesUtilsService = developmentPrioritiesUtilsService;
  }

  public StatusRequest createStatusRequestFromPolicyEvaluation(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult,
      final GitApiClient gitApiClient,
      final SourceControlProvider provider)
  {
    final String evaluationOutcome = getEvaluationOutcome(policyEvaluationResult);
    final int criticalCount = policyEvaluationResult.getCriticalComponentCount();
    final int severeCount = policyEvaluationResult.getSevereComponentCount();
    final int moderateCount = policyEvaluationResult.getModerateComponentCount();

    return gitApiClient.createStatusRequest(
        getState(evaluationOutcome, gitApiClient),
        IQ_POLICY_EVALUATION,
        createStatusMessage(criticalCount, severeCount, moderateCount),
        getReportUrl(policyEvaluation.getApplicationId(), policyEvaluation.getScanId(), provider));
  }

  public StatusRequest createStatusRequestFromSourceControlEvent(
      final SourceControlEvent event,
      final GitApiClient gitApiClient,
      final SourceControlProvider provider)
  {
    final int criticalCount = event.getCriticalComponentCount();
    final int severeCount = event.getSevereComponentCount();
    final int moderateCount = event.getModerateComponentCount();

    return gitApiClient.createStatusRequest(
        getState(event.getPolicyEvaluationOutcome(), gitApiClient),
        IQ_POLICY_EVALUATION,
        createStatusMessage(criticalCount, severeCount, moderateCount),
        getReportUrl(event.getApplicationId(), event.getScanId(), provider));
  }

  private String getReportUrl(
      final String applicationId,
      final String scanId,
      final SourceControlProvider provider)
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);
    String reportPath = UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), scanId);
    reportPath = addSourceQuery(reportPath, provider);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setReportUrl(reportPath);

    if (developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()) {
      scanReceipt.setPrioritiesUrl(UserInterfaceLinksHelper.getPrioritiesUrl(application.getPublicId(), scanId));
      return scanReceipt.resolvePrioritiesUrl(baseUrl.get());
    }

    return scanReceipt.resolveReportUrl(baseUrl.get());
  }

  private String addSourceQuery(
      final String reportPath,
      final SourceControlProvider provider)
  {
    return UriBuilder.fromPath(reportPath).queryParam("source", provider.toString()).toString();
  }

  private String getState(
      final String evaluationOutcome,
      final GitApiClient gitApiClient)
  {
    if (Action.ID_FAIL.equals(evaluationOutcome)) {
      return gitApiClient.getState(StateType.FAILURE);
    }
    return gitApiClient.getState(StateType.SUCCESS);
  }

  private String createStatusMessage(int criticalCount, int severeCount, int moderateCount) {
    return String.format("Components: Critical: %d, Severe: %d, Moderate: %d", criticalCount,
        severeCount, moderateCount);
  }

  private String getEvaluationOutcome(PolicyEvaluationResult policyEvaluationResult) {
    return policyEvaluationResult.getAlerts()
        .stream()
        .map(PolicyAlert::getActions)
        .flatMap(Collection::stream)
        .map(Action::getActionTypeId)
        .filter(action -> action.equals(Action.ID_FAIL))
        .findFirst()
        .orElse(DEFAULT_OUTCOME);
  }
}
