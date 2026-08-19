/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.io.InputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service in charge of managing the MTIQ licenses
 */
@Named
public class TenantLicenseService
{
  private static final Logger log = LoggerFactory.getLogger(TenantLicenseService.class);

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final ProductLicenseService licenseService;

  @Inject
  public TenantLicenseService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      ProductLicenseService licenseService)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.licenseService = licenseService;
  }

  /**
   * Installs/Update a license for a tenant
   *
   * @param inputStream the input stream of the license file
   * @param fileName the file name
   * @param tenantSlug the tenant name
   */
  public void updateLicense(final InputStream inputStream, final String fileName, final String tenantSlug) {
    /*
     * Proper validations for the tenant name were executed as part of the AdminTenantFilter.
     * Here we are just checking we are not using the global tenant
     */
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug.replaceAll("[\n\r]", "_"));
      throw new NotFoundException("Tenant doesn't exist");
    }

    // Install license
    licenseService.installLicenseNoAuthz(inputStream, fileName);
  }
}
