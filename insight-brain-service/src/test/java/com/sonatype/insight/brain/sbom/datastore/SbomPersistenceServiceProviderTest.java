/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import org.junit.Test;

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomPersistenceServiceProviderTest
    extends AbstractComponentTest
{
  @Inject
  protected InsightConfig insightConfig;

  @Test
  public void testGetProvider_nullStorageConfig() {
    insightConfig.setStorage(null);

    SbomPersistenceService result = lookup(SbomPersistenceService.class);

    assertThat(result).isInstanceOf(FileSbomPersistenceService.class);
  }

  @Test
  public void testGetProvider_fileDataStoreType() {
    var storageConfig = insightConfig.getStorage();
    storageConfig.setType(DataStoreType.FILE);

    SbomPersistenceService result = lookup(SbomPersistenceService.class);

    assertThat(result).isInstanceOf(FileSbomPersistenceService.class);
  }

  @Test
  public void testGetProvider_s3DataStoreType() {
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName("test-bucket");
    s3Config.setRegion("us-east-1");
    storageConfig.setS3Config(s3Config);
    storageConfig.setType(DataStoreType.S3);

    SbomPersistenceService result = lookup(SbomPersistenceService.class);

    assertThat(result).isInstanceOf(S3SbomPersistenceService.class);
  }

  @Test
  public void testGetProvider_hybridDataStoreType() {
    var storageConfig = insightConfig.getStorage();

    // Create hybrid config with FILE and S3 types
    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    LinkedHashSet<DataStoreType> types = new LinkedHashSet<>();
    types.add(DataStoreType.FILE);
    types.add(DataStoreType.S3);
    hybridConfig.setTypes(types);

    // Set S3 config for the S3 part of the hybrid setup
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName("test-bucket");
    s3Config.setRegion("us-east-1");
    storageConfig.setS3Config(s3Config);

    // Set hybrid config
    storageConfig.setHybridConfig(hybridConfig);
    storageConfig.setType(DataStoreType.HYBRID);

    SbomPersistenceService result = lookup(SbomPersistenceService.class);

    assertThat(result).isInstanceOf(HybridSbomPersistenceService.class);
  }

  @Test
  public void testGetDataStoreType_file() {
    // Get the provider directly
    SbomPersistenceServiceProvider provider = lookup(SbomPersistenceServiceProvider.class);

    // Call get with specific DataStoreType
    SbomPersistenceService result = provider.get(DataStoreType.FILE);

    // Verify it returns the correct implementation
    assertThat(result).isInstanceOf(FileSbomPersistenceService.class);
  }

  @Test
  public void testGetDataStoreType_s3() {
    // Setup S3 config to ensure it works
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName("test-bucket");
    s3Config.setRegion("us-east-1");
    storageConfig.setS3Config(s3Config);

    // Get the provider directly
    SbomPersistenceServiceProvider provider = lookup(SbomPersistenceServiceProvider.class);

    // Call get with specific DataStoreType
    SbomPersistenceService result = provider.get(DataStoreType.S3);

    // Verify it returns the correct implementation
    assertThat(result).isInstanceOf(S3SbomPersistenceService.class);
  }

  @Test
  public void testGetDataStoreType_hybrid() {
    // Setup hybrid config to ensure it works
    var storageConfig = insightConfig.getStorage();
    HybridDataStoreConfig hybridConfig = new HybridDataStoreConfig();
    LinkedHashSet<DataStoreType> types = new LinkedHashSet<>();
    types.add(DataStoreType.FILE);
    types.add(DataStoreType.S3);
    hybridConfig.setTypes(types);
    storageConfig.setHybridConfig(hybridConfig);

    // Set S3 config for the S3 part
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName("test-bucket");
    s3Config.setRegion("us-east-1");
    storageConfig.setS3Config(s3Config);

    // Get the provider directly
    SbomPersistenceServiceProvider provider = lookup(SbomPersistenceServiceProvider.class);

    // Call get with specific DataStoreType
    SbomPersistenceService result = provider.get(DataStoreType.HYBRID);

    // Verify it returns the correct implementation
    assertThat(result).isInstanceOf(HybridSbomPersistenceService.class);
  }
}
