/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.utils.IdValidationUtils;

@Named
@Singleton
public class InsightWork
{
  private final InsightConfig insightConfig;

  @Inject
  public InsightWork(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  public File getWorkDir() {
    return insightConfig.getSonatypeWork();
  }

  public File getScanDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(insightConfig.getClusterDirectory(), "scan/" + appId);
  }

  public File getScanFile(final String appId, final String scanId) {
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
    return new File(getScanDir(appId), "scan-" + scanId + ".xml.gz");
  }

  public File getAuditDir() {
    return new File(insightConfig.getClusterDirectory(), "audit");
  }

  public File getAuditDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(getAuditDir(), appId);
  }

  public File getReportDir() {
    return new File(insightConfig.getClusterDirectory(), "report");
  }

  public File getReportDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(getReportDir(), appId);
  }

  public File getReportDir(final String appId, final String scanId) {
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
    return new File(getReportDir(appId), scanId);
  }

  public File getReportFile(final String appId, final String scanId) {
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
    return new File(getReportDir(appId, scanId), "report.zip");
  }

  public File getComponentDetailsDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(insightConfig.getClusterDirectory(), "componentDetails/" + appId);
  }

  public File getComponentDetailsFile(final String appId, final String resultsId) {
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(resultsId);
    return new File(getComponentDetailsDir(appId), "componentDetails-" + resultsId + ".json");
  }

  public File getApplicationIconDir() {
    return new File(getDataDir(), "application");
  }

  public File getOrganizationIconDir() {
    return new File(getDataDir(), "organization");
  }

  public File getDataDir() {
    return new File(insightConfig.getClusterDirectory(), "data");
  }

  /**
   * @since 1.63
   */
  public File getTrashDir() {
    return new File(insightConfig.getClusterDirectory(), "trash");
  }

  /**
   * @since 1.88
   */
  public File getSearchDir() {
    return new File(insightConfig.getClusterDirectory(), "search");
  }

  /**
   * @since 1.88
   */
  public File getSearchIndexDir() {
    return new File(getSearchDir(), "index");
  }

  /**
   * @since 1.104
   */
  public File getSourceControlDir(String appId) {
    IdValidationUtils.validate(appId);
    return new File(insightConfig.getSourceControl().getCloneDirectory(), appId);
  }
}
