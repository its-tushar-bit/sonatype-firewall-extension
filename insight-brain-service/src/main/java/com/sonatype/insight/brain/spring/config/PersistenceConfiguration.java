/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceServiceProvider;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceServiceProvider;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PersistenceConfiguration
{
  @Bean
  @Primary
  public ScanPersistenceService scanPersistenceService(final ScanPersistenceServiceProvider provider) {
    return provider.get();
  }

  @Bean
  @Primary
  public LifecycleReportPersistenceService lifecycleReportPersistenceService(
      final LifecycleReportPersistenceServiceProvider provider)
  {
    return provider.get();
  }

  @Bean
  @Primary
  public SbomPersistenceService sbomPersistenceService(final SbomPersistenceServiceProvider provider) {
    return provider.get();
  }
}
