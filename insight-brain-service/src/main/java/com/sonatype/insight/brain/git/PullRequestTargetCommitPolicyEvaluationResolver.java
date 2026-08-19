/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.RepositorySyncCommand;
import com.sonatype.nexus.iq.manager.RepositorySyncExecutor;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class PullRequestTargetCommitPolicyEvaluationResolver
    extends BasePullRequestPolicyEvaluationResolver
{
  private final GitApiFactory gitApiFactory;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public PullRequestTargetCommitPolicyEvaluationResolver(
      GitApiFactory gitApiFactory,
      SourceControlUtils sourceControlUtils,
      PolicyEvaluationDAO policyEvaluationDAO,
      SourceControlScanService sourceControlScanService)
  {
    super(policyEvaluationDAO, sourceControlScanService);
    this.gitApiFactory = gitApiFactory;
    this.sourceControlUtils = sourceControlUtils;
  }

  /**
   * Finds the target commit associated policy evaluation, if one exists, for the given application.
   * The target commit is either the common ancestor of the base and head branches (if one exists),
   * or the base commit of the pull request.
   * If there are multiple evaluations for the same commit we prefer a build stage policy eval, if one is available.
   * Next we'd consider a source stage policy eval, or any stage available.
   * If neither exists, we will perform a scan and a policy evaluation against the source stage (IFF the most recent
   * eval was not externally triggered).
   */
  public PolicyEvaluation getOrPerformTargetCommitPolicyEvaluation(
      Application application,
      GitRepositoryInfo gitRepositoryInfo,
      String baseBranchName,
      String baseCommitHash,
      String headBranchName) throws GitException, IOException
  {
    boolean hasExternalPolicyEvaluations = hasExternalPolicyEvaluations(application.getId());

    PolicyEvaluation targetCommitPolicyEvaluation = null;

    // if possible, we try to find the policy evaluation for the target commit of the PR
    if (StringUtils.isNotBlank(baseBranchName) && StringUtils.isNotBlank(headBranchName)) {
      // the last common ancestor between the two branches is preferred for comparison
      String targetCommitHash =
          getCommonAncestorCommitHash(application, gitRepositoryInfo, baseBranchName, headBranchName);

      if (StringUtils.isNotBlank(targetCommitHash)) {
        targetCommitPolicyEvaluation = getLatestPolicyEvaluationForCommitHash(application.getId(), targetCommitHash,
            hasExternalPolicyEvaluations);
      }
      else if (StringUtils.isNotBlank(baseCommitHash)) {
        targetCommitPolicyEvaluation = getLatestPolicyEvaluationForCommitHash(application.getId(), baseCommitHash,
            hasExternalPolicyEvaluations);
      }

      // if not found, and no externally triggered evaluations, perform the policy evaluation for the target commit
      if (null == targetCommitPolicyEvaluation && !hasExternalPolicyEvaluations) {
        Stage stage = Objects.equals(gitRepositoryInfo.getBaseBranch(), baseBranchName) ? SOURCE_STAGE : DEVELOP_STAGE;
        targetCommitPolicyEvaluation = sourceControlScanService
            .doSynchronousSourceControlScan(application.getId(), stage, baseBranchName, targetCommitHash);
      }
    }

    return targetCommitPolicyEvaluation;
  }

  private String getCommonAncestorCommitHash(
      final Application application,
      final GitRepositoryInfo gitRepositoryInfo,
      final String baseBranchName,
      final String headBranchName) throws GitException
  {
    GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
    File repositoryDirectory = sourceControlUtils.getCheckoutDirectory(application);

    new RepositorySyncExecutor().execute(new RepositorySyncCommand(gitApi, headBranchName, repositoryDirectory));

    return gitApi.getCommonAncestorCommit(repositoryDirectory, baseBranchName, headBranchName);
  }

  private PolicyEvaluation getLatestPolicyEvaluationForCommitHash(
      final String applicationId,
      final String targetCommitHash,
      final boolean externallyTriggered)
  {
    PolicyEvaluation policyEvaluation = null;

    PolicyEvaluation availablePolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationAndCommitHashAndTriggerType(applicationId, targetCommitHash,
            externallyTriggered);

    if (null != availablePolicyEvaluation) {
      policyEvaluation = resolveForPreferredStages(availablePolicyEvaluation);
    }
    return policyEvaluation;
  }
}
