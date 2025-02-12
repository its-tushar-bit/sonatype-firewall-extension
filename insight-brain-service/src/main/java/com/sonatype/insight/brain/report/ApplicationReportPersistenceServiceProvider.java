/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

@Named
@Singleton
public class ApplicationReportPersistenceServiceProvider implements Provider<ApplicationReportPersistenceService>
{
  private final InsightConfig insightConfig;

  private final Provider<S3ApplicationReportPersistenceService> s3ApplicationReportPersistenceServiceProvider;

  private final Provider<FileApplicationReportPersistenceService> fileApplicationReportPersistenceServiceProvider;

  @Inject
  public ApplicationReportPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3ApplicationReportPersistenceService> s3ApplicationReportPersistenceServiceProvider,
      final Provider<FileApplicationReportPersistenceService> fileApplicationReportPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3ApplicationReportPersistenceServiceProvider = s3ApplicationReportPersistenceServiceProvider;
    this.fileApplicationReportPersistenceServiceProvider = fileApplicationReportPersistenceServiceProvider;
  }

  @Override
  public ApplicationReportPersistenceService get() {
    if (insightConfig.getReportDataStoreConfig() == null) {
      return fileApplicationReportPersistenceServiceProvider.get();
    }
    return switch (insightConfig.getReportDataStoreConfig().getType()) {
      case FILE -> fileApplicationReportPersistenceServiceProvider.get();
      case S3 -> s3ApplicationReportPersistenceServiceProvider.get();
    };
  }
}
