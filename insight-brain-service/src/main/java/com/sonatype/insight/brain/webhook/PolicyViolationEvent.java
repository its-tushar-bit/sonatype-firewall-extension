/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

/**
 * @since 1.64.0
 */
public class PolicyViolationEvent
    extends WebhookEvent
{
  public ApplicationEvaluationEvent applicationEvaluationEvent;

  public ApplicationSummary application;

  public List<PolicyFact> violations;

  public static class ApplicationSummary
  {
    public String id;

    public String name;

    public OrganizationSummary organizationSummary;

    @Override
    public String toString() {
      return "ApplicationSummary{" +
          "id='" + id + '\'' +
          ", name='" + name + '\'' +
          ", organizationSummary=" + organizationSummary +
          '}';
    }
  }

  public static class OrganizationSummary
  {
    public String id;

    public String name;

    public String namePath;

    public String idPath;

    @Override
    public String toString() {
      return "OrganizationSummary{" +
          "id='" + id + '\'' +
          ", name='" + name + '\'' +
          ", namePath='" + namePath + '\'' +
          ", idPath='" + idPath + '\'' +
          '}';
    }
  }
}
