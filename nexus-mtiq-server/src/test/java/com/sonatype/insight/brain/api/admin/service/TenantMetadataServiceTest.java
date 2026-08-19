/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantMetadataServiceTest
    extends AbstractMultiTenantTest
{
  public static final String APP_ID = "appId";

  public static final String APP_NAME = "appName";

  public static final String CONN_ID = "connId";

  public static final String CONN_NAME = "connName";

  public static final String ENC_KEY_NAME = "encKeyName";

  public static final String ORG_ID = "orgId";

  public static final String ORG_NAME = "orgName";

  @Mock
  private MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private TenantMetadataDAO tenantMetadataDAO;

  private TenantMetadataConfigurationService underTest;

  @BeforeEach
  public void setup() {
    underTest = new TenantMetadataConfigurationService(multiTenantEncryptionKeyStore, tenantMetadataDAO,
        new TenantUtil(), tenantValidator);
  }

  @Test
  public void shouldInsertAuth0Configuration() {
    testAsNewTenant(tenant -> {
      TenantMetadataDTO expected1 =
          new TenantMetadataDTO(APP_ID, APP_NAME, CONN_ID, CONN_NAME, ENC_KEY_NAME, ORG_ID, ORG_NAME);

      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(tenantMetadataDAO.get()).thenReturn(null);

      underTest.insertOrUpdateMetadata(expected1, tenant.tenantSlug);

      verify(tenantMetadataDAO).get();

      ArgumentCaptor<TenantMetadata> argument = ArgumentCaptor.forClass(TenantMetadata.class);
      verify(tenantMetadataDAO).insert(argument.capture());
      assertEquals(APP_ID, argument.getValue().getApplicationId());
      assertEquals(APP_NAME, argument.getValue().getApplicationName());
      assertEquals(CONN_ID, argument.getValue().getConnectionId());
      assertEquals(CONN_NAME, argument.getValue().getConnectionName());
      assertEquals(ENC_KEY_NAME, argument.getValue().getEncryptionKeyName());
      assertEquals(ORG_ID, argument.getValue().getOrganizationId());
      assertEquals(ORG_NAME, argument.getValue().getOrganizationName());

      verify(multiTenantEncryptionKeyStore).initializeKey();
    });
  }

  @Test
  public void shouldUpdateAuth0Configuration() {
    testAsNewTenant(tenant -> {
      TenantMetadataDTO expected1 =
          new TenantMetadataDTO(APP_ID, APP_NAME, CONN_ID, CONN_NAME, ENC_KEY_NAME, ORG_ID, ORG_NAME);

      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(tenantMetadataDAO.get()).thenReturn(
          TenantMetadataDTO.fromDTO(
              new TenantMetadataDTO(null, APP_NAME, CONN_ID, CONN_NAME, ENC_KEY_NAME, ORG_ID, ORG_NAME)));

      underTest.insertOrUpdateMetadata(expected1, tenant.tenantSlug);

      verify(tenantMetadataDAO).get();

      ArgumentCaptor<TenantMetadata> argument = ArgumentCaptor.forClass(TenantMetadata.class);
      verify(tenantMetadataDAO).update(argument.capture());
      assertEquals(APP_ID, argument.getValue().getApplicationId());
      assertEquals(APP_NAME, argument.getValue().getApplicationName());
      assertEquals(CONN_ID, argument.getValue().getConnectionId());
      assertEquals(CONN_NAME, argument.getValue().getConnectionName());
      assertEquals(ENC_KEY_NAME, argument.getValue().getEncryptionKeyName());
      assertEquals(ORG_ID, argument.getValue().getOrganizationId());
      assertEquals(ORG_NAME, argument.getValue().getOrganizationName());

      verify(multiTenantEncryptionKeyStore).initializeKey();
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      TenantMetadataDTO expected =
          new TenantMetadataDTO(APP_ID, APP_NAME, CONN_ID, CONN_NAME, ENC_KEY_NAME, ORG_ID, ORG_NAME);

      assertThatThrownBy(
          () -> underTest.insertOrUpdateMetadata(expected, tenant.tenantSlug))
              .withFailMessage("Tenant doesn't exist")
              .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    TenantMetadataDTO expected =
        new TenantMetadataDTO(APP_ID, APP_NAME, CONN_ID, CONN_NAME, ENC_KEY_NAME, ORG_ID, ORG_NAME);

    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(
          () -> underTest.insertOrUpdateMetadata(expected, tenant.tenantSlug))
              .withFailMessage("Invalid tenant")
              .isInstanceOf(BadRequestException.class);
    });
  }
}
