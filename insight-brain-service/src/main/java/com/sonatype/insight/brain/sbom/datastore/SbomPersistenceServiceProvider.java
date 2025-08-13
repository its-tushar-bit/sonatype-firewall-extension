/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SbomPersistenceServiceProvider implements Provider<SbomPersistenceService>
{
  private static final Logger log = LoggerFactory.getLogger(SbomPersistenceServiceProvider.class);
  
  private final InsightConfig insightConfig;

  private final Provider<S3SbomPersistenceService> s3SbomPersistenceServiceProvider;

  private final Provider<FileSbomPersistenceService> fileSbomPersistenceServiceProvider;

  @Inject
  public SbomPersistenceServiceProvider(
      final InsightConfig insightConfig,
      final Provider<S3SbomPersistenceService> s3SbomPersistenceServiceProvider,
      final Provider<FileSbomPersistenceService> fileSbomPersistenceServiceProvider)
  {
    this.insightConfig = insightConfig;
    this.s3SbomPersistenceServiceProvider = s3SbomPersistenceServiceProvider;
    this.fileSbomPersistenceServiceProvider = fileSbomPersistenceServiceProvider;
  }

  @Override
  public SbomPersistenceService get() {
    if (insightConfig.getStorage() == null) {
      log.info("No storage config found, using FileSbomPersistenceService");
      return fileSbomPersistenceServiceProvider.get();
    }
    return switch (insightConfig.getStorage().getType()) {
      case FILE, HYBRID -> {
        log.info("Using FileSbomPersistenceService");
        yield fileSbomPersistenceServiceProvider.get();
      }
      case S3 -> {
        log.info("Using S3SbomPersistenceService");
        yield s3SbomPersistenceServiceProvider.get();
      }
    };
  }
}
