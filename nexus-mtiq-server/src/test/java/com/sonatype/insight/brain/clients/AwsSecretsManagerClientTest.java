/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.clients;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AwsSecretsManagerClientTest
    extends AbstractMultiTenantTest
{
  @Mock
  public SecretsManagerClient secretsClient;

  private AwsSecretsManagerClient underTest;

  @BeforeEach
  public void setup() {
    underTest = new AwsSecretsManagerClient(secretsClient);
  }

  @Test
  public void testAwsSecretsManagerClient_getSecret() {
    String expectedKey = "tenantTestKey";

    when(secretsClient.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(GetSecretValueResponse.builder().secretString(expectedKey).build());

    testAsNewTenant(t1 -> {
      assertThat(underTest.getSecret("tenantTestKeyName")).isEqualTo(expectedKey);
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_registerAndGetKey_globalTenant() {
    String expectedKeyName = "tenantTestKeyName";

    when(secretsClient.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(SecretsManagerException.builder().build());

    testAsNewTenant(t1 -> {
      Exception exception = assertThrows(RuntimeException.class, () -> underTest.getSecret(expectedKeyName));
      String actualMessage = exception.getMessage();
      assertTrue(
          actualMessage.contains(String.format("Unable to get secret %s from AWS secrets manager.", expectedKeyName)));
    });
  }
}
