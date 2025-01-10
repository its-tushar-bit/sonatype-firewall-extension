/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.clients;

import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.aws.credentials.InsightAwsCredentialsProvider;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

@Named
@Singleton
public class AwsSecretsManagerClient
{
  private final SecretsManagerClient secretsClient;

  @Inject
  public AwsSecretsManagerClient(InsightAwsCredentialsProvider credentialsProvider) {
    this(SecretsManagerClient.builder()
        .credentialsProvider(credentialsProvider.get())
        .httpClientBuilder(ApacheHttpClient.builder()
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(5)))
        .build());
  }

  public AwsSecretsManagerClient(SecretsManagerClient secretsClient) {
    this.secretsClient = secretsClient;
  }

  public String getSecret(String keyName) {
    GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(keyName).build();

    GetSecretValueResponse valueResponse;
    try {
      valueResponse = secretsClient.getSecretValue(valueRequest);
    }
    catch (SecretsManagerException e) {
      throw new RuntimeException(String.format("Unable to get secret %s from AWS secrets manager.",
          keyName), e);
    }

    return valueResponse.secretString();
  }
}
