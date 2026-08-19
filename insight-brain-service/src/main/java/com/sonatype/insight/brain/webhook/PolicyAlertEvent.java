/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;

/**
 * @since 1.64.0
 */
public class PolicyAlertEvent
    extends WebhookEvent
{
  public ApplicationEvaluationEvent applicationEvaluation;

  public ApplicationSummary application;

  public List<PolicyFact> policyFacts = new ArrayList<>();

  public String targetId;

  public PolicyAlertEvent(final String targetId) {
    this.targetId = targetId;
  }

  @Override
  public String toString() {
    return "PolicyAlertEvent{" +
        "targetId='" + targetId + '\'' +
        ", applicationEvaluation=" + applicationEvaluation +
        ", application=" + application +
        ", policyFacts=" + policyFacts +
        '}';
  }
}
