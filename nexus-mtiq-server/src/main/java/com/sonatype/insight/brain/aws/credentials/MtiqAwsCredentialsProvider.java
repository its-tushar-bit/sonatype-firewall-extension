/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.aws.credentials;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;

/**
 * MTIQ specific credentials provider to cut down on misleading errors in the logs from the default credential chain
 */
@Named
public class MtiqAwsCredentialsProvider
    implements Provider<AwsCredentialsProvider>
{
  @Override
  public AwsCredentialsProvider get() {
    return LazyAwsCredentialsProvider.create(
        () -> AwsCredentialsProviderChain.builder().reuseLastProviderEnabled(true)
            .credentialsProviders(new AwsCredentialsProvider[]{
                WebIdentityTokenFileCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build(),
                SystemPropertyCredentialsProvider.create(),
                EnvironmentVariableCredentialsProvider.create(),
                ProfileCredentialsProvider.create(),
                ContainerCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build(),
                InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build(),
                }).build());
  }
}
