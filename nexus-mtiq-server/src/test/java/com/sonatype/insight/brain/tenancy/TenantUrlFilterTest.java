/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.PrintWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.tenancy.Tenant.InvalidTenantSlugException;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantUrlFilterTest
    extends AbstractMultiTenantTest
{
  private static final String TENANT_NAME = "tenant";

  private static final String SERVER_NAME = TENANT_NAME + ".mtiq.cloudy.sonatype.dev";

  @Mock
  TenantManager tenantManager;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  FilterChain chain;

  @Mock
  PrintWriter printWriter;

  TenantUrlFilter underTest;

  @BeforeEach
  public void setup() {
    underTest = new TenantUrlFilter(tenantManager, new TenantUtil());
    lenient().when(request.getServerName()).thenReturn(SERVER_NAME);
    lenient().when(tenantManager.getTenant()).thenReturn(new Tenant(TENANT_NAME));
  }

  @Test
  public void shouldSetTenantFromUrlSlug() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenant(TENANT_NAME);
  }

  @Test
  public void shouldInvalidateTenantWhenRequestFinished() throws Exception {
    testAsTenant(new Tenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertThat(TenantThreadLocal.getTenantWithoutValidation().isInvalid()).isTrue();
    });
  }

  @Test
  public void shouldSetGlobalTenant_whenApplicationHealthCheck() throws Exception {
    when(request.getServerName()).thenReturn("192.168.0.1");

    testAsTenant(new Tenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertTenantSet(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldSetGlobalTenant_whenAdminRequest() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/admin/tenants");
    when(request.getPathInfo()).thenReturn("/admin/tenants");

    testAsTenant(new Tenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertTenantSet(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldPassRequestDownChain() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldCreateErrorResponse_whenErrorRegisteringTenant() throws Exception {
    doThrow(new IllegalArgumentException("Error registering tenant")).when(tenantManager).setTenant(TENANT_NAME);
    when(response.getWriter()).thenReturn(printWriter);

    underTest.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    verify(response).setContentType(ContentType.TEXT_PLAIN.getMimeType());
    verify(response).getWriter();
    verify(printWriter).print("Not Found");
  }

  @Test
  public void shouldCreateErrorResponse_whenInvalidTenantSlugExceptionRegisteringTenant() throws Exception {
    doThrow(new InvalidTenantSlugException("Slug name must be at least 3 characters")).when(tenantManager)
        .setTenant(TENANT_NAME);
    when(response.getWriter()).thenReturn(printWriter);

    underTest.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType(ContentType.TEXT_PLAIN.getMimeType());
    verify(response).getWriter();
    verify(printWriter).print("Invalid Tenant Slug");
    verify(chain, never()).doFilter(request, response);
  }

  @Test
  public void shouldNotPassRequestDownChain_whenErrorRegisteringTenant() throws Exception {
    doThrow(new IllegalArgumentException("Error registering tenant")).when(tenantManager).setTenant(TENANT_NAME);
    when(response.getWriter()).thenReturn(printWriter);

    underTest.doFilter(request, response, chain);

    verify(chain, never()).doFilter(request, response);
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
