/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.tenancy.TenantManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
public class TenantRegistrationHealthCheckTest
{
  @Mock
  private TenantManager tenantManager;

  @Test
  public void testHealthCheck() throws Exception {
    TenantRegistrationHealthCheck healthCheck = new TenantRegistrationHealthCheck(tenantManager);
    assertThat(healthCheck.getName()).isEqualTo("tenant-registration");

    when(tenantManager.areTenantsPreRegistered()).thenReturn(false);
    Health health = healthCheck.check();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);

    when(tenantManager.areTenantsPreRegistered()).thenReturn(true);
    health = healthCheck.check();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }
}
