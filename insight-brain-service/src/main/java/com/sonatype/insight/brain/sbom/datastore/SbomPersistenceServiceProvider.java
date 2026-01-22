/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SbomPersistenceServiceProvider
    implements Provider<SbomPersistenceService>
{
  private static final Logger log = LoggerFactory.getLogger(SbomPersistenceServiceProvider.class);
  
  private final InsightConfig insightConfig;

  private final Provider<S3SbomPersistenceService> s3SbomPersistenceServiceProvider;

  private final Provider<FileSbomPersistenceService> fileSbomPersistenceServiceProvider;

  private final Provider<HybridSbomPersistenceService> hybridSbomPersistenceServiceProvider;

  @Inject
  public SbomPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3SbomPersistenceService> s3SbomPersistenceServiceProvider,
      final Provider<FileSbomPersistenceService> fileSbomPersistenceServiceProvider,
      final Provider<HybridSbomPersistenceService> hybridSbomPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3SbomPersistenceServiceProvider = s3SbomPersistenceServiceProvider;
    this.fileSbomPersistenceServiceProvider = fileSbomPersistenceServiceProvider;
    this.hybridSbomPersistenceServiceProvider = hybridSbomPersistenceServiceProvider;
  }

  @Override
  public SbomPersistenceService get() {
    if (insightConfig.getStorage() == null) {
      log.info("No storage config found, using FileSbomPersistenceService");
      return fileSbomPersistenceServiceProvider.get();
    }
    return get(insightConfig.getStorage().getType());
  }

  public SbomPersistenceService get(final DataStoreType dataStoreType) {
    return switch (dataStoreType) {
      case FILE -> {
        log.info("Using FileSbomPersistenceService");
        yield fileSbomPersistenceServiceProvider.get();
      }
      case S3 -> {
        log.info("Using S3SbomPersistenceService");
        yield s3SbomPersistenceServiceProvider.get();
      }
      case HYBRID -> {
        log.info("Using HybridSbomPersistenceService");
        yield hybridSbomPersistenceServiceProvider.get();
      }
    };
  }
}
