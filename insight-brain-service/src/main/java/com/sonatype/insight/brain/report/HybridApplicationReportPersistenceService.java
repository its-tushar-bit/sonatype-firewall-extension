/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class HybridApplicationReportPersistenceService
    extends ApplicationReportPersistenceService
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(HybridApplicationReportPersistenceService.class);

  private final List<ApplicationReportPersistenceService> applicationReportPersistenceServices;

  private final Provider<ApiConfigurationService> apiConfigurationServiceProvider;

  private volatile boolean warnOnNonPrimaryStorageAccess;

  @Inject
  public HybridApplicationReportPersistenceService(
      final InsightConfig config,
      final Provider<ApplicationReportPersistenceServiceProvider> applicationReportPersistenceServiceProviderProvider,
      final Provider<ApiConfigurationService> apiConfigurationServiceProvider)
  {
    StorageConfig storageConfig = config.getStorage();
    HybridDataStoreConfig hybridDataStoreConfig = storageConfig == null ? null : storageConfig.getHybridConfig();
    LinkedHashSet<DataStoreType> types =
        hybridDataStoreConfig == null ? new LinkedHashSet<>() : hybridDataStoreConfig.getTypes();
    /*
     * The order of this collection is significant:
     * 1. The first element is the default storage mechanism.
     *    - All writes are directed here.
     * 2. The remaining elements act as backup storage mechanisms.
     *
     * Behavior:
     * - Reads are attempted in order, starting from the default.
     * - Writes go only to the default storage.
     * - Deletes are applied to all storage mechanisms.
     */
    applicationReportPersistenceServices =
        types.stream().map(t -> applicationReportPersistenceServiceProviderProvider.get().get(t)).toList();
    this.apiConfigurationServiceProvider = apiConfigurationServiceProvider;
    warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get().getConfigurationNoAuthz(
        SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
  }

  @Override
  protected ReportEntity doGetReportEntity(final String applicationId, final String scanId, final String name)
      throws IOException
  {
    ReportEntity reportEntity = hybridDoGetReportEntity(applicationId, scanId, name);
    log.trace("Getting report entity '{}' for app id '{}' and scan id '{}' using {}.",
        name,
        applicationId,
        scanId,
        reportEntity.getApplicationReportPersistenceServiceClass()
    );
    if (warnOnNonPrimaryStorageAccess && !applicationReportPersistenceServices.get(0).getClass()
        .equals(reportEntity.getApplicationReportPersistenceServiceClass())) {
      log.warn("Non-primary storage access for report entity '{}' for app id '{}' and scan id '{}' using {}.",
          name,
          applicationId,
          scanId,
          reportEntity.getApplicationReportPersistenceServiceClass()
      );
    }
    return reportEntity;
  }

  private ReportEntity hybridDoGetReportEntity(final String applicationId, final String scanId, final String name)
      throws IOException
  {
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      if (applicationReportPersistenceService.reportExists(applicationId, scanId)) {
        return applicationReportPersistenceService.doGetReportEntity(applicationId, scanId, name);
      }
    }
    return applicationReportPersistenceServices.get(0).doGetReportEntity(applicationId, scanId, name);
  }

  @Override
  public Stream<ReportEntity> getAllReportEntities(final String applicationId, final String scanId) throws IOException {
    return hybridGetAllReportEntities(applicationId, scanId);
  }

  @Override
  public Stream<ReportEntity> getOriginalReportEntities(final String applicationId, final String scanId) {
    throw new UnsupportedOperationException();
  }

  private Stream<ReportEntity> hybridGetAllReportEntities(final String applicationId, final String scanId)
      throws IOException
  {
    ApplicationReportPersistenceService targetService = applicationReportPersistenceServices.get(0);
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      if (applicationReportPersistenceService.reportExists(applicationId, scanId)) {
        targetService = applicationReportPersistenceService;
        break;
      }
    }
    log.trace("Getting all report entities for app id '{}' and scan id '{}' using {}.",
        applicationId,
        scanId,
        targetService.getClass()
    );
    if (warnOnNonPrimaryStorageAccess &&
        !applicationReportPersistenceServices.get(0).getClass().equals(targetService.getClass())) {
      log.warn("Non-primary storage access for all report entities for app id '{}' and scan id '{}' using {}.",
          applicationId,
          scanId,
          targetService.getClass()
      );
    }
    return targetService.getAllReportEntities(applicationId, scanId);
  }

  @Override
  public void saveOriginalReport(final String applicationId, final String scanId, final InputStream reportZipContents)
      throws IOException
  {
    applicationReportPersistenceServices.get(0).saveOriginalReport(applicationId, scanId, reportZipContents);
  }

  @Override
  public void saveOriginalReportEntities(
      final String applicationId,
      final String scanId,
      final Stream<ReportEntity> originalReportEntities) throws IOException
  {
    applicationReportPersistenceServices.get(0)
        .saveOriginalReportEntities(applicationId, scanId, originalReportEntities);
  }

  @Override
  public void moveReport(final String appId, final String sourceScanId, final String destinationScanId)
      throws IOException
  {
    applicationReportPersistenceServices.get(0).moveReport(appId, sourceScanId, destinationScanId);
  }

  @Override
  protected void doSaveReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    applicationReportPersistenceServices.get(0).doSaveReportFile(applicationId, scanId, name, contents);
  }

  @Override
  protected void doSaveAdditionalReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    applicationReportPersistenceServices.get(0).doSaveAdditionalReportFile(applicationId, scanId, name, contents);
  }

  @Override
  public ReportPdfEntity getPdfEntity(final String applicationId, final String scanId) {
    try {
      ReportPdfEntity reportPdfEntity = hybridGetPdfEntity(applicationId, scanId);
      log.trace("Getting report pdf entity for app id '{}' and scan id '{}' using {}.",
          applicationId,
          scanId,
          reportPdfEntity.getApplicationReportPersistenceServiceClass()
      );
      if (warnOnNonPrimaryStorageAccess &&
          !applicationReportPersistenceServices.get(0).getReportEntityClass()
              .isAssignableFrom(reportPdfEntity.getClass())) {
        log.warn("Non-primary storage access for report pdf entity for app id '{}' and scan id '{}' using {}.",
            applicationId,
            scanId,
            reportPdfEntity.getApplicationReportPersistenceServiceClass()
        );
      }
      return reportPdfEntity;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private ReportPdfEntity hybridGetPdfEntity(final String applicationId, final String scanId) throws IOException {
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      if (applicationReportPersistenceService.getPdfEntity(applicationId, scanId).exists()) {
        return applicationReportPersistenceService.getPdfEntity(applicationId, scanId);
      }
    }
    return applicationReportPersistenceServices.get(0).getPdfEntity(applicationId, scanId);
  }

  @Override
  public BaseReportEntity getVulnerabilitySignaturesEntity(final String applicationId, final String scanId) {
    try {
      BaseReportEntity baseReportEntity = hybridGetVulnerabilitySignaturesEntity(applicationId, scanId);
      log.trace("Getting vulnerability signatures entity for app id '{}' and scan id '{}' using {}.",
          applicationId,
          scanId,
          baseReportEntity.getApplicationReportPersistenceServiceClass()
      );
      if (warnOnNonPrimaryStorageAccess &&
          !applicationReportPersistenceServices.get(0).getReportEntityClass()
              .isAssignableFrom(baseReportEntity.getClass())) {
        log.warn(
            "Non-primary storage access for vulnerability signatures entity for app id '{}' and scan id '{}' using {}.",
            applicationId,
            scanId,
            baseReportEntity.getApplicationReportPersistenceServiceClass()
        );
      }
      return baseReportEntity;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BaseReportEntity hybridGetVulnerabilitySignaturesEntity(final String applicationId, final String scanId)
      throws IOException
  {
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      if (applicationReportPersistenceService.getVulnerabilitySignaturesEntity(applicationId, scanId).exists()) {
        return applicationReportPersistenceService.getVulnerabilitySignaturesEntity(applicationId, scanId);
      }
    }
    return applicationReportPersistenceServices.get(0).getVulnerabilitySignaturesEntity(applicationId, scanId);
  }

  @Override
  public String getReportLocation(final String applicationId, final String scanId) {
    try {
      for (ApplicationReportPersistenceService applicationReportPersistenceService :
          applicationReportPersistenceServices) {
        if (applicationReportPersistenceService.reportExists(applicationId, scanId)) {
          return applicationReportPersistenceService.getReportLocation(applicationId, scanId);
        }
      }
      return applicationReportPersistenceServices.get(0).getReportLocation(applicationId, scanId);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean reportExists(final String applicationId, final String scanId) throws IOException {
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      if (applicationReportPersistenceService.reportExists(applicationId, scanId)) {
        log.trace("Report for applicationId {} and scanId {} exists using {}.",
            applicationId,
            scanId,
            applicationReportPersistenceService.getClass()
        );
        return true;
      }
    }
    return false;
  }

  @Override
  public void deleteReport(final String applicationId, final String scanId) throws IOException {
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      applicationReportPersistenceService.deleteReport(applicationId, scanId);
    }
  }

  @Override
  public void deleteReports(final String applicationId) throws IOException {
    for (ApplicationReportPersistenceService applicationReportPersistenceService :
        applicationReportPersistenceServices) {
      applicationReportPersistenceService.deleteReports(applicationId);
    }
  }

  @Override
  public void deleteReportEntity(final ReportEntity reportEntity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? extends ReportEntity> getReportEntityClass() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS)) {
      warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get().getConfigurationNoAuthz(
          SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
    }
  }
}
