/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.OffsetDateTime;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public abstract class BasePullRequestPolicyEvaluationResolver
{
  protected static final long INTERNAL_POLICY_EVALUATION_RECHECK_INTERVAL = 1000L * 60 * 90; // 90 minutes

  protected static final Stage SOURCE_STAGE = new Stage(Stage.ID_SOURCE);

  protected static final Stage DEVELOP_STAGE = new Stage(Stage.ID_DEVELOP);

  protected final PolicyEvaluationDAO policyEvaluationDAO;

  protected final SourceControlScanService sourceControlScanService;

  public BasePullRequestPolicyEvaluationResolver(
      PolicyEvaluationDAO policyEvaluationDAO,
      SourceControlScanService sourceControlScanService)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.sourceControlScanService = sourceControlScanService;
  }

  protected boolean hasExternalPolicyEvaluations(final String applicationId) {
    OffsetDateTime dateTime = OffsetDateTime.now().minusDays(SourceControlDAO.EXTERNAL_EVALUATION_WINDOW_IN_DAYS);
    Date cutoffTime = Date.from(dateTime.toInstant());
    return policyEvaluationDAO.hasExternalPolicyEvaluations(applicationId, cutoffTime);
  }

  /**
   * If multiple policy evaluations exist for the same commit hash, we prefer them in the following order:
   * <ul>
   * <li>the BUILD stage evaluation, if one exists, or</li>
   * <li>the SOURCE stage evaluation, if one exists, or</li>
   * <li>the default evaluation (passed as a parameter).</li>
   * </ul>
   */
  protected PolicyEvaluation resolveForPreferredStages(PolicyEvaluation defaultPolicyEvaluation) {
    PolicyEvaluation policyEvaluation = resolveForStage(defaultPolicyEvaluation, Stage.ID_BUILD);
    if (null == policyEvaluation) {
      policyEvaluation = resolveForStage(defaultPolicyEvaluation, Stage.ID_SOURCE);
    }
    if (null == policyEvaluation) {
      policyEvaluation = defaultPolicyEvaluation;
    }
    return policyEvaluation;
  }

  /**
   * if the given policy evaluation is not for the given stage try to find one for the same commit that is
   */
  private PolicyEvaluation resolveForStage(PolicyEvaluation policyEvaluation, String stageTypeId) {
    if (!policyEvaluation.getStageTypeId().equalsIgnoreCase(stageTypeId)) {
      final PolicyEvaluation policyEvaluationCandidate =
          policyEvaluationDAO.getLastByApplicationIdCommitHashAndStageId(
              policyEvaluation.getApplicationId(),
              policyEvaluation.getCommitHash(),
              stageTypeId);
      if (policyEvaluationCandidate != null &&
          policyEvaluationCandidate.wasInternallyTriggered() == policyEvaluation.wasInternallyTriggered())
      {
        return policyEvaluationCandidate;
      }
      return null;
    }
    return policyEvaluation;
  }
}
