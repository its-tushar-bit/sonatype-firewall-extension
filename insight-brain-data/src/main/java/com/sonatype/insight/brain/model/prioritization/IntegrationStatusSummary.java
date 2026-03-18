/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.prioritization;

/**
 * Record representing integration status information for an application.
 * Contains aggregated data including policy evaluation, commit history, and CI integration status.
 *
 * @since 1.196
 */
public record IntegrationStatusSummary(
    String applicationId,
    String applicationName,
    String applicationPublicId,
    String organizationId,
    long lastEvaluationTimestamp,
    String lastScanId,
    long lastCommitTimestamp,
    boolean isCiIntegrationEnabled)
{
  public boolean hasPrioritiesReport() {
    return lastEvaluationTimestamp > 0 && lastScanId != null;
  }
}
