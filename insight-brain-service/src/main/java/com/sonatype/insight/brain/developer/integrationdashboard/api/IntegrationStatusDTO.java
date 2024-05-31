/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard.api;

public class IntegrationStatusDTO
{
  private String applicationName;

  private String applicationId;

  private String applicationPublicId;

  private boolean isCiIntegrationEnabled;

  private boolean isAutomatedSourceControlFeedbackEnabled;

  private long lastCommitTimestamp;

  private long lastEvaluationTimestamp;

  private String organizationId;

  private int totalRiskScore;

  private boolean hasPrioritiesReport;

  private String lastScanId;

  public String getApplicationName() {
    return applicationName;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getApplicationPublicId() {
    return applicationPublicId;
  }

  public boolean isCiIntegrationEnabled() {
    return isCiIntegrationEnabled;
  }

  public boolean isAutomatedSourceControlFeedbackEnabled() {
    return isAutomatedSourceControlFeedbackEnabled;
  }

  public long getLastCommitTimestamp() {
    return lastCommitTimestamp;
  }

  public long getLastEvaluationTimestamp() {
    return lastEvaluationTimestamp;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public int getTotalRiskScore() {
    return totalRiskScore;
  }

  public boolean isHasPrioritiesReport() {
    return hasPrioritiesReport;
  }

  public String getLastScanId() {
    return lastScanId;
  }

  public IntegrationStatusDTO setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
    return this;
  }

  public IntegrationStatusDTO setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public IntegrationStatusDTO setApplicationPublicId(final String applicationPublicId) {
    this.applicationPublicId = applicationPublicId;
    return this;
  }

  public IntegrationStatusDTO setCiIntegrationEnabled(final boolean ciIntegrationEnabled) {
    isCiIntegrationEnabled = ciIntegrationEnabled;
    return this;
  }

  public IntegrationStatusDTO setAutomatedSourceControlFeedbackEnabled(
      final boolean automatedSourceControlFeedbackEnabled)
  {
    isAutomatedSourceControlFeedbackEnabled = automatedSourceControlFeedbackEnabled;
    return this;
  }

  public IntegrationStatusDTO setLastCommitTimestamp(final long lastCommitTimestamp) {
    this.lastCommitTimestamp = lastCommitTimestamp;
    return this;
  }

  public IntegrationStatusDTO setLastEvaluationTimestamp(final long lastEvaluationTimestamp) {
    this.lastEvaluationTimestamp = lastEvaluationTimestamp;
    return this;
  }

  public IntegrationStatusDTO setOrganizationId(final String organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public IntegrationStatusDTO setTotalRiskScore(final int totalRiskScore) {
    this.totalRiskScore = totalRiskScore;
    return this;
  }

  public IntegrationStatusDTO setHasPrioritiesReport(final boolean hasPrioritiesReport) {
    this.hasPrioritiesReport = hasPrioritiesReport;
    return this;
  }

  public IntegrationStatusDTO setLastScanId(final String lastScanId) {
    this.lastScanId = lastScanId;
    return this;
  }
}
