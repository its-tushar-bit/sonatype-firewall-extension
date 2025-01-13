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
public class ReportDataStoreProvider implements Provider<ReportDataStore>
{
  private final InsightConfig insightConfig;

  private final Provider<S3ReportDataStore> s3ObjectStorageReportDataStoreProvider;

  private final Provider<FileReportDataStore> fileReportDataStoreProvider;

  @Inject
  public ReportDataStoreProvider(
      final InsightConfig insightConfig,
      final Provider<S3ReportDataStore> s3ReportDataStoreProvider,
      final Provider<FileReportDataStore> fileReportDataStoreProvider)
  {
    this.insightConfig = insightConfig;
    this.s3ObjectStorageReportDataStoreProvider = s3ReportDataStoreProvider;
    this.fileReportDataStoreProvider = fileReportDataStoreProvider;
  }

  @Override
  public ReportDataStore get() {
    if (insightConfig.getReportDataStoreConfig() == null) {
      return fileReportDataStoreProvider.get();
    }
    return switch (insightConfig.getReportDataStoreConfig().getType()) {
      case File -> fileReportDataStoreProvider.get();
      case S3 -> s3ObjectStorageReportDataStoreProvider.get();
    };
  }
}
