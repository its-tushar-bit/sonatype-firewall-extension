/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send notifications of policy alerts to Source Code Management
 * systems like github
 */
public class PolicyAlertScmNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertScmNotifier.class);

  private final PullRequestFeatureCheck pullRequestFeatureCheck;

  @Inject
  public PolicyAlertScmNotifier(final PullRequestFeatureCheck pullRequestFeatureCheck) {
    this.pullRequestFeatureCheck = pullRequestFeatureCheck;
  }

  public void sendNotifications(final Application app,
                                final String scanId,
                                final Stage stage,
                                final List<PolicyNotification> policyNotifications,
                                final int grandfatheredPolicyViolationCount,
                                final String targetUrl)
      throws IOException
  {
    if (!pullRequestFeatureCheck.isPullRequestFeatureSupported(app)) {
      return;
    }

    // TODO invoke PR engine
    log.debug("Invoke PR engine to construct a PR");
  }
}
