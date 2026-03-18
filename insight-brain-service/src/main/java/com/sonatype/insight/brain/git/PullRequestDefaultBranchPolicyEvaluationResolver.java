/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.scm.api.model.CommitInformation;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class PullRequestDefaultBranchPolicyEvaluationResolver
    extends BasePullRequestPolicyEvaluationResolver
{
  private final GitCommitHistoryService gitCommitHistoryService;

  private final PullRequestInfoClient pullRequestInfoClient;

  @Inject
  public PullRequestDefaultBranchPolicyEvaluationResolver(
      GitCommitHistoryService gitCommitHistoryService,
      PolicyEvaluationDAO policyEvaluationDAO,
      PullRequestInfoClient pullRequestInfoClient,
      SourceControlScanService sourceControlScanService)
  {
    super(policyEvaluationDAO, sourceControlScanService);
    this.gitCommitHistoryService = gitCommitHistoryService;
    this.pullRequestInfoClient = pullRequestInfoClient;
  }

  /**
   * Finds the most recent policy evaluation (with respect to commit history) for the given application. If there are
   * multiple evaluations for the same commit we prefer a build stage policy eval, if one is available. Next we'd
   * consider a source stage policy eval. If neither exists we will perform a scan and a policy evaluation against
   * the source stage (IFF the most recent eval was not externally triggered).
   *
   * One hiccup that currently exists is that we don't currently distinguish between stages with respect to the
   * default branch commit history. This means we have a little extra work to do to sort out which policy evaluation
   * to use for the default branch.
   */
  public PolicyEvaluation getOrPerformDefaultBranchPolicyEvaluation(
      String applicationId,
      GitRepositoryInfo gitRepositoryInfo,
      String pullRequestHeadCommitHash) throws GitException, IOException
  {
    boolean hasExternalPolicyEvaluations = hasExternalPolicyEvaluations(applicationId);

    PolicyEvaluation defaultBranchPolicyEvaluation =
        getLatestPolicyEvaluationForBaseBranch(applicationId, hasExternalPolicyEvaluations);

    if (shouldUpdateCommitHistory(defaultBranchPolicyEvaluation)) {
      CommitInformation commitInfo =
          pullRequestInfoClient.getCommitInfoFromScm(gitRepositoryInfo, pullRequestHeadCommitHash);

      if (CollectionUtils.isNotEmpty(commitInfo.getCommits())) {
        gitCommitHistoryService.updateCommitHistoryForCommits(applicationId, commitInfo.getCommits());
        defaultBranchPolicyEvaluation =
            getLatestPolicyEvaluationForBaseBranch(applicationId, hasExternalPolicyEvaluations);
      }
    }

    if (!hasExternalPolicyEvaluations && isMissingOrStaleInternalPolicyEvaluation(defaultBranchPolicyEvaluation)) {
      defaultBranchPolicyEvaluation = sourceControlScanService
          .doSynchronousSourceControlScan(applicationId, SOURCE_STAGE, gitRepositoryInfo.getBaseBranch());
    }

    return defaultBranchPolicyEvaluation;
  }

  private boolean shouldUpdateCommitHistory(PolicyEvaluation policyEvaluation) {
    if (null == policyEvaluation) {
      return true;
    }

    Date cutoffTime = new Date(System.currentTimeMillis() - INTERNAL_POLICY_EVALUATION_RECHECK_INTERVAL);
    return policyEvaluation.wasInternallyTriggered() && policyEvaluation.getTime().before(cutoffTime);
  }

  private boolean isMissingOrStaleInternalPolicyEvaluation(PolicyEvaluation policyEvaluation) {
    if (null == policyEvaluation) {
      return true;
    }

    boolean staleInternal = false;
    if (policyEvaluation.wasInternallyTriggered()) {
      // stale by means of not being for the latest default branch commit
      String latestCommit = gitCommitHistoryService.getLatestCommitForApplication(policyEvaluation.getApplicationId());
      staleInternal = !StringUtils.equalsIgnoreCase(policyEvaluation.getCommitHash(), latestCommit);
    }

    return staleInternal;
  }

  /**
   * we're looking for either a build stage (preferred) or a source stage policy evaluation for the most recent
   * commit that has a policy evaluation; otherwise just return the latest and allow the caller figure out what to
   * do with it
   */
  private PolicyEvaluation getLatestPolicyEvaluationForBaseBranch(
      String applicationId,
      boolean externallyTriggered)
  {
    PolicyEvaluation policyEvaluation = null;

    // this represents the most recent commit with a policy evaluation
    Optional<PolicyEvaluation> latestPolicyEvaluation =
        gitCommitHistoryService.getLatestPolicyEvaluationForApplicationBaseBranch(applicationId, externallyTriggered);

    if (latestPolicyEvaluation.isPresent()) {
      policyEvaluation = resolveForPreferredStages(latestPolicyEvaluation.get());
    }

    return policyEvaluation;
  }
}
