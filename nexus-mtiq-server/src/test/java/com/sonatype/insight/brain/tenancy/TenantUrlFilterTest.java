/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantUrlFilterTest
    extends MultiTenantTest
{
  private static final String TENANT_NAME = "tenant";

  private static final String SERVER_NAME = TENANT_NAME + ".mtiq.cloudy.sonatype.dev";

  @Mock
  TenantManager tenantManager;

  @Mock
  ServletRequest request;

  @Mock
  ServletResponse response;

  @Mock
  FilterChain chain;

  TenantUrlFilter underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    underTest = new TenantUrlFilter(tenantManager);
    when(request.getServerName()).thenReturn(SERVER_NAME);
    when(tenantManager.getTenant()).thenReturn(new Tenant(TENANT_NAME));
  }

  @Test
  public void shouldSetTenantFromUrlSlug() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenant(TENANT_NAME);
  }

  @Test
  public void shouldInvalidateTenantWhenRequestFinished() throws Exception {
    testAs(new Tenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertThat(TenantThreadLocal.getTenantWithoutValidation().isInvalid()).isTrue();
    });
  }

  @Test
  public void shouldSetGlobalTenant_whenApplicationHealthCheck() throws Exception {
    when(request.getServerName()).thenReturn("192.168.0.1");

    testAs(new Tenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertTenantSet(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldPassRequestDownChain() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  private void underTestDoFilter() {
    try {
      underTest.doFilter(request, response, chain);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
