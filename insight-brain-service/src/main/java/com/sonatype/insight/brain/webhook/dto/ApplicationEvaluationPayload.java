/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.Date;

/**
 * @since 1.25.0
 */
public class ApplicationEvaluationPayload
    extends WebhookPayload
{
  public String id;

  public ApplicationEvaluationDTO applicationEvaluation;

  /**
   * Evaluation type distinguishing between Lifecycle and Firewall contexts.
   *
   * In Lifecycle context (APPLICATION): represents an "Application Evaluation"
   * In Firewall context (CONTAINER): represents a "Container Evaluation"
   *
   * Defaults to APPLICATION for backward compatibility.
   */
  public EvaluationType evaluationType = EvaluationType.APPLICATION;

  public static class ApplicationEvaluationDTO
  {
    /**
     * Since 1.91
     */
    public ApplicationSummary application = new ApplicationSummary();

    public String policyEvaluationId;

    public String stage;

    public String ownerId;

    public Date evaluationDate;

    public int affectedComponentCount;

    public int criticalComponentCount;

    public int severeComponentCount;

    public int moderateComponentCount;

    public String outcome;

    public String reportId;

    public boolean isForLatestScan;
  }
}
