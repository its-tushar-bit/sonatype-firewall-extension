/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Date;

import com.sonatype.insight.brain.model.Application;

public class ReportMetadataDTO
{
  private boolean expandedCoverage;

  private Date reportTime;

  private String reportTitle;

  private Application application;

  private String stageId;

  private String commitHash;

  private String initiator;

  public boolean isExpandedCoverage() {
    return expandedCoverage;
  }

  public void setExpandedCoverage(final boolean expandedCoverage) {
    this.expandedCoverage = expandedCoverage;
  }

  public Date getReportTime() {
    return reportTime;
  }

  public void setReportTime(final Date reportTime) {
    this.reportTime = reportTime;
  }

  public String getReportTitle() {
    return reportTitle;
  }

  public void setReportTitle(final String reportTitle) {
    this.reportTitle = reportTitle;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(final Application application) {
    this.application = application;
  }

  public String getStageId() {
    return stageId;
  }

  public void setStageId(final String stageId) {
    this.stageId = stageId;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(String commitHash) {
    this.commitHash = commitHash;
  }

  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(final String initiator) {
    this.initiator = initiator;
  }
}
