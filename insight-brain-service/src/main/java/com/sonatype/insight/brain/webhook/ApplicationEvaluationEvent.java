/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.Date;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;

/**
 * @since 1.25.0
 */
public class ApplicationEvaluationEvent
    extends WebhookEvent
{
  public static final String ACTION_ID_NONE = "none";

  public String policyEvaluationId;

  public String stageTypeId;

  public String ownerId;

  public Date evaluationDate;

  public int affectedComponentCount;

  public int criticalComponentCount;

  public int severeComponentCount;

  public int moderateComponentCount;

  public String outcome;

  public String reportId;

  /**
   * Set only if available from underlying data
   *
   * @since 1.67.0
   */
  public String commitHash;

  /**
   * @since 1.91
   */
  public ApplicationSummary application = new ApplicationSummary();

  /**
   * @since 1.98
   */
  public boolean isForLatestScan;

  /**
   * Set only if available from underlying data
   *
   * @since 1.186.0
   */
  public String branchName;

  @Override
  public String toString() {
    return getClass().getName() + "{policyEvaluationId=" + policyEvaluationId + "}";
  }
}
