/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.aws.credentials;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Self-hosted should use the default credentials chain provider
 */
@Named
@Singleton
public class DefaultInsightAwsCredentialProvider
    implements Provider<AwsCredentialsProvider>
{
  @Override
  public AwsCredentialsProvider get() {
    return DefaultCredentialsProvider.builder().build();
  }
}
