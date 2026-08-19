/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;

@Named
@Singleton
public class PullRequestCommentingEligibilityValidator
{
  public boolean isLocationDiscoveryNeededAndAllowed(
      final SourceControlProvider sourceControlProvider,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff)
  {
    boolean isEligibleForLineCommenting = sourceControlProvider.supportsCodeInsights()
        || (SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.isEnabled()
            && sourceControlProvider.supportsPullRequestLineCommenting());
    return isEligibleForLineCommenting && policyViolationDiff.hasAppeared();
  }

  public boolean isPullRequestLineCommentingEnabled(final GitRepositoryInfo gitRepositoryInfo) {
    return SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.isEnabled() &&
        gitRepositoryInfo != null && gitRepositoryInfo.getProvider() != null &&
        gitRepositoryInfo.getProvider().supportsPullRequestLineCommenting();
  }

  public boolean isPullRequestCommentingEnabled(final GitRepositoryInfo gitRepositoryInfo) {
    return gitRepositoryInfo != null && gitRepositoryInfo.pullRequestCommentingEnabled != null &&
        gitRepositoryInfo.pullRequestCommentingEnabled;
  }
}
