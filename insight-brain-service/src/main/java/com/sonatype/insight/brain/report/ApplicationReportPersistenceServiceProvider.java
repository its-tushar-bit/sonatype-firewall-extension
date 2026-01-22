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
public class ApplicationReportPersistenceServiceProvider
    implements Provider<ApplicationReportPersistenceService>
{
  private final InsightConfig insightConfig;

  private final Provider<S3ApplicationReportPersistenceService> s3ApplicationReportPersistenceServiceProvider;

  private final Provider<FileApplicationReportPersistenceService> fileApplicationReportPersistenceServiceProvider;

  private final Provider<HybridApplicationReportPersistenceService> hybridApplicationReportPersistenceServiceProvider;

  @Inject
  public ApplicationReportPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3ApplicationReportPersistenceService> s3ApplicationReportPersistenceServiceProvider,
      final Provider<FileApplicationReportPersistenceService> fileApplicationReportPersistenceServiceProvider,
      final Provider<HybridApplicationReportPersistenceService> hybridApplicationReportPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3ApplicationReportPersistenceServiceProvider = s3ApplicationReportPersistenceServiceProvider;
    this.fileApplicationReportPersistenceServiceProvider = fileApplicationReportPersistenceServiceProvider;
    this.hybridApplicationReportPersistenceServiceProvider = hybridApplicationReportPersistenceServiceProvider;
  }

  @Override
  public ApplicationReportPersistenceService get() {
    if (insightConfig.getStorage() == null) {
      return fileApplicationReportPersistenceServiceProvider.get();
    }
    return get(insightConfig.getStorage().getType());
  }

  public ApplicationReportPersistenceService get(final DataStoreType dataStoreType) {
    return switch (dataStoreType) {
      case FILE -> fileApplicationReportPersistenceServiceProvider.get();
      case S3 -> s3ApplicationReportPersistenceServiceProvider.get();
      case HYBRID -> hybridApplicationReportPersistenceServiceProvider.get();
    };
  }
}
