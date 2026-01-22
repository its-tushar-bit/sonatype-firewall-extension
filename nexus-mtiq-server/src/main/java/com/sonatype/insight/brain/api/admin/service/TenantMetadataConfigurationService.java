/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class TenantMetadataConfigurationService
{
  private final MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore;

  private final TenantMetadataDAO tenantMetadataDAO;

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  @Inject
  public TenantMetadataConfigurationService(
      @Nullable MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore,
      TenantMetadataDAO tenantMetadataDAO,
      TenantUtil tenantUtil,
      TenantValidator tenantValidator
  )
  {
    this.multiTenantEncryptionKeyStore = multiTenantEncryptionKeyStore;
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
  }

  public void insertOrUpdateMetadata(
      final TenantMetadataDTO tenantMetadataDTO,
      final String tenantSlug)
  {
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      throw new NotFoundException("Tenant doesn't exist");
    }

    TenantMetadata tenantMetadata = this.tenantMetadataDAO.get();
    TenantMetadata updateAuth0 = TenantMetadataDTO.fromDTO(tenantMetadataDTO);
    if (tenantMetadata == null) {
      this.tenantMetadataDAO.insert(updateAuth0);
    }
    else {
      updateAuth0.setId(tenantMetadata.getId());
      this.tenantMetadataDAO.update(updateAuth0);
    }

    if (multiTenantEncryptionKeyStore != null) {
      multiTenantEncryptionKeyStore.initializeKey();
    }
  }
}
