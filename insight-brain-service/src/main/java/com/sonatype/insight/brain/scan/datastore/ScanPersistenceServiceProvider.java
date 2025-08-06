/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

/**
 * Provider for ScanPersistenceService implementations.
 * Selects between file-based and S3-based persistence based on storage configuration.
 */
@Named
@Singleton
public class ScanPersistenceServiceProvider implements Provider<ScanPersistenceService>
{
  private final InsightConfig insightConfig;

  private final Provider<S3ScanPersistenceService> s3ScanPersistenceServiceProvider;

  private final Provider<FileScanPersistenceService> fileScanPersistenceServiceProvider;

  @Inject
  public ScanPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3ScanPersistenceService> s3ScanPersistenceServiceProvider,
      final Provider<FileScanPersistenceService> fileScanPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3ScanPersistenceServiceProvider = s3ScanPersistenceServiceProvider;
    this.fileScanPersistenceServiceProvider = fileScanPersistenceServiceProvider;
  }

  @Override
  public ScanPersistenceService get() {
    if (insightConfig.getStorage() == null) {
      return fileScanPersistenceServiceProvider.get();
    }
    
    return switch (insightConfig.getStorage().getType()) {
      case FILE -> fileScanPersistenceServiceProvider.get();
      case S3 -> s3ScanPersistenceServiceProvider.get();
    };
  }
}
