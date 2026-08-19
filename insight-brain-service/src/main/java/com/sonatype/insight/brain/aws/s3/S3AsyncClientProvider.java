/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aws.s3;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

@Named
@Singleton
public class S3AsyncClientProvider
    implements Provider<S3AsyncClient>
{
  public static final long MINIMUM_PART_SIZE_IN_BYTES = 5 * MB;

  public static final long THRESHOLD_IN_BYTES = 100 * MB;

  private final InsightConfig config;

  private final AwsCredentialsProvider credentialsProvider;

  @Inject
  public S3AsyncClientProvider(final InsightConfig config, final AwsCredentialsProvider credentialsProvider) {
    this.config = config;
    this.credentialsProvider = credentialsProvider;
  }

  @Override
  public S3AsyncClient get() {
    S3DataStoreConfig s3Config = config.getStorage().getS3Config();
    if (s3Config == null) {
      return null;
    }
    S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient.builder()
        .multipartEnabled(true)
        .multipartConfiguration(multipartConfiguration -> multipartConfiguration
            .minimumPartSizeInBytes(MINIMUM_PART_SIZE_IN_BYTES)
            .thresholdInBytes(THRESHOLD_IN_BYTES))
        .region(Region.of(s3Config.getRegion()))
        .credentialsProvider(credentialsProvider);
    if (s3Config.getEndpoint() != null) {
      s3AsyncClientBuilder.endpointOverride(s3Config.getEndpoint());
    }
    return s3AsyncClientBuilder.build();
  }
}
