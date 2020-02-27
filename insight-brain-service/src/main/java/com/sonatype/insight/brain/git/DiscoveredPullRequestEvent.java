/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

/**
 * @since 1.86
 */
public class DiscoveredPullRequestEvent
    extends WebhookEvent
{
  public String policyEvaluationId;

  public String applicationId;

  public String commitHash;

  public int pullRequestNumber;

  public String targetPolicyEvaluationId;
}
