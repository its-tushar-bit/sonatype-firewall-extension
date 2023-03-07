/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sonatype.insight.brain.support.SupportInfoUtil;
import com.sonatype.insight.brain.support.SupportInformation;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
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
    extends MultiTenantTestSupport
{
  protected Tenant tenant;

  @Mock
  private TenantUtil tenantUtil;

  @Mock
  private TenantValidator tenantValidator;

  private TenantSupportInfoService tenantSupportInfoService;

  @Mock
  private SupportInformation supportInformation;

  @Mock
  private SupportInfoUtil supportInfoUtil;

  @Before
  @Override
  public void setup() {
    tenantSupportInfoService = new TenantSupportInfoService(tenantUtil, tenantValidator,
        supportInformation, supportInfoUtil);
  }

  @Test
  public void shouldGetSupportZip() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant)).thenReturn(true);

      try {
        when(supportInformation.aNewListOfSupportFiles()).thenReturn(supportInformation);
        when(supportInformation.withJavaVersion()).thenReturn(supportInformation);
        when(supportInformation.withProductVersion()).thenReturn(supportInformation);
        when(supportInformation.withLicenseDetails()).thenReturn(supportInformation);
        when(supportInformation.withUsersDetails()).thenReturn(supportInformation);
        when(supportInformation.withRolesDetails()).thenReturn(supportInformation);
        when(supportInformation.withMembershipMappings()).thenReturn(supportInformation);
        when(supportInformation.withPolicies()).thenReturn(supportInformation);
        when(supportInformation.withComponentsInQuarantine()).thenReturn(supportInformation);
        when(supportInformation.withWaivers()).thenReturn(supportInformation);
        when(supportInformation.build()).thenReturn(new ArrayList<>());
        when(supportInfoUtil.generateZip(any(), any())).thenReturn(new File("mtiq-support.zip"));
        File supportZip = tenantSupportInfoService.getSupportZip();

        assertThat(supportZip).hasName("mtiq-support.zip");
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void shouldThrowBadRequestException_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);
      assertThatThrownBy(() -> tenantSupportInfoService.getSupportZip())
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldThrowNotFoundException_whenTenantDoesNotExist() {
    final String errorMessage = "Tenant doesn't exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant)).thenReturn(false);
      assertThatThrownBy(() -> tenantSupportInfoService.getSupportZip())
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }
}
