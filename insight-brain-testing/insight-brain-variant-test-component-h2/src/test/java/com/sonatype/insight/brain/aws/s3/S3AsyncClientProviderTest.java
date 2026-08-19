/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aws.s3;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class S3AsyncClientProviderTest
    extends AbstractComponentH2Test
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
