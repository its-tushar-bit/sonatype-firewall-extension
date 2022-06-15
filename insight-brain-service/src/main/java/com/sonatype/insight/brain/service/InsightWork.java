/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.SourceControlConfigurationListener;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.utils.IdValidationUtils;

@Named
@Singleton
public class InsightWork
    implements SourceControlConfigurationListener
{
  private final InsightConfig insightConfig;

  private final SourceControlConfigurationDAO sourceControlConfigurationDAO;

  // Visible for testing
  final AtomicReference<SourceControlConfiguration> sourceControlConfigurationAtomicReference = new AtomicReference<>();

  @Inject
  public InsightWork(
      final InsightConfig insightConfig,
      final SourceControlConfigurationDAO sourceControlConfigurationDAO)
  {
    this.insightConfig = insightConfig;
    this.sourceControlConfigurationDAO = sourceControlConfigurationDAO;
    sourceControlConfigurationChanged();
  }

  public InsightWork(final InsightConfig insightConfig) {
    this(insightConfig, new SourceControlConfigurationDAO());
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
    return new File(getResolvedCloneDirectory(), appId);
  }

  public File getResolvedCloneDirectory() {
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationAtomicReference.get();
    File file = new File(sourceControlConfiguration.getCloneDirectory());
    if (!file.isAbsolute()) {
      file = new File(insightConfig.getSonatypeWork(), sourceControlConfiguration.getCloneDirectory());
    }
    return file;
  }

  /**
   * @since 1.114
   */
  public File getTemporaryDirectory() {
    return new File(insightConfig.getSonatypeWork(), "temp");
  }

  /**
   * @since 1.133
   */
  public String getInitialAdminPassword() {
    return insightConfig.getInitialAdminPassword();
  }

  @Override
  public void sourceControlConfigurationChanged() {
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    if (sourceControlConfiguration == null) {
      sourceControlConfiguration = new SourceControlConfiguration();
    }
    sourceControlConfigurationAtomicReference.set(sourceControlConfiguration);
  }
}
