/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import jakarta.inject.Provider;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScanPersistenceServiceProviderTest
{
  @Mock
  private InsightConfig insightConfig;

  @Mock
  private StorageConfig storageConfig;

  @Mock
  private Provider<S3ScanPersistenceService> s3ScanPersistenceServiceProvider;

  @Mock
  private Provider<FileScanPersistenceService> fileScanPersistenceServiceProvider;

  @Mock
  private Provider<HybridScanPersistenceService> hybridScanPersistenceServiceProvider;

  @Mock
  private S3ScanPersistenceService s3ScanPersistenceService;

  @Mock
  private FileScanPersistenceService fileScanPersistenceService;

  @Mock
  private HybridScanPersistenceService hybridScanPersistenceService;

  private ScanPersistenceServiceProvider provider;

  @Before
  public void setup() {
    provider = new ScanPersistenceServiceProvider(
        insightConfig,
        s3ScanPersistenceServiceProvider,
        fileScanPersistenceServiceProvider,
        hybridScanPersistenceServiceProvider
    );

    when(s3ScanPersistenceServiceProvider.get()).thenReturn(s3ScanPersistenceService);
    when(fileScanPersistenceServiceProvider.get()).thenReturn(fileScanPersistenceService);
    when(hybridScanPersistenceServiceProvider.get()).thenReturn(hybridScanPersistenceService);
  }

  @Test
  public void testGetProvider_nullStorageConfig() {
    // When storage config is null, should default to file-based persistence
    when(insightConfig.getStorage()).thenReturn(null);

    ScanPersistenceService result = provider.get();

    assertThat(result).isEqualTo(fileScanPersistenceService);
  }

  @Test
  public void testGetProvider_fileDataStoreType() {
    when(insightConfig.getStorage()).thenReturn(storageConfig);
    when(storageConfig.getType()).thenReturn(DataStoreType.FILE);

    ScanPersistenceService result = provider.get();

    assertThat(result).isEqualTo(fileScanPersistenceService);
  }

  @Test
  public void testGetProvider_s3DataStoreType() {
    when(insightConfig.getStorage()).thenReturn(storageConfig);
    when(storageConfig.getType()).thenReturn(DataStoreType.S3);

    ScanPersistenceService result = provider.get();

    assertThat(result).isEqualTo(s3ScanPersistenceService);
  }

  @Test
  public void testGetProvider_hybridDataStoreType() {
    when(insightConfig.getStorage()).thenReturn(storageConfig);
    when(storageConfig.getType()).thenReturn(DataStoreType.HYBRID);

    ScanPersistenceService result = provider.get();

    assertThat(result).isEqualTo(hybridScanPersistenceService);
  }

  @Test
  public void testGetProvider_consistentResults() {
    when(insightConfig.getStorage()).thenReturn(storageConfig);
    when(storageConfig.getType()).thenReturn(DataStoreType.S3);

    ScanPersistenceService result1 = provider.get();
    ScanPersistenceService result2 = provider.get();

    assertThat(result1).isEqualTo(s3ScanPersistenceService);
    assertThat(result2).isEqualTo(s3ScanPersistenceService);
    assertThat(result1).isEqualTo(result2);
  }

  @Test
  public void testGetProvider_configurationChanges() {
    // Test that provider responds to configuration changes
    when(insightConfig.getStorage()).thenReturn(storageConfig);

    // First call with FILE type
    when(storageConfig.getType()).thenReturn(DataStoreType.FILE);
    ScanPersistenceService fileResult = provider.get();
    assertThat(fileResult).isEqualTo(fileScanPersistenceService);

    // Second call with S3 type (simulating config change)
    when(storageConfig.getType()).thenReturn(DataStoreType.S3);
    ScanPersistenceService s3Result = provider.get();
    assertThat(s3Result).isEqualTo(s3ScanPersistenceService);

    // Third call with S3 type (simulating config change)
    when(storageConfig.getType()).thenReturn(DataStoreType.HYBRID);
    ScanPersistenceService hybridResult = provider.get();
    assertThat(hybridResult).isEqualTo(hybridScanPersistenceService);

    // Results should be different
    assertThat(fileResult).isNotEqualTo(s3Result);
  }

  @Test
  public void testProviderImplementation_NamedSingleton() {
    assertThat(provider.getClass().isAnnotationPresent(jakarta.inject.Singleton.class)).isTrue();
    assertThat(provider.getClass().isAnnotationPresent(jakarta.inject.Named.class)).isTrue();
  }

  @Test
  public void testProviderImplementation_interfaceCompliance() {
    assertThat(provider).isInstanceOf(Provider.class);
  }
}
