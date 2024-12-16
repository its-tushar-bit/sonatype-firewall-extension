/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import com.sonatype.insight.brain.tenancy.TenantManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantRegistrationHealthCheckTest
{
  @Mock
  private TenantManager tenantManager;

  @Test
  public void testHealthCheck() throws Exception {
    TenantRegistrationHealthCheck healthCheck = new TenantRegistrationHealthCheck(tenantManager);
    assertThat(healthCheck.getName()).isEqualTo("tenant-registration");

    when(tenantManager.areTenantsPreRegistered()).thenReturn(false);
    assertThat(healthCheck.check().isHealthy()).isFalse();

    when(tenantManager.areTenantsPreRegistered()).thenReturn(true);
    assertThat(healthCheck.check().isHealthy()).isTrue();
  }
}
