/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

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
public class HybridScanPersistenceService
    extends ScanPersistenceService
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(HybridScanPersistenceService.class);

  private final List<ScanPersistenceService> scanPersistenceServices;

  private final Map<Class<? extends ScanPersistenceService>, ScanPersistenceService> scanPersistenceServiceByClass;

  private final Provider<ApiConfigurationService> apiConfigurationServiceProvider;

  private volatile boolean warnOnNonPrimaryStorageAccess;

  @Inject
  public HybridScanPersistenceService(
      final InsightConfig config,
      final Provider<ScanPersistenceServiceProvider> scanPersistenceServiceProviderProvider,
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
    scanPersistenceServices = types.stream().map(t -> scanPersistenceServiceProviderProvider.get().get(t)).toList();
    scanPersistenceServiceByClass = new HashMap<>();
    for (ScanPersistenceService scanPersistenceService : scanPersistenceServices) {
      scanPersistenceServiceByClass.put(scanPersistenceService.getClass(), scanPersistenceService);
    }
    this.apiConfigurationServiceProvider = apiConfigurationServiceProvider;
    warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get().getConfigurationNoAuthz(
        SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
  }

  @Override
  protected ScanEntity doGetScan(final String appId, final String scanId) {
    ScanEntity scanEntity = hybridDoGetScan(appId, scanId);
    log.trace("Getting scan by id from {}.", scanEntity.getLocation());
    if (warnOnNonPrimaryStorageAccess &&
        !scanPersistenceServices.get(0).getClass().equals(scanEntity.getScanPersistenceServiceClass())) {
      log.warn("Non-primary storage access for scan by id from {}.", scanEntity.getLocation());
    }
    return scanEntity;
  }

  private ScanEntity hybridDoGetScan(final String appId, final String scanId) {
    for (ScanPersistenceService scanPersistenceServices : scanPersistenceServices) {
      ScanEntity scanEntity = scanPersistenceServices.doGetScan(appId, scanId);
      if (scanEntity.exists()) {
        return scanEntity;
      }
    }
    return scanPersistenceServices.get(0).doGetScan(appId, scanId);
  }

  @Override
  public ScanEntity createTempScan(final String appId) throws IOException {
    return scanPersistenceServices.get(0).createTempScan(appId);
  }

  @Override
  public void moveTempScan(final ScanEntity tempScanEntity, final String appId, final String scanId)
      throws IOException
  {
    ScanEntity targetScanEntity = scanPersistenceServices.get(0).getScan(appId, scanId);
    if (tempScanEntity.getClass() != targetScanEntity.getClass()) {
      log.warn("Unexpected scan move from {} to {}.", tempScanEntity.getLocation(), targetScanEntity.getLocation());
      copy(tempScanEntity, targetScanEntity);
      tempScanEntity.delete();
      return;
    }
    scanPersistenceServices.get(0).moveTempScan(tempScanEntity, appId, scanId);
  }

  @Override
  public ScanEntity getScanByName(final String appId, final String name) {
    ScanEntity scanEntity = hybridGetScanByName(appId, name);
    log.trace("Getting scan by name from {}.", scanEntity.getLocation());
    return scanEntity;
  }

  private ScanEntity hybridGetScanByName(final String appId, final String name) {
    for (ScanPersistenceService scanPersistenceServices : scanPersistenceServices) {
      ScanEntity scanEntity = scanPersistenceServices.getScanByName(appId, name);
      if (scanEntity.exists()) {
        return scanEntity;
      }
    }
    return scanPersistenceServices.get(0).getScanByName(appId, name);
  }

  @Override
  public void copyScanFile(final ScanEntity source, final ScanEntity destination) throws IOException {
    log.trace("Copying scan from {} to {}.", source.getLocation(), destination.getLocation());

    if (!scanPersistenceServiceByClass.containsKey(source.getScanPersistenceServiceClass()) ||
        !scanPersistenceServiceByClass.containsKey(destination.getScanPersistenceServiceClass())) {
      throw new IllegalStateException("Source and/or destination scan files are on unsupported storage mechanisms.");
    }

    if (source.getClass() == destination.getClass()) {
      scanPersistenceServiceByClass.get(source.getScanPersistenceServiceClass()).copyScanFile(source, destination);
    }
    else {
      if (destination.getClass() != scanPersistenceServices.get(0).getScanEntityClass()) {
        log.warn("Unexpected scan copy from {} to {}.", source.getLocation(), destination.getLocation());
      }
      copy(source, destination);
    }
  }

  @Override
  public void deleteScansFor(final String appId) throws IOException {
    for (ScanPersistenceService scanPersistenceService : scanPersistenceServices) {
      scanPersistenceService.deleteScansFor(appId);
    }
  }

  @Override
  public Stream<ScanEntity> allScanFilesFor(final String appId) {
    return scanPersistenceServices.stream()
        .flatMap(scanPersistenceService -> scanPersistenceService.allScanFilesFor(appId));
  }

  @Override
  public void deleteScan(final String appId, final String scanId) throws IOException {
    for (ScanPersistenceService scanPersistenceService : scanPersistenceServices) {
      scanPersistenceService.deleteScan(appId, scanId);
    }
  }

  @Override
  public Class<? extends ScanEntity> getScanEntityClass() {
    throw new UnsupportedOperationException();
  }

  private static void copy(final ScanEntity source, final ScanEntity target) throws IOException {
    try (InputStream inputStream = source.getInputStream(); OutputStream outputStream = target.getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS)) {
      warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get().getConfigurationNoAuthz(
          SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
    }
  }
}
