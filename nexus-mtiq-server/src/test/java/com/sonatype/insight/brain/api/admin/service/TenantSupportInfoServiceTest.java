/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import com.sonatype.insight.brain.support.SupportInfo;
import com.sonatype.insight.brain.support.SupportInfoFiles;
import com.sonatype.insight.brain.support.SupportInfoUtil;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantSupportInfoServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  private TenantUtil tenantUtil;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private SupportInfoFiles supportInfoFiles;

  @Mock
  private SupportInfoUtil supportInfoUtil;

  private TenantSupportInfoService underTest;

  @Before
  public void setup() {
    underTest = new TenantSupportInfoService(tenantUtil, tenantValidator,
        supportInfoFiles, supportInfoUtil);
  }

  @Test
  public void testGetSupportInfo() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(supportInfoFiles.aNewListOfSupportFiles()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withConfigPropertiesInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withJavaVersion()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withProductVersion()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withLicenseDetails()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withTenantInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withUsersDetails()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withSamlUsersDetails()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withOauth2UsersDetails()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withRolesDetails()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withRolePermissionDetails()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withMembershipMappings()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withPolicies()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withComponentsInQuarantine()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withWaivers()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withRepositoryManager()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withRepositories()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withSecurityVulnerabilityOverrides()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withSystemConfigurationInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withSystemNoticeInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withWebhookInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withOrganizationInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withApplicationInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withApplicationTagInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withTagInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withPolicyTagInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withComponentLabelInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withLabelInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withDataRetentionPolicyInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withLicenseInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withMultiLicenseInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withLicenseThreatGroupInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withLicenseThreatGroupLicenseInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withProprietaryConfigInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withScmInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withSourceControlInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withPolicyMonitoringInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withMigrationTrackerInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withInnerSourceRepositoryInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withSystemConfigPropertiesInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withFeatureConfigPropertiesInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.withTenantMetadataInfo()).thenReturn(supportInfoFiles);
      when(supportInfoFiles.build()).thenReturn(new ArrayList<>());
      when(supportInfoUtil.generateSupportInfo(any(), any())).thenReturn(
          new SupportInfo(new ByteArrayOutputStream(), "tenant-support-mtiq"));
      SupportInfo supportInfo = underTest.getSupportInfo(tenant.tenantSlug);

      assertThat(supportInfo.getSupportInfoName()).isEqualTo("tenant-support-mtiq");
    });
  }

  @Test
  public void testGetSupportInfo_globalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);
      assertThatThrownBy(() -> underTest.getSupportInfo(global.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void testGetSupportInfo_tenantDoesNotExist() {
    final String errorMessage = "Tenant doesn't exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);
      assertThatThrownBy(() -> underTest.getSupportInfo(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }
}
