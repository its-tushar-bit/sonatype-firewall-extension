/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.PrintWriter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminTenantFilterTest
    extends MultiTenantTest
{
  private static final String TENANT_NAME = "tenant1";

  @Mock
  private TenantManager tenantManager;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private PrintWriter printWriter;

  @Mock
  private FilterChain chain;

  private AdminTenantFilter underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    underTest = new AdminTenantFilter(tenantManager, new TenantUtil());

    when(request.getRequestURI()).thenReturn("/api/admin/other");
  }

  @Test
  public void shouldSetTenant_whenTenantPathParameterIsSent() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenant/%s/", TENANT_NAME));

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenantForAdminRequest(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetTenant_whenTenantPathParameterIsSentOnMiddle() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenant/%s/feature/api-update", TENANT_NAME));

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenantForAdminRequest(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetGlobalTenant_whenTenantPathParameterIsGlobal() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenant/%s", GLOBAL_TENANT.tenantSlug));

    underTest.doFilter(request, response, chain);

    assertThat(TenantThreadLocal.getTenant()).isEqualTo(GLOBAL_TENANT);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldThrowException_whenTenantPathParameterIsEmpty() throws Exception {
    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenant/%s", ""));

    underTest.doFilter(request, response, chain);

    assertErrorResponseIsCreated();
  }

  @Test
  public void shouldSetTenant_whenTenantQueryParameterIsSent() throws Exception {
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn(TENANT_NAME);

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenantForAdminRequest(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetGlobalTenant_whenTenantQueryParameterIsGlobal() throws Exception {
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn(GLOBAL_TENANT.tenantSlug);

    underTest.doFilter(request, response, chain);

    assertThat(TenantThreadLocal.getTenant()).isEqualTo(GLOBAL_TENANT);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldInvalidateTenant_whenRequestFinished() {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenant/%s", TENANT_NAME));

    testAs(createTenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertThat(TenantThreadLocal.getTenantWithoutValidation().isInvalid()).isTrue();
      assertRequestIsPassedDownChain();
    });
  }

  public void assertErrorResponseIsCreated() throws Exception {
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType(ContentType.TEXT_PLAIN.getMimeType());
    verify(response).getWriter();
    verify(printWriter).print("Invalid tenant");
  }

  private void underTestDoFilter() {
    try {
      underTest.doFilter(request, response, chain);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void assertRequestIsPassedDownChain() {
    try {
      verify(chain).doFilter(request, response);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
