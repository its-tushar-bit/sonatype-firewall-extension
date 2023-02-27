/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

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
  public static final String TENANT_NAME = "test";

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
      when(tenantValidator.validateTenantExists(TENANT_NAME)).thenReturn(false);

      underTest.provisionTenant(TENANT_NAME);

      verify(databaseProvisionUtils).initializeDatabases(any(InsightConfig.class),
          any(MultiTenantDatabaseConfigProvider.class));
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantAlreadyExists() {
    final String errorMessage = "Tenant already exists";

    testAsNewTenant(t -> {
      when(tenantValidator.validateTenantExists(TENANT_NAME)).thenReturn(true);

      assertThatThrownBy(() -> underTest.provisionTenant(TENANT_NAME)).isInstanceOf(ConflictException.class)
          .withFailMessage(errorMessage);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(g -> {
      assertThatThrownBy(() -> underTest.provisionTenant(TENANT_NAME)).isInstanceOf(BadRequestException.class)
          .withFailMessage(errorMessage);
    });
  }
}
