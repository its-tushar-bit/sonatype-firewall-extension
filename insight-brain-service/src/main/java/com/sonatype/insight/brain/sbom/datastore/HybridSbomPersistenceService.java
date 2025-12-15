/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class HybridSbomPersistenceService
    extends SbomPersistenceService
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(HybridSbomPersistenceService.class);

  private final List<SbomPersistenceService> sbomPersistenceServices;

  private final Map<Class<? extends SbomPersistenceService>, SbomPersistenceService> sbomPersistenceServiceByClass;

  private final Provider<ApiConfigurationService> apiConfigurationServiceProvider;

  private volatile boolean warnOnNonPrimaryStorageAccess;

  @Inject
  public HybridSbomPersistenceService(
      final InsightConfig config,
      final Provider<SbomPersistenceServiceProvider> sbomPersistenceServiceProviderProvider,
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
     * - Deletes are applied to all storage mechanisms unless an SBOM entity is specified, in this case,
     *   the deletion is applied only to the storage mechanism that created the SBOM entity.
     */
    sbomPersistenceServices = types.stream()
        .map(t -> sbomPersistenceServiceProviderProvider.get().get(t)).toList();
    sbomPersistenceServiceByClass = new HashMap<>();
    for (SbomPersistenceService sbomPersistenceService : sbomPersistenceServices) {
      sbomPersistenceServiceByClass.put(sbomPersistenceService.getClass(), sbomPersistenceService);
    }
    this.apiConfigurationServiceProvider = apiConfigurationServiceProvider;
    warnOnNonPrimaryStorageAccess = (boolean) this.apiConfigurationServiceProvider.get().getConfigurationNoAuthz(
        SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS);
  }

  @Override
  public SbomEntity doGetSbom(final String appId, final String fileName) {
    SbomEntity sbomEntity = hybridDoGetSbom(appId, fileName);
    log.trace("Getting SBOM by app id and file name from {}.", sbomEntity.getLocation());
    if (warnOnNonPrimaryStorageAccess &&
        !sbomPersistenceServices.get(0).getClass().equals(sbomEntity.getSbomPersistenceServiceClass())) {
      log.warn("Non-primary storage access for SBOM by app id and file name from {}.", sbomEntity.getLocation());
    }
    return sbomEntity;
  }

  private SbomEntity hybridDoGetSbom(final String appId, final String fileName) {
    for (SbomPersistenceService sbomPersistenceService : sbomPersistenceServices) {
      SbomEntity sbomEntity = sbomPersistenceService.doGetSbom(appId, fileName);
      if (sbomEntity.exists()) {
        return sbomEntity;
      }
    }
    return sbomPersistenceServices.get(0).doGetSbom(appId, fileName);
  }

  @Override
  public SbomEntity getTemporarySbom(final String fileName, @Nullable final String prefix) {
    SbomEntity sbomEntity = hybridGetTemporarySbom(fileName, prefix);
    log.trace("Getting temporary SBOM by file name and prefix from {}.", sbomEntity.getLocation());
    if (warnOnNonPrimaryStorageAccess &&
        !sbomPersistenceServices.get(0).getClass().equals(sbomEntity.getSbomPersistenceServiceClass())) {
      log.warn("Non-primary storage access for temporary SBOM by file name and prefix from {}.",
          sbomEntity.getLocation());
    }
    return sbomEntity;
  }

  private SbomEntity hybridGetTemporarySbom(final String fileName, @Nullable final String prefix) {
    for (SbomPersistenceService sbomPersistenceService : sbomPersistenceServices) {
      SbomEntity sbomEntity = sbomPersistenceService.getTemporarySbom(fileName, prefix);
      if (sbomEntity.exists()) {
        return sbomEntity;
      }
    }
    return sbomPersistenceServices.get(0).getTemporarySbom(fileName, prefix);
  }

  @Override
  public SbomEntity getTransientSbom(final String fileName) throws IOException {
    return sbomPersistenceServices.get(0).getTransientSbom(fileName);
  }

  @Override
  public SbomEntity saveTemporarySbom(final SbomEntity sbomEntity, final String fileName, @Nullable final String prefix)
      throws IOException
  {
    SbomPersistenceService entityPersistenceService =
        sbomPersistenceServiceByClass.get(sbomEntity.getSbomPersistenceServiceClass());
    SbomPersistenceService primaryPersistenceService = sbomPersistenceServices.get(0);
    if (primaryPersistenceService.getClass().equals(entityPersistenceService.getClass())) {
      return entityPersistenceService.saveTemporarySbom(sbomEntity, fileName, prefix);
    }
    log.warn("Unexpected SBOM copy from {} to {}.", sbomEntity.getSbomPersistenceServiceClass(),
        primaryPersistenceService.getClass());
    SbomEntity writeTransientEntity = null;
    try {
      writeTransientEntity = primaryPersistenceService.getTransientSbom(fileName);
      copy(sbomEntity, writeTransientEntity);
      return primaryPersistenceService.saveTemporarySbom(writeTransientEntity, fileName, prefix);
    }
    finally {
      if (writeTransientEntity != null) {
        primaryPersistenceService.deleteSbom(writeTransientEntity);
      }
    }
  }

  @Override
  public void deleteSbom(final SbomEntity sbomEntity) throws IOException {
    SbomPersistenceService persistenceService =
        sbomPersistenceServiceByClass.get(sbomEntity.getSbomPersistenceServiceClass());
    if (persistenceService == null) {
      log.warn("SbomPersistenceService {}, cannot delete {}.", sbomEntity.getSbomPersistenceServiceClass(),
          sbomEntity.getLocation());
    }
    else {
      persistenceService.deleteSbom(sbomEntity);
    }
  }

  @Override
  public void deleteSbom(final String appId, final String fileName) throws IOException {
    for (SbomPersistenceService sbomPersistenceService : sbomPersistenceServices) {
      sbomPersistenceService.deleteSbom(appId, fileName);
    }
  }

  @Override
  public void deleteSbomsFor(final String appId) throws IOException {
    for (SbomPersistenceService sbomPersistenceService : sbomPersistenceServices) {
      sbomPersistenceService.deleteSbomsFor(appId);
    }
  }

  @Override
  public void deleteTransientSbomsOlderThan(final Instant instant) throws IOException {
    for (SbomPersistenceService sbomPersistenceService : sbomPersistenceServices) {
      sbomPersistenceService.deleteTransientSbomsOlderThan(instant);
    }
  }

  @Override
  public void moveSbomEntity(final SbomEntity from, final SbomEntity to) {
    throw new UnsupportedOperationException();
  }

  private void copy(final SbomEntity source, final SbomEntity target) throws IOException {
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
