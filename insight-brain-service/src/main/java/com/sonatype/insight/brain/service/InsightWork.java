/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.utils.IdValidationUtils;

@Named
@Singleton
public class InsightWork 
{
  private final InsightConfig insightConfig;

  private final Configuration configuration;

  @Inject
  public InsightWork(
      final InsightConfig insightConfig,
      final Configuration configuration)
  {
    this.insightConfig = insightConfig;
    this.configuration = configuration;
  }

  public InsightWork(final InsightConfig insightConfig) {
    this(insightConfig, null);
  }

  public File getWorkDir() {
    return insightConfig.getSonatypeWork();
  }

  public File getCacheDir() {
    return new File(insightConfig.getClusterDirectory(), "cache");
  }

  public File getScanDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(getScanDir(), appId);
  }

  public File getScanDir() {
    return new File(insightConfig.getClusterDirectory(), "scan");
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

  public File getRepositoryManagerIconDir() {
    return new File(getDataDir(), "repositoryManager");
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
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
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
   * @since 1.170
   */
  public File getIerDashboardIconsDirectory() {
    return new File(insightConfig.getSonatypeWork(), "enterpriseReportingDashboardIcons");
  }

  public File getSbomDir() {
    File sbomTempDir = new File(insightConfig.getClusterDirectory(), "sboms");

    if (!sbomTempDir.exists()) {
      try {
        Files.createDirectories(sbomTempDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed creating SBOM directory", e);
      }
    }
    return sbomTempDir;
  }

  public File getSbomDir(final String appId) {
    IdValidationUtils.validate(appId);
    File sbomTempDir = new File(getSbomDir(), appId);

    if (!sbomTempDir.exists()) {
      try {
        Files.createDirectories(sbomTempDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed creating SBOM directory for appId " + appId, e);
      }
    }
    return sbomTempDir;
  }
  
  public File getSbomTempDir() {
    File sbomTempDir = new File(getSbomDir(), "temp");
    if (!sbomTempDir.exists()) {
      try {
        Files.createDirectories(sbomTempDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed creating SBOM temporary directory", e);
      }
    }
    return sbomTempDir;
  }
}
