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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Named
@Singleton
public class S3ClientProvider
    implements Provider<S3Client>
{
  private final InsightConfig config;

  private final AwsCredentialsProvider credentialsProvider;

  @Inject
  public S3ClientProvider(final InsightConfig config, final AwsCredentialsProvider credentialsProvider) {
    this.config = config;
    this.credentialsProvider = credentialsProvider;
  }

  @Override
  public S3Client get() {
    S3DataStoreConfig s3Config = config.getStorage().getS3Config();
    if (s3Config == null) {
      return null;
    }
    S3ClientBuilder s3ClientBuilder = S3Client.builder()
        .region(Region.of(s3Config.getRegion()))
        .credentialsProvider(credentialsProvider);
    if (s3Config.getEndpoint() != null) {
      s3ClientBuilder.endpointOverride(s3Config.getEndpoint());
    }
    return s3ClientBuilder.build();
  }
}
