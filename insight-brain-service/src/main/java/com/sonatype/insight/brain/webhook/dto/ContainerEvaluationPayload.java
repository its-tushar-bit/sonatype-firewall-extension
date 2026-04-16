/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.Date;

/**
 * Webhook payload for Firewall container evaluation events.
 * Semantically represents container evaluation in Firewall context.
 *
 * @since 1.203.0
 */
public class ContainerEvaluationPayload
    extends WebhookPayload
{
  public String id;

  public ContainerEvaluationDTO containerEvaluation;

  public static class ContainerEvaluationDTO
  {
    /**
     * Repository being evaluated (Firewall repository)
     *
     * @since 1.203.0
     */
    public ContainerRepositorySummary repository = new ContainerRepositorySummary();

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
