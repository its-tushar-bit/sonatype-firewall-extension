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
import javax.inject.Inject;
import javax.inject.Provider;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HybridSbomPersistenceService
    extends SbomPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(HybridSbomPersistenceService.class);

  private final List<SbomPersistenceService> sbomPersistenceServices;

  private final Map<Class<? extends SbomPersistenceService>, SbomPersistenceService> sbomPersistenceServiceByClass;

  @Inject
  public HybridSbomPersistenceService(
      final InsightConfig config,
      final Provider<SbomPersistenceServiceProvider> sbomPersistenceServiceProviderProvider)
  {
    HybridDataStoreConfig hybridDataStoreConfig = config.getStorage().getHybridConfig();
    LinkedHashSet<DataStoreType> types = hybridDataStoreConfig.getTypes();
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
  }

  @Override
  public SbomEntity doGetSbom(final String appId, final String fileName) {
    SbomEntity sbomEntity = hybridDoGetSbom(appId, fileName);
    log.trace("Getting SBOM by app id and file name from {}.", sbomEntity.getLocation());
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
    return hybridGetTransientSbom(fileName);
  }

  private SbomEntity hybridGetTransientSbom(final String fileName) throws IOException {
    SbomEntity transientEntity = sbomPersistenceServices.get(0).getTransientSbom(fileName);
    log.trace("Getting transient SBOM by file name from {}.", transientEntity.getLocation());
    return transientEntity;
  }

  @Override
  public SbomEntity saveTemporarySbom(final SbomEntity sbomEntity, final String fileName, @Nullable final String prefix)
      throws IOException
  {
    SbomPersistenceService entityPersistenceService =
        sbomPersistenceServiceByClass.get(sbomEntity.getScanPersistenceServiceClass());
    SbomPersistenceService primaryPersistenceService = sbomPersistenceServices.get(0);
    if (primaryPersistenceService.getClass().equals(entityPersistenceService.getClass())) {
      return entityPersistenceService.saveTemporarySbom(sbomEntity, fileName, prefix);
    }
    log.warn("Unexpected SBOM copy from {} to {}.", sbomEntity.getScanPersistenceServiceClass(),
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
        sbomPersistenceServiceByClass.get(sbomEntity.getScanPersistenceServiceClass());
    if (persistenceService == null) {
      log.warn("SbomPersistenceService {}, cannot delete {}.", sbomEntity.getScanPersistenceServiceClass(),
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

  private void copy(final SbomEntity source, final SbomEntity target) throws IOException {
    try (InputStream inputStream = source.getInputStream(); OutputStream outputStream = target.getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }
}
