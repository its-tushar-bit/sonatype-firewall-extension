/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.support.SupportInformation;
import com.sonatype.insight.brain.support.SupportInfoUtil;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SupportInfoService
{
  private static final Logger log = LoggerFactory.getLogger(SupportInfoService.class);

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final SupportInformation supportInformation;

  private final SupportInfoUtil supportInfoUtil;

  @Inject
  public SupportInfoService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      SupportInformation supportInformation,
      SupportInfoUtil supportInfoUtil)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.supportInformation = supportInformation;
    this.supportInfoUtil = supportInfoUtil;
  }

  public File getSupportZip() throws IOException {
    final Tenant tenant = TenantThreadLocal.getTenant();

    if (tenantUtil.isGlobalTenant()) {
      log.error("Cannot generate Support Info, invalid Tenant: {}", tenant);
      throw new BadRequestException("Invalid Tenant");
    }

    if (!tenantValidator.validateTenantExists(tenant)) {
      log.error("Cannot generate Support Info, Tenant does not exist");
      throw new NotFoundException("Tenant does not exist");
    }

    final List<SupportFile> supportFiles = supportInformation
        .aNewListOfSupportFiles()
        .withJavaVersion()
        .withProductVersion()
        .withLicenseDetails()
        .withUsersDetails()
        .withRolesDetails()
        .withMembershipMappings()
        .withPolicies()
        .withComponentsInQuarantine()
        .withWaivers()
        .build();
    final String prefix = supportInfoUtil.generateUniqueName("mtiq-support-");

    return supportInfoUtil.generateZip(prefix, supportFiles);
  }
}
