/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;

@Named
@Singleton
public class LifecycleReportPersistenceServiceProvider
    implements Provider<LifecycleReportPersistenceService>
{
  private final InsightConfig insightConfig;

  private final Provider<S3LifecycleReportPersistenceService> s3LifecycleReportPersistenceServiceProvider;

  private final Provider<FileLifecycleReportPersistenceService> fileLifecycleReportPersistenceServiceProvider;

  private final Provider<HybridLifecycleReportPersistenceService> hybridLifecycleReportPersistenceServiceProvider;

  @Inject
  public LifecycleReportPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3LifecycleReportPersistenceService> s3LifecycleReportPersistenceServiceProvider,
      final Provider<FileLifecycleReportPersistenceService> fileLifecycleReportPersistenceServiceProvider,
      final Provider<HybridLifecycleReportPersistenceService> hybridLifecycleReportPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3LifecycleReportPersistenceServiceProvider = s3LifecycleReportPersistenceServiceProvider;
    this.fileLifecycleReportPersistenceServiceProvider = fileLifecycleReportPersistenceServiceProvider;
    this.hybridLifecycleReportPersistenceServiceProvider = hybridLifecycleReportPersistenceServiceProvider;
  }

  @Override
  public LifecycleReportPersistenceService get() {
    if (insightConfig.getStorage() == null) {
      return fileLifecycleReportPersistenceServiceProvider.get();
    }
    return get(insightConfig.getStorage().getType());
  }

  public LifecycleReportPersistenceService get(final DataStoreType dataStoreType) {
    return switch (dataStoreType) {
      case FILE -> fileLifecycleReportPersistenceServiceProvider.get();
      case S3 -> s3LifecycleReportPersistenceServiceProvider.get();
      case HYBRID -> hybridLifecycleReportPersistenceServiceProvider.get();
    };
  }
}
