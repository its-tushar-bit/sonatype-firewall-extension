/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.io.IOException;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.support.SupportInfo;
import com.sonatype.insight.brain.support.SupportInfoFiles;
import com.sonatype.insight.brain.support.SupportInfoUtil;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.brain.support.SupportZipInProgressException;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class TenantSupportInfoService
{
  private static final Logger log = LoggerFactory.getLogger(TenantSupportInfoService.class);

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final SupportInfoFiles supportInfoFiles;

  private final SupportInfoUtil supportInfoUtil;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public TenantSupportInfoService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      SupportInfoFiles supportInfoFiles,
      SupportInfoUtil supportInfoUtil,
      ClusterLockManager clusterLockManager)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.supportInfoFiles = supportInfoFiles;
    this.supportInfoUtil = supportInfoUtil;
    this.clusterLockManager = clusterLockManager;
  }

  public SupportInfo getSupportInfo(final String tenantSlug) throws IOException {
    if (tenantUtil.isGlobalTenant()) {
      log.error("Cannot generate Support Info, invalid Tenant: {}", tenantSlug);
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.error("Cannot generate Support Info, Tenant does not exist");
      throw new NotFoundException("Tenant doesn't exist");
    }

    // Mirrors on-prem SupportService.createSupportZip(): a concurrent generation for the same
    // tenant (executed in the tenant's DB context) would double or triple the disk footprint of
    // a large bundle on the shared MTIQ ECS task. Reject early instead of racing.
    try (ClusterLock clusterLock = clusterLockManager.createForSupportZip()) {
      if (!clusterLock.tryLock()) {
        throw new SupportZipInProgressException("Support zip generation is already in progress " +
            "for tenant '" + tenantSlug + "'. Please wait for the current operation to complete.");
      }

      final List<SupportFile> supportFiles = this.supportInfoFiles
          .aNewListOfSupportFiles()
          .withConfigPropertiesInfo()
          .withJavaVersion()
          .withProductVersion()
          .withLicenseDetails()
          .withTenantInfo()
          .withUsersDetails()
          .withSamlUsersDetails()
          .withOauth2UsersDetails()
          .withRolesDetails()
          .withRolePermissionDetails()
          .withMembershipMappings()
          .withPolicies()
          .withComponentsInQuarantine()
          .withWaivers()
          .withRepositoryManager()
          .withRepositories()
          .withSecurityVulnerabilityOverrides()
          .withSystemConfigurationInfo()
          .withSystemNoticeInfo()
          .withWebhookInfo()
          .withOrganizationInfo()
          .withApplicationInfo()
          .withApplicationTagInfo()
          .withTagInfo()
          .withPolicyTagInfo()
          .withComponentLabelInfo()
          .withLabelInfo()
          .withDataRetentionPolicyInfo()
          .withLicenseInfo()
          .withMultiLicenseInfo()
          .withLicenseThreatGroupInfo()
          .withLicenseThreatGroupLicenseInfo()
          .withProprietaryConfigInfo()
          .withScmInfo()
          .withSourceControlInfo()
          .withPolicyMonitoringInfo()
          .withMigrationTrackerInfo()
          .withInnerSourceRepositoryInfo()
          .withSystemConfigPropertiesInfo()
          .withFeatureConfigPropertiesInfo()
          .withTenantMetadataInfo()
          .build();

      return supportInfoUtil.generateSupportInfo(tenantSlug, supportFiles);
    }
  }
}
