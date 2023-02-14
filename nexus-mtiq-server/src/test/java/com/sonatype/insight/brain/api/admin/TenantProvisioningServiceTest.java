/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;
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

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantProvisioningServiceTest
    extends MultiTenantTest
{
  public static final String TENANT_NAME = "test";

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private DatabaseProvisionUtils databaseProvisionUtils;

  @Mock
  private DatabaseConfigProvider databaseConfigProvider;

  @Mock
  private TenantValidator tenantValidator;

  private TenantUtil tenantUtil;

  private TenantProvisioningService underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    tenantUtil = new TenantUtil();
    underTest = new TenantProvisioningService(insightConfig, databaseProvisionUtils, databaseConfigProvider, tenantUtil,
        tenantValidator);
  }

  @Test
  public void shouldProvisionNewTenant() {
    testAs(createTenant(TENANT_NAME), tenant -> {
      when(tenantValidator.validateTenantExists(TENANT_NAME)).thenReturn(false);

      underTest.provisionTenant(TENANT_NAME);

      verify(databaseProvisionUtils).initializeDatabases(insightConfig, databaseConfigProvider);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantAlreadyExists() {
    final String errorMessage = "Tenant already exists";

    testAs(createTenant(TENANT_NAME), tenant -> {
      when(tenantValidator.validateTenantExists(TENANT_NAME)).thenReturn(true);

      assertThatThrownBy(() -> underTest.provisionTenant(TENANT_NAME)).isInstanceOf(ConflictException.class)
          .withFailMessage(errorMessage);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAs(Tenant.GLOBAL_TENANT, tenant -> {
      assertThatThrownBy(() -> underTest.provisionTenant(TENANT_NAME)).isInstanceOf(BadRequestException.class)
          .withFailMessage(errorMessage);
    });
  }
}
