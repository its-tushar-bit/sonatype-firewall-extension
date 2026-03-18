/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;

/**
 * For every pull request comment that is created or updated, the {@link #invokeAction} method of implementors of this
 * class will be invoked to allow for any required follow-up actions.
 */
public interface PullRequestPostCommentAction
{
  /**
   * Invoke the post-pull-request-comment action
   *
   * @param gitClientFactory factory for the git client
   * @param gitRepositoryInfo the repository the action is being invoked on
   * @param policyViolationDiff the policy violation diff between the source and branch
   * @param sourceCommitPolicyEvaluation {@link PolicyViolation} for the source commit
   * @param baseBranchPolicyEvaluation {@link PolicyViolation} for the base branch
   */
  void invokeAction(
      final GitClientFactory gitClientFactory,
      final GitRepositoryInfo gitRepositoryInfo,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final SourceControlComponentDetails sourceControlComponentDetails,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final String branch,
      final LocationDiscoveryResult locationDiscoveryResult);
}
