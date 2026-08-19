/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceServiceProvider;
import com.sonatype.insight.brain.report.FileLifecycleReportPersistenceService;
import com.sonatype.insight.brain.report.HybridLifecycleReportPersistenceService;
import com.sonatype.insight.brain.report.S3LifecycleReportPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.FileSbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.HybridSbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.S3SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceServiceProvider;
import com.sonatype.insight.brain.scan.datastore.FileScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.HybridScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.S3ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import jakarta.inject.Provider;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class PersistenceConfigurationTest
{
  @Test
  public void shouldUseFilePersistenceServicesByDefault() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext(new InsightConfig())) {
      assertThat(context.getBean(ScanPersistenceService.class))
          .isSameAs(context.getBean(FileScanPersistenceService.class));
      assertThat(context.getBean(LifecycleReportPersistenceService.class))
          .isSameAs(context.getBean(FileLifecycleReportPersistenceService.class));
      assertThat(context.getBean(SbomPersistenceService.class))
          .isSameAs(context.getBean(FileSbomPersistenceService.class));
    }
  }

  @Test
  public void shouldUseFilePersistenceServicesWhenStorageConfigIsNull() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setStorage(null);

    try (AnnotationConfigApplicationContext context = createContext(insightConfig)) {
      assertThat(context.getBean(ScanPersistenceService.class))
          .isSameAs(context.getBean(FileScanPersistenceService.class));
      assertThat(context.getBean(LifecycleReportPersistenceService.class))
          .isSameAs(context.getBean(FileLifecycleReportPersistenceService.class));
      assertThat(context.getBean(SbomPersistenceService.class))
          .isSameAs(context.getBean(FileSbomPersistenceService.class));
    }
  }

  @Test
  public void shouldUseS3PersistenceServicesWhenConfigured() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = insightConfig.getStorage();
    storageConfig.setType(DataStoreType.S3);
    storageConfig.setS3Config(createS3Config());

    try (AnnotationConfigApplicationContext context = createContext(insightConfig)) {
      assertThat(context.getBean(ScanPersistenceService.class))
          .isSameAs(context.getBean(S3ScanPersistenceService.class));
      assertThat(context.getBean(LifecycleReportPersistenceService.class))
          .isSameAs(context.getBean(S3LifecycleReportPersistenceService.class));
      assertThat(context.getBean(SbomPersistenceService.class))
          .isSameAs(context.getBean(S3SbomPersistenceService.class));
    }
  }

  @Test
  public void shouldUseHybridPersistenceServicesWhenConfigured() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    StorageConfig storageConfig = insightConfig.getStorage();
    storageConfig.setType(DataStoreType.HYBRID);
    storageConfig.setS3Config(createS3Config());

    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    hybridConfig.setTypes(new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.S3)));
    storageConfig.setHybridConfig(hybridConfig);

    try (AnnotationConfigApplicationContext context = createContext(insightConfig)) {
      assertThat(context.getBean(ScanPersistenceService.class))
          .isSameAs(context.getBean(HybridScanPersistenceService.class));
      assertThat(context.getBean(LifecycleReportPersistenceService.class))
          .isSameAs(context.getBean(HybridLifecycleReportPersistenceService.class));
      assertThat(context.getBean(SbomPersistenceService.class))
          .isSameAs(context.getBean(HybridSbomPersistenceService.class));
    }
  }

  private AnnotationConfigApplicationContext createContext(InsightConfig insightConfig) throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(loadPersistenceConfiguration(), TestProviderConfiguration.class);
    context.registerBean(InsightConfig.class, () -> insightConfig);

    context.registerBean(FileScanPersistenceService.class, () -> mock(FileScanPersistenceService.class));
    context.registerBean(S3ScanPersistenceService.class, () -> mock(S3ScanPersistenceService.class));
    context.registerBean(HybridScanPersistenceService.class, () -> mock(HybridScanPersistenceService.class));

    context.registerBean(FileLifecycleReportPersistenceService.class,
        () -> mock(FileLifecycleReportPersistenceService.class));
    context.registerBean(S3LifecycleReportPersistenceService.class,
        () -> mock(S3LifecycleReportPersistenceService.class));
    context.registerBean(HybridLifecycleReportPersistenceService.class,
        () -> mock(HybridLifecycleReportPersistenceService.class));

    context.registerBean(FileSbomPersistenceService.class, () -> mock(FileSbomPersistenceService.class));
    context.registerBean(S3SbomPersistenceService.class, () -> mock(S3SbomPersistenceService.class));
    context.registerBean(HybridSbomPersistenceService.class, () -> mock(HybridSbomPersistenceService.class));

    context.refresh();
    return context;
  }

  private static Class<?> loadPersistenceConfiguration() throws ClassNotFoundException {
    return Class.forName("com.sonatype.insight.brain.spring.config.PersistenceConfiguration");
  }

  private static S3DataStoreConfig createS3Config() {
    S3DataStoreConfig s3Config = new S3DataStoreConfig();
    s3Config.setBucketName("test-bucket");
    s3Config.setRegion("us-east-1");
    s3Config.setEndpoint(URI.create("http://localhost"));
    return s3Config;
  }

  @Configuration
  static class TestProviderConfiguration
  {
    @Bean
    ScanPersistenceServiceProvider scanPersistenceServiceProvider(
        InsightConfig insightConfig,
        Provider<S3ScanPersistenceService> s3Provider,
        Provider<FileScanPersistenceService> fileProvider,
        Provider<HybridScanPersistenceService> hybridProvider)
    {
      return new ScanPersistenceServiceProvider(insightConfig, s3Provider, fileProvider, hybridProvider);
    }

    @Bean
    LifecycleReportPersistenceServiceProvider applicationReportPersistenceServiceProvider(
        InsightConfig insightConfig,
        Provider<S3LifecycleReportPersistenceService> s3Provider,
        Provider<FileLifecycleReportPersistenceService> fileProvider,
        Provider<HybridLifecycleReportPersistenceService> hybridProvider)
    {
      return new LifecycleReportPersistenceServiceProvider(insightConfig, s3Provider, fileProvider, hybridProvider);
    }

    @Bean
    SbomPersistenceServiceProvider sbomPersistenceServiceProvider(
        InsightConfig insightConfig,
        Provider<S3SbomPersistenceService> s3Provider,
        Provider<FileSbomPersistenceService> fileProvider,
        Provider<HybridSbomPersistenceService> hybridProvider)
    {
      return new SbomPersistenceServiceProvider(insightConfig, s3Provider, fileProvider, hybridProvider);
    }
  }
}
