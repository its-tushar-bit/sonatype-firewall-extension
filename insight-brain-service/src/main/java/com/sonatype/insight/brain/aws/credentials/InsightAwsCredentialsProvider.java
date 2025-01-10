/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.aws.credentials;

import javax.inject.Named;
import javax.inject.Provider;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;

@Named
public class InsightAwsCredentialsProvider
    implements Provider<AwsCredentialsProvider>
{
  @Override
  public AwsCredentialsProvider get() {
    return LazyAwsCredentialsProvider.create(() -> {
      try (var profileCredentialsProvider = ProfileCredentialsProvider.create();
           var webIdentityTokenFileCredentialsProvider = WebIdentityTokenFileCredentialsProvider.builder()
              .asyncCredentialUpdateEnabled(false)
              .build();
           var containerCredentialsProvider = ContainerCredentialsProvider.builder()
               .asyncCredentialUpdateEnabled(false)
               .build();
           var instanceProfileCredentialsProvider = InstanceProfileCredentialsProvider.builder()
               .asyncCredentialUpdateEnabled(false)
               .build()) {

        return AwsCredentialsProviderChain.builder().reuseLastProviderEnabled(true)
            .credentialsProviders(
                webIdentityTokenFileCredentialsProvider,
                SystemPropertyCredentialsProvider.create(),
                EnvironmentVariableCredentialsProvider.create(),
                profileCredentialsProvider,
                containerCredentialsProvider,
                instanceProfileCredentialsProvider
            ).build();
      }
    });
  }
}
