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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
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

  public File getClusterCacheDir() {
    return new File(insightConfig.getClusterDirectory(), "cache");
  }

  public File getNodeCacheDir() {
    return new File(insightConfig.getSonatypeWork(), "cache");
  }

  @Deprecated
  public File getScanDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(getScanDir(), appId);
  }

  @Deprecated
  public File getScanDir() {
    return new File(insightConfig.getClusterDirectory(), "scan");
  }

  /**
   * Should no longer be used in production code,
   * See {@link com.sonatype.insight.brain.scan.datastore.ScanPersistenceService#getScan(String, String)}
   *
   * @param appId
   * @param scanId
   * @return
   */
  @Deprecated
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

  @Deprecated
  public File getReportDir() {
    return new File(insightConfig.getClusterDirectory(), "report");
  }

  @Deprecated
  public File getReportDir(final String appId) {
    IdValidationUtils.validate(appId);
    return new File(getReportDir(), appId);
  }

  /**
   * Should no longer be used in production code see
   * com.sonatype.insight.brain.report.ApplicationReportPersistenceService#getReportEntity(String, String, String)
   *
   * @param appId
   * @param scanId
   * @return
   */
  @Deprecated
  public File getReportDir(final String appId, final String scanId) {
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
    return new File(getReportDir(appId), scanId);
  }

  /**
   * Should no longer be used in production code see
   * com.sonatype.insight.brain.report.ApplicationReportPersistenceService#getReportEntity(String, String, String)
   *
   * @param appId
   * @param scanId
   * @return
   */
  @Deprecated
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
    return TenantThreadLocal.runAsGlobal(
        () -> new File(getNodeCacheDir(), "enterpriseReportingDashboardIcons"));
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

  public File getSbomDir(final String appId, final boolean createIfNotExist) {
    IdValidationUtils.validate(appId);
    File sbomTempDir = new File(getSbomDir(), appId);

    if (!sbomTempDir.exists() && createIfNotExist) {
      try {
        Files.createDirectories(sbomTempDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed creating SBOM directory for appId " + appId, e);
      }
    }
    return sbomTempDir;
  }

  public File getSbomDir(final String appId) {
    return getSbomDir(appId, true);
  }

  /**
   * Parent dir of all "temporary" SBOM storage
   */
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

  /**
   * Directory where SBOM uploads are stored briefly during the processing of a single REST call. Files here should
   * have random names and not last longer than a single REST call
   */
  public File getSbomTransientDir() {
    File sbomTransientDir = new File(getSbomTempDir(), "transient");
    if (!sbomTransientDir.exists()) {
      try {
        Files.createDirectories(sbomTransientDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed creating SBOM temporary directory", e);
      }
    }
    return sbomTransientDir;
  }

  /**
   * Directory where binaries uploaded as SBOMs are stored by the detectSbom REST call, so that they can be picked
   * up by the importSbom REST call. importSbom deletes these files.
   */
  public File getSbomPersistentTempDir() {
    File sbomPersistentTempDir = new File(getSbomTempDir(), "persistent");
    if (!sbomPersistentTempDir.exists()) {
      try {
        Files.createDirectories(sbomPersistentTempDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed creating SBOM temporary directory", e);
      }
    }
    return sbomPersistentTempDir;
  }
}
