/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.nexus.git.utils.api.GitException;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class PullRequestBaseCommitPolicyEvaluationResolver
    extends BasePullRequestPolicyEvaluationResolver
{
  @Inject
  public PullRequestBaseCommitPolicyEvaluationResolver(
      PolicyEvaluationDAO policyEvaluationDAO,
      SourceControlScanService sourceControlScanService)
  {
    super(policyEvaluationDAO, sourceControlScanService);
  }

  /**
   * Finds the base commit associated policy evaluation, if one exists, for the given application.
   * If there are multiple evaluations for the same commit we prefer a build stage policy eval, if one is available.
   * Next we'd consider a source stage policy eval, or any stage available.
   * If neither exists, we will perform a scan and a policy evaluation against the source stage (IFF the most recent
   * eval was not externally triggered).
   */
  public PolicyEvaluation getOrPerformBaseCommitPolicyEvaluation(
      String applicationId,
      String baseBranchName,
      String baseCommitHash) throws GitException, IOException
  {
    boolean hasExternalPolicyEvaluations = hasExternalPolicyEvaluations(applicationId);

    PolicyEvaluation baseCommitPolicyEvaluation = null;

    // if possible, we try to find the policy evaluation for the base commit of the PR
    if (StringUtils.isNotBlank(baseCommitHash)) {
      baseCommitPolicyEvaluation = getLatestPolicyEvaluationForBaseCommitHash(applicationId, baseCommitHash,
          hasExternalPolicyEvaluations);

      // if not found, and no externally triggered evaluations, perform the policy evaluation for the base commit
      if (null == baseCommitPolicyEvaluation && !hasExternalPolicyEvaluations &&
          StringUtils.isNotBlank(baseBranchName)) {
        baseCommitPolicyEvaluation = sourceControlScanService
            .doSynchronousSourceControlScan(applicationId, SOURCE_STAGE, baseBranchName, baseCommitHash);
      }
    }

    return baseCommitPolicyEvaluation;
  }

  private PolicyEvaluation getLatestPolicyEvaluationForBaseCommitHash(
      final String applicationId,
      final String baseCommitHash,
      final boolean externallyTriggered)
  {
    PolicyEvaluation policyEvaluation = null;

    PolicyEvaluation availablePolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationAndCommitHashAndTriggerType(applicationId, baseCommitHash,
            externallyTriggered);

    if (null != availablePolicyEvaluation) {
      policyEvaluation = resolveForPreferredStages(availablePolicyEvaluation);
    }
    return policyEvaluation;
  }
}
