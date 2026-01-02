/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aws.s3;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import org.junit.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class S3AsyncClientProviderTest
    extends AbstractComponentTest
{
  @Inject
  private S3AsyncClientProvider s3AsyncClientProvider;

  @Inject
  private InsightConfig insightConfig;

  @Test
  public void testGet_NoConfig() {
    assertThat(insightConfig.getStorage().getS3Config()).isNull();
    assertThat(s3AsyncClientProvider.get()).isNull();
  }

  @Test
  public void testGet_WithConfig() {
    S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
    s3DataStoreConfig.setBucketName("someBucket");
    s3DataStoreConfig.setRegion("someRegion");
    insightConfig.getStorage().setS3Config(s3DataStoreConfig);

    S3AsyncClient s3AsyncClient = s3AsyncClientProvider.get();
    assertThat(s3AsyncClient).isNotNull();
  }
}
