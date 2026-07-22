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
public class HybridLifecycleReportPersistenceService
    extends LifecycleReportPersistenceService
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(HybridLifecycleReportPersistenceService.class);

  private final List<LifecycleReportPersistenceService> lifecycleReportPersistenceServices;

  private final Provider<ApiConfigurationService> apiConfigurationServiceProvider;

  private volatile boolean warnOnNonPrimaryStorageAccess;

  @Inject
  public HybridLifecycleReportPersistenceService(
      final InsightConfig config,
      final Provider<LifecycleReportPersistenceServiceProvider> lifecycleReportPersistenceServiceProviderProvider,
      final Provider<ApiConfigurationService> apiConfigurationServiceProvider)
  {
    StorageConfig storageConfig = config.getStorage();
    HybridDataStoreConfig hybridDataStoreConfig = storageConfig == null ? null : storageConfig.getHybridConfig();
    LinkedHashSet<DataStoreType> types =
        hybridDataStoreConfig == null ? new LinkedHashSet<>() : hybridDataStoreConfig.getTypes();
    /*
     * The order of this collection is significant:
     * 1. The first element is the default storage mechanism.
     * - All writes are directed here.
     * 2. The remaining elements act as backup storage mechanisms.
     *
     * Behavior:
     * - Reads are attempted in order, starting from the default.
     * - Writes go only to the default storage.
     * - Deletes are applied to all storage mechanisms.
     */
    lifecycleReportPersistenceServices =
        types.stream().map(t -> lifecycleReportPersistenceServiceProviderProvider.get().get(t)).toList();
    this.apiConfigurationServiceProvider = apiConfigurationServiceProvider;
    warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get()
        .getConfigurationNoAuthz(
            SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
  }

  @Override
  protected ReportEntity doGetReportEntity(
      final String ownerId,
      final String scanId,
      final String name) throws IOException
  {
    ReportEntity reportEntity = hybridDoGetReportEntity(ownerId, scanId, name);
    log.trace("Getting report entity '{}' for owner id '{}' and scan id '{}' using {}.",
        name,
        ownerId,
        scanId,
        reportEntity.getLifecycleReportPersistenceServiceClass());
    if (warnOnNonPrimaryStorageAccess && !lifecycleReportPersistenceServices.get(0)
        .getClass()
        .equals(reportEntity.getLifecycleReportPersistenceServiceClass()))
    {
      log.warn("Non-primary storage access for report entity '{}' for owner id '{}' and scan id '{}' using {}.",
          name,
          ownerId,
          scanId,
          reportEntity.getLifecycleReportPersistenceServiceClass());
    }
    return reportEntity;
  }

  private ReportEntity hybridDoGetReportEntity(
      final String ownerId,
      final String scanId,
      final String name) throws IOException
  {
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      if (lifecycleReportPersistenceService.reportExists(ownerId, scanId)) {
        return lifecycleReportPersistenceService.doGetReportEntity(ownerId, scanId, name);
      }
    }
    return lifecycleReportPersistenceServices.get(0).doGetReportEntity(ownerId, scanId, name);
  }

  @Override
  public Stream<ReportEntity> getAllReportEntities(final String ownerId, final String scanId) throws IOException {
    return hybridGetAllReportEntities(ownerId, scanId);
  }

  @Override
  public Stream<ReportEntity> getOriginalReportEntities(final String ownerId, final String scanId) {
    throw new UnsupportedOperationException();
  }

  private Stream<ReportEntity> hybridGetAllReportEntities(
      final String ownerId,
      final String scanId) throws IOException
  {
    LifecycleReportPersistenceService targetService = lifecycleReportPersistenceServices.get(0);
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      if (lifecycleReportPersistenceService.reportExists(ownerId, scanId)) {
        targetService = lifecycleReportPersistenceService;
        break;
      }
    }
    log.trace("Getting all report entities for owner id '{}' and scan id '{}' using {}.",
        ownerId,
        scanId,
        targetService.getClass());
    if (warnOnNonPrimaryStorageAccess &&
        !lifecycleReportPersistenceServices.get(0).getClass().equals(targetService.getClass()))
    {
      log.warn("Non-primary storage access for all report entities for owner id '{}' and scan id '{}' using {}.",
          ownerId,
          scanId,
          targetService.getClass());
    }
    return targetService.getAllReportEntities(ownerId, scanId);
  }

  @Override
  public void saveOriginalReport(
      final String ownerId,
      final String scanId,
      final InputStream reportZipContents) throws IOException
  {
    lifecycleReportPersistenceServices.get(0).saveOriginalReport(ownerId, scanId, reportZipContents);
  }

  @Override
  public void saveOriginalReportEntities(
      final String ownerId,
      final String scanId,
      final Stream<ReportEntity> originalReportEntities) throws IOException
  {
    lifecycleReportPersistenceServices.get(0)
        .saveOriginalReportEntities(ownerId, scanId, originalReportEntities);
  }

  @Override
  public void moveReport(
      final String appId,
      final String sourceScanId,
      final String destinationScanId) throws IOException
  {
    lifecycleReportPersistenceServices.get(0).moveReport(appId, sourceScanId, destinationScanId);
  }

  @Override
  protected void doSaveReportFile(
      final String ownerId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    lifecycleReportPersistenceServices.get(0).doSaveReportFile(ownerId, scanId, name, contents);
  }

  @Override
  protected void doSaveAdditionalReportFile(
      final String ownerId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    lifecycleReportPersistenceServices.get(0).doSaveAdditionalReportFile(ownerId, scanId, name, contents);
  }

  @Override
  public ReportPdfEntity getPdfEntity(final String ownerId, final String scanId) {
    try {
      ReportPdfEntity reportPdfEntity = hybridGetPdfEntity(ownerId, scanId);
      log.trace("Getting report pdf entity for owner id '{}' and scan id '{}' using {}.",
          ownerId,
          scanId,
          reportPdfEntity.getLifecycleReportPersistenceServiceClass());
      if (warnOnNonPrimaryStorageAccess &&
          !lifecycleReportPersistenceServices.get(0)
              .getReportEntityClass()
              .isAssignableFrom(reportPdfEntity.getClass()))
      {
        log.warn("Non-primary storage access for report pdf entity for owner id '{}' and scan id '{}' using {}.",
            ownerId,
            scanId,
            reportPdfEntity.getLifecycleReportPersistenceServiceClass());
      }
      return reportPdfEntity;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private ReportPdfEntity hybridGetPdfEntity(final String ownerId, final String scanId) throws IOException {
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      if (lifecycleReportPersistenceService.getPdfEntity(ownerId, scanId).exists()) {
        return lifecycleReportPersistenceService.getPdfEntity(ownerId, scanId);
      }
    }
    return lifecycleReportPersistenceServices.get(0).getPdfEntity(ownerId, scanId);
  }

  @Override
  public BaseReportEntity getVulnerabilitySignaturesEntity(final String ownerId, final String scanId) {
    try {
      BaseReportEntity baseReportEntity = hybridGetVulnerabilitySignaturesEntity(ownerId, scanId);
      log.trace("Getting vulnerability signatures entity for owner id '{}' and scan id '{}' using {}.",
          ownerId,
          scanId,
          baseReportEntity.getLifecycleReportPersistenceServiceClass());
      if (warnOnNonPrimaryStorageAccess &&
          !lifecycleReportPersistenceServices.get(0)
              .getReportEntityClass()
              .isAssignableFrom(baseReportEntity.getClass()))
      {
        log.warn(
            "Non-primary storage access for vulnerability signatures entity for owner id '{}' and scan id '{}' using {}.",
            ownerId,
            scanId,
            baseReportEntity.getLifecycleReportPersistenceServiceClass());
      }
      return baseReportEntity;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BaseReportEntity hybridGetVulnerabilitySignaturesEntity(
      final String ownerId,
      final String scanId) throws IOException
  {
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      if (lifecycleReportPersistenceService.getVulnerabilitySignaturesEntity(ownerId, scanId).exists()) {
        return lifecycleReportPersistenceService.getVulnerabilitySignaturesEntity(ownerId, scanId);
      }
    }
    return lifecycleReportPersistenceServices.get(0).getVulnerabilitySignaturesEntity(ownerId, scanId);
  }

  @Override
  public String getReportLocation(final String ownerId, final String scanId) {
    try {
      for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
        if (lifecycleReportPersistenceService.reportExists(ownerId, scanId)) {
          return lifecycleReportPersistenceService.getReportLocation(ownerId, scanId);
        }
      }
      return lifecycleReportPersistenceServices.get(0).getReportLocation(ownerId, scanId);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean reportExists(final String ownerId, final String scanId) throws IOException {
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      if (lifecycleReportPersistenceService.reportExists(ownerId, scanId)) {
        log.trace("Report for ownerId {} and scanId {} exists using {}.",
            ownerId,
            scanId,
            lifecycleReportPersistenceService.getClass());
        return true;
      }
    }
    return false;
  }

  @Override
  public void deleteReport(final String ownerId, final String scanId) throws IOException {
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      lifecycleReportPersistenceService.deleteReport(ownerId, scanId);
    }
  }

  @Override
  public void deleteReports(final String ownerId) throws IOException {
    for (LifecycleReportPersistenceService lifecycleReportPersistenceService : lifecycleReportPersistenceServices) {
      lifecycleReportPersistenceService.deleteReports(ownerId);
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
      warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get()
          .getConfigurationNoAuthz(
              SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
    }
  }
}
