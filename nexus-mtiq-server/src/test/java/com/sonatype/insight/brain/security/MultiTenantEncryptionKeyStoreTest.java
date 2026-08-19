/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.clients.AwsSecretsManagerClient;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantEncryptionKeyStoreTest
    extends AbstractMultiTenantTest
{
  @Mock
  AwsSecretsManagerClient awsSecretsManagerClient;

  @Mock
  TenantMetadataDAO tenantMetadataDAO;

  private MultiTenantInsightConfig multiTenantInsightConfig = new MultiTenantInsightConfig();

  private TenantUtil tenantUtil = new TenantUtil();

  private MultiTenantEncryptionKeyStore underTest;

  @BeforeEach
  public void setup() {
    underTest = new MultiTenantEncryptionKeyStore(awsSecretsManagerClient, multiTenantInsightConfig, tenantMetadataDAO,
        tenantUtil);
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_registerAndGetKey() {
    String expectedKey = "tenantTestKey";
    TenantMetadata tenantMetadata = new TenantMetadata();
    tenantMetadata.setEncryptionKeyName("tenantTestKeyName");

    when(tenantMetadataDAO.get()).thenReturn(tenantMetadata);
    when(awsSecretsManagerClient.getSecret(tenantMetadata.getEncryptionKeyName())).thenReturn(expectedKey);

    testAsNewTenant(t1 -> {
      underTest.register();

      assertThat(underTest.getKey()).isEqualTo(expectedKey);
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_registerAndGetKey_globalTenant() {
    String expectedKey = "tenantTestKey";
    String tenantTestKeyName = "tenantTestKeyName";

    multiTenantInsightConfig.setGlobalTenantEncryptionKeyName(tenantTestKeyName);
    when(awsSecretsManagerClient.getSecret(tenantTestKeyName)).thenReturn(expectedKey);

    testAsGlobalTenant(global -> {
      underTest.register();

      assertThat(underTest.getKey()).isEqualTo(expectedKey);
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_noRegisterAndGetKey_globalTenant() {
    String expectedKey = "tenantTestKey";
    String tenantTestKeyName = "tenantTestKeyName";

    multiTenantInsightConfig.setGlobalTenantEncryptionKeyName(tenantTestKeyName);
    when(awsSecretsManagerClient.getSecret(tenantTestKeyName)).thenReturn(expectedKey);

    testAsGlobalTenant(global -> {
      assertThat(underTest.getKey()).isEqualTo(expectedKey);
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_getKey_tenantNotRegistered() {
    testAsNewTenant(t1 -> {
      Exception exception = assertThrows(RuntimeException.class, () -> underTest.getKey());

      String expectedMessage = String.format("Tenant %s encryption key not found.", t1.tenantSlug);
      String actualMessage = exception.getMessage();
      assertTrue(actualMessage.contains(expectedMessage));
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_registerAndGetKey_noMetadata() {
    when(tenantMetadataDAO.get()).thenReturn(null);

    testAsNewTenant(t1 -> {
      underTest.register();

      Exception exception = assertThrows(RuntimeException.class, () -> underTest.getKey());
      String expectedMessage = String.format("Tenant %s encryption key not found.", t1.tenantSlug);
      String actualMessage = exception.getMessage();
      assertTrue(actualMessage.contains(expectedMessage));
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_registerAndGetKey_noKey() {
    TenantMetadata tenantMetadata = new TenantMetadata();
    tenantMetadata.setEncryptionKeyName("tenantTestKeyName");

    when(tenantMetadataDAO.get()).thenReturn(tenantMetadata);
    when(awsSecretsManagerClient.getSecret(tenantMetadata.getEncryptionKeyName())).thenReturn(null);

    testAsNewTenant(t1 -> {
      underTest.register();

      Exception exception = assertThrows(RuntimeException.class, () -> underTest.getKey());
      String expectedMessage = String.format("Tenant %s encryption key not found.", t1.tenantSlug);
      String actualMessage = exception.getMessage();
      assertTrue(actualMessage.contains(expectedMessage));
    });
  }

  @Test
  public void testMultiTenantEncryptionKeyStore_registerAndGetKey_awsClientError() {
    TenantMetadata tenantMetadata = new TenantMetadata();
    tenantMetadata.setEncryptionKeyName("tenantTestKeyName");

    when(tenantMetadataDAO.get()).thenReturn(tenantMetadata);
    when(awsSecretsManagerClient.getSecret(tenantMetadata.getEncryptionKeyName()))
        .thenThrow(new RuntimeException(String.format("Unable to get secret %s from AWS secrets manager.",
            tenantMetadata.getEncryptionKeyName())));

    testAsNewTenant(t1 -> {
      underTest.register();

      Exception exception = assertThrows(RuntimeException.class, () -> underTest.getKey());
      String actualMessage = exception.getMessage();
      assertTrue(actualMessage.contains(String.format("Tenant %s encryption key not found.", t1.tenantSlug)));
    });
  }
}
