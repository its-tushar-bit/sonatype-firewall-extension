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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantMetadataConfigurationServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  private MultiTenantEncryptionKeyStore mockMultiTenantEncryptionKeyStore;

  @Mock
  private TenantMetadataDAO mockTenantMetadataDAO;

  @Mock
  private TenantUtil mockTenantUtil;

  @Mock
  private TenantValidator mockTenantValidator;

  @Before
  public void setup() {
    when(mockTenantValidator.validateTenantExists(anyString())).thenReturn(true);
  }

  @Test
  public void testInsertOrUpdateMetadata_NullMultiTenantEncryptionKeyStore_DoesNotInitializeTenantKey() {
    TenantMetadataConfigurationService underTest = new TenantMetadataConfigurationService(
        null, mockTenantMetadataDAO, mockTenantUtil, mockTenantValidator);

    underTest.insertOrUpdateMetadata(new TenantMetadataDTO(), "tenantSlug");
  }

  @Test
  public void testInsertOrUpdateMetadata_MultiTenantEncryptionKeyStore_DoesInitializeTenantKey() {
    TenantMetadataConfigurationService underTest = new TenantMetadataConfigurationService(
        mockMultiTenantEncryptionKeyStore, mockTenantMetadataDAO, mockTenantUtil, mockTenantValidator);

    underTest.insertOrUpdateMetadata(new TenantMetadataDTO(), "tenantSlug");

    verify(mockMultiTenantEncryptionKeyStore).initializeKey();
  }

  @Test
  public void getMetadata_returnsDtoFromDao() {
    TenantMetadataConfigurationService underTest = new TenantMetadataConfigurationService(
        null, mockTenantMetadataDAO, mockTenantUtil, mockTenantValidator);

    TenantMetadata stored = new TenantMetadata(
        "appId1", "appName1", "connId1", "connName1", "encKeyName1", "orgId", "orgName");
    when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
    when(mockTenantValidator.validateTenantExists("tenant1")).thenReturn(true);
    when(mockTenantMetadataDAO.get()).thenReturn(stored);

    TenantMetadataDTO result = underTest.getMetadata("tenant1");

    assertThat(result.getApplicationId()).isEqualTo("appId1");
    assertThat(result.getApplicationName()).isEqualTo("appName1");
    assertThat(result.getConnectionId()).isEqualTo("connId1");
    assertThat(result.getConnectionName()).isEqualTo("connName1");
    assertThat(result.getEncryptionKeyName()).isEqualTo("encKeyName1");
    assertThat(result.getOrganizationId()).isEqualTo("orgId");
    assertThat(result.getOrganizationName()).isEqualTo("orgName");
  }

  @Test
  public void getMetadata_throwsNotFound_whenMetadataMissing() {
    TenantMetadataConfigurationService underTest = new TenantMetadataConfigurationService(
        null, mockTenantMetadataDAO, mockTenantUtil, mockTenantValidator);

    when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
    when(mockTenantValidator.validateTenantExists("tenant1")).thenReturn(true);
    when(mockTenantMetadataDAO.get()).thenReturn(null);

    assertThatThrownBy(() -> underTest.getMetadata("tenant1"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Tenant metadata not set");
  }

  @Test
  public void getMetadata_throwsNotFound_whenTenantDoesNotExist() {
    TenantMetadataConfigurationService underTest = new TenantMetadataConfigurationService(
        null, mockTenantMetadataDAO, mockTenantUtil, mockTenantValidator);

    when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
    when(mockTenantValidator.validateTenantExists("tenant1")).thenReturn(false);

    assertThatThrownBy(() -> underTest.getMetadata("tenant1"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Tenant tenant1 doesn't exist");
  }

  @Test
  public void getMetadata_throwsBadRequest_whenGlobalTenant() {
    TenantMetadataConfigurationService underTest = new TenantMetadataConfigurationService(
        null, mockTenantMetadataDAO, mockTenantUtil, mockTenantValidator);

    when(mockTenantUtil.isGlobalTenant()).thenReturn(true);

    assertThatThrownBy(() -> underTest.getMetadata("global"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Operation not supported for global tenant");
  }
}
