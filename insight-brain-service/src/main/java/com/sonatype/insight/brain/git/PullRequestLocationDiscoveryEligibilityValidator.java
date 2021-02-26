/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.nexus.scm.SourceControlProvider;

@Named
@Singleton
public class PullRequestLocationDiscoveryEligibilityValidator
{
  private final InsightConfig insightConfig;

  @Inject
  public PullRequestLocationDiscoveryEligibilityValidator(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  public boolean isLocationDiscoveryNeededAndAllowed(
      final SourceControlProvider sourceControlProvider,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff)
  {
    boolean isEligibleForLineCommenting = sourceControlProvider.supportsCodeInsights()
        || (insightConfig.isFeatureEnabled(Feature.PR_LINE_COMMENTING)
                && sourceControlProvider.supportsPullRequestLineCommenting());
    return isEligibleForLineCommenting && policyViolationDiff.hasAppeared();
  }
}
