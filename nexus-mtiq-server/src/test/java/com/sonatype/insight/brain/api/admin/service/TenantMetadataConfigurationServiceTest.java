/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
}
