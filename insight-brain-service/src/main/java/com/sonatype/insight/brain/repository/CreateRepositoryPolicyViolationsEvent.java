/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;

public class CreateRepositoryPolicyViolationsEvent
    extends WebhookEvent
{
  public List<RepositoryPolicyViolation> repositoryPolicyViolations = new ArrayList<>();
}
