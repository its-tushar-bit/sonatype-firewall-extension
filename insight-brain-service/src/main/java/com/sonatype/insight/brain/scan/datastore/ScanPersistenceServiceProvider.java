/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;

/**
 * Provider for ScanPersistenceService implementations.
 * Selects between file-based, S3-based, and hyrbid persistence based on storage configuration.
 */
@Named
@Singleton
public class ScanPersistenceServiceProvider
    implements Provider<ScanPersistenceService>
{
  private final InsightConfig insightConfig;

  private final Provider<S3ScanPersistenceService> s3ScanPersistenceServiceProvider;

  private final Provider<FileScanPersistenceService> fileScanPersistenceServiceProvider;

  private final Provider<HybridScanPersistenceService> hybridScanPersistenceServiceProvider;

  @Inject
  public ScanPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3ScanPersistenceService> s3ScanPersistenceServiceProvider,
      final Provider<FileScanPersistenceService> fileScanPersistenceServiceProvider,
      final Provider<HybridScanPersistenceService> hybridScanPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3ScanPersistenceServiceProvider = s3ScanPersistenceServiceProvider;
    this.fileScanPersistenceServiceProvider = fileScanPersistenceServiceProvider;
    this.hybridScanPersistenceServiceProvider = hybridScanPersistenceServiceProvider;
  }

  @Override
  public ScanPersistenceService get() {
    if (insightConfig.getStorage() == null) {
      return fileScanPersistenceServiceProvider.get();
    }

    return get(insightConfig.getStorage().getType());
  }

  public ScanPersistenceService get(final DataStoreType dataStoreType) {
    return switch (dataStoreType) {
      case FILE -> fileScanPersistenceServiceProvider.get();
      case S3 -> s3ScanPersistenceServiceProvider.get();
      case HYBRID -> hybridScanPersistenceServiceProvider.get();
    };
  }
}
