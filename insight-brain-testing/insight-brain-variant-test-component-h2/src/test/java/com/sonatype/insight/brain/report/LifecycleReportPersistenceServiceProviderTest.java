/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.variant.ComponentH2DirtiesContextTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// This test mutates the shared insightConfig.storage to exercise each persistence-service variant,
// which changes the type of the lazily-created @Primary lifecycleReportPersistenceService singleton
// (PersistenceConfiguration builds it from provider.get()). @ComponentH2DirtiesContextTest keeps the
// DirtiesContext listeners (which @ComponentH2Test disables for context reuse) so the shared context is
// evicted and rebuilt clean after this class, instead of leaking the wrong bean type into sibling classes
// when the module is sharded across forks (reuseForks=true, forkCount>1).
@ComponentH2DirtiesContextTest
public class LifecycleReportPersistenceServiceProviderTest
    extends AbstractComponentH2Test
{
  @Inject
  private LifecycleReportPersistenceServiceProvider provider;

  @Inject
  private InsightConfig insightConfig;

  @Test
  public void testGet_NoStorageConfig() {
    insightConfig.setStorage(null);

    assertThat(provider.get()).isInstanceOf(FileLifecycleReportPersistenceService.class);
  }

  @Test
  public void testGet_File() {
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.FILE);
    insightConfig.setStorage(storageConfig);

    assertThat(provider.get()).isInstanceOf(FileLifecycleReportPersistenceService.class);
  }

  @Test
  public void testGet_S3() {
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.S3);
    S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
    s3DataStoreConfig.setBucketName("someBucketName");
    s3DataStoreConfig.setRegion("someRegion");
    s3DataStoreConfig.setEndpoint(URI.create("http://localhost"));
    storageConfig.setS3Config(s3DataStoreConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(provider.get()).isInstanceOf(S3LifecycleReportPersistenceService.class);
  }

  @Test
  public void testGet_Hybrid() {
    StorageConfig storageConfig = new StorageConfig();
    storageConfig.setType(DataStoreType.HYBRID);
    S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
    s3DataStoreConfig.setBucketName("someBucketName");
    s3DataStoreConfig.setRegion("someRegion");
    s3DataStoreConfig.setEndpoint(URI.create("http://localhost"));
    storageConfig.setS3Config(s3DataStoreConfig);
    HybridDataStoreConfig hybridDataStoreConfig = new HybridDataStoreConfig();
    hybridDataStoreConfig.setTypes(new LinkedHashSet<>(List.of(DataStoreType.S3, DataStoreType.FILE)));
    storageConfig.setHybridConfig(hybridDataStoreConfig);
    insightConfig.setStorage(storageConfig);

    assertThat(provider.get()).isInstanceOf(HybridLifecycleReportPersistenceService.class);
  }
}
