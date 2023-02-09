/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.PrintWriter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
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
  private static final String TENANT_NAME = "tenant";

  @Mock
  private TenantManager tenantManager;

  @Mock
  private ServletRequest request;

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
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn(TENANT_NAME);
  }

  @Test
  public void shouldSetTenant_whenTenantParameterIsSent() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenantForAdminRequest(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetGlobalTenant_whenTenantParameterIsGlobal() throws Exception {
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn("global");

    underTest.doFilter(request, response, chain);

    assertThat(TenantThreadLocal.getTenant()).isEqualTo(GLOBAL_TENANT);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldInvalidateTenant_whenRequestFinished() {
    testAs(createTenant(TENANT_NAME), t -> {
      underTestDoFilter();

      assertThat(TenantThreadLocal.getTenantWithoutValidation().isInvalid()).isTrue();
      assertRequestIsPassedDownChain();
    });
  }

  @Test
  public void shouldThrowException_whenTenantParameterIsNotSent() throws Exception {
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn(null);
    when(response.getWriter()).thenReturn(printWriter);

    underTest.doFilter(request, response, chain);

    assertErrorResponseIsCreated();
  }

  @Test
  public void shouldThrowException_whenTenantParameterIsBlank() throws Exception {
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn("");
    when(response.getWriter()).thenReturn(printWriter);

    underTest.doFilter(request, response, chain);

    assertErrorResponseIsCreated();
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
