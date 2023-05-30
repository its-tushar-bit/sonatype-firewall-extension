/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class TenantMetadataConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(TenantMetadataConfigurationService.class);

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final TenantMetadataDAO tenantMetadataDAO;

  @Inject
  public TenantMetadataConfigurationService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      TenantMetadataDAO tenantMetadataDAO)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.tenantMetadataDAO = tenantMetadataDAO;
  }

  public void insertOrUpdateMetadata(
      final TenantMetadataDTO tenantMetadataDTO,
      final String tenantSlug)
  {
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug.replaceAll("[\n\r]", "_"));
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
  }
}
