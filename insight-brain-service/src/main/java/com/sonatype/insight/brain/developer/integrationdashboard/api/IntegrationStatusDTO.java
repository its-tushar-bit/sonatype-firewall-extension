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

  private boolean hasSastReport;

  private String lastSastReportId;

  private Long lastSastReportTime;

  public IntegrationStatusDTO() {
  }

  public IntegrationStatusDTO(
      final String applicationName,
      final String applicationId,
      final String applicationPublicId,
      final boolean isCiIntegrationEnabled,
      final boolean isAutomatedSourceControlFeedbackEnabled,
      final long lastCommitTimestamp,
      final long lastEvaluationTimestamp,
      final String organizationId,
      final int totalRiskScore,
      final boolean hasSastReport,
      final String lastSastReportId,
      final Long lastReportTime)
  {
    this.applicationName = applicationName;
    this.applicationId = applicationId;
    this.applicationPublicId = applicationPublicId;
    this.isCiIntegrationEnabled = isCiIntegrationEnabled;
    this.isAutomatedSourceControlFeedbackEnabled = isAutomatedSourceControlFeedbackEnabled;
    this.lastCommitTimestamp = lastCommitTimestamp;
    this.lastEvaluationTimestamp = lastEvaluationTimestamp;
    this.organizationId = organizationId;
    this.totalRiskScore = totalRiskScore;
    this.hasSastReport = hasSastReport;
    this.lastSastReportId = lastSastReportId;
    this.lastSastReportTime = lastReportTime;
  }

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

  public boolean isHasSastReport() {
    return hasSastReport;
  }

  public String getLastSastReportId() {
    return lastSastReportId;
  }

  public Long getLastSastReportTime() {
    return lastSastReportTime;
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

  public IntegrationStatusDTO setHasSastReport(final boolean hasSastReport) {
    this.hasSastReport = hasSastReport;
    return this;
  }

  public IntegrationStatusDTO setLastSastReportId(final String lastSastReportId) {
    this.lastSastReportId = lastSastReportId;
    return this;
  }

  public IntegrationStatusDTO setLastSastReportTime(final Long lastSastReportTime) {
    this.lastSastReportTime = lastSastReportTime;
    return this;
  }
}
