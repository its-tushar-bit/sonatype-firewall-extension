/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantProvisioningServiceTest
    extends MultiTenantTestSupport
{
  @Mock
  private InsightConfig insightConfig;

  @Mock
  private DatabaseProvisionUtils databaseProvisionUtils;

  @Mock
  private TenantValidator tenantValidator;

  private TenantUtil tenantUtil;

  private TenantProvisioningService underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    tenantUtil = new TenantUtil();
    underTest = new TenantProvisioningService(insightConfig, databaseProvisionUtils, tenantUtil,
        tenantValidator);
  }

  @Test
  public void shouldProvisionNewTenant() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      underTest.provisionTenant(tenant.tenantSlug);

      verify(databaseProvisionUtils).initializeDatabases(any(InsightConfig.class),
          any(MultiTenantDatabaseConfigProvider.class));
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantAlreadyExists() {
    final String errorMessage = "Tenant already exists";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      assertThatThrownBy(() -> underTest.provisionTenant(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(ConflictException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(() -> underTest.provisionTenant(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }
}
