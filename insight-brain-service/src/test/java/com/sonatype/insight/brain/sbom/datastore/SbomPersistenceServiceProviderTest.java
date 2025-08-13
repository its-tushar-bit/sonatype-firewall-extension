/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import org.junit.Test;

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
}
