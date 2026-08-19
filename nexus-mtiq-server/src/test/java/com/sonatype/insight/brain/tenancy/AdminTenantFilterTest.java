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

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminTenantFilterTest
    extends AbstractMultiTenantTest
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

  @BeforeEach
  public void setup() {
    underTest = new AdminTenantFilter(tenantManager, new TenantUtil());

    lenient().when(request.getRequestURI()).thenReturn("/api/admin/other");
  }

  @Test
  public void shouldSetTenant_whenTenantPathParameterIsSent() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/", TENANT_NAME));

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenant(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetTenantForAdminRequest_whenTenantRegistrationFails() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/", TENANT_NAME));
    doThrow(new IllegalArgumentException("Tenant registration fails")).when(tenantManager).setTenant(TENANT_NAME);

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenantForAdminRequest(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetTenant_whenTenantPathParameterIsSentOnMiddle() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/feature/api-update", TENANT_NAME));

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenant(TENANT_NAME);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldSetGlobalTenant_whenTenantPathParameterIsGlobal() throws Exception {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s", GLOBAL_TENANT.tenantSlug));

    underTest.doFilter(request, response, chain);

    assertThat(TenantThreadLocal.getTenant()).isEqualTo(GLOBAL_TENANT);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldThrowException_whenTenantPathParameterIsEmpty() throws Exception {
    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s", ""));

    underTest.doFilter(request, response, chain);

    assertErrorResponseIsCreated();
  }

  @Test
  public void shouldSetTenant_whenTenantQueryParameterIsSent() throws Exception {
    when(request.getParameter(AdminTenantFilter.TENANT_PARAMETER)).thenReturn(TENANT_NAME);

    underTest.doFilter(request, response, chain);

    verify(tenantManager).setTenant(TENANT_NAME);
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
    testAsNewTenant(t -> {
      when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s", TENANT_NAME));

      underTestDoFilter();

      assertThat(TenantThreadLocal.getTenantWithoutValidation().isInvalid()).isTrue();
      assertRequestIsPassedDownChain();
    });
  }

  @Test
  public void shouldGetProperTenantParameterFromURIPath_whenTenantParameterIsAtTheEnd() {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/", TENANT_NAME));

    String tenant = AdminTenantFilter.getTenantParameterFromPath(request);

    assertThat(tenant).isEqualTo(TENANT_NAME);
  }

  @Test
  public void shouldGetProperTenantParameterFromURIPath_whenUrlEndsOnSlash() {
    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/", TENANT_NAME));

    String tenant = AdminTenantFilter.getTenantParameterFromPath(request);

    assertThat(tenant).isEqualTo(TENANT_NAME);
  }

  @Test
  public void shouldGetProperTenantParameterFromURIPath_whenTenantParameterIsInTheMiddle() {
    when(request.getRequestURI()).thenReturn(
        String.format("/api/admin/tenants/%s/collection/entity/other", TENANT_NAME));

    String tenant = AdminTenantFilter.getTenantParameterFromPath(request);

    assertThat(tenant).isEqualTo(TENANT_NAME);
  }

  @Test
  public void shouldReturnNullForTenantParameter_whenURIIsNotOnTheExpectedContext() {
    when(request.getRequestURI()).thenReturn(String.format("/api/other/tenants/%s", TENANT_NAME));

    String tenant = AdminTenantFilter.getTenantParameterFromPath(request);

    assertThat(tenant).isNull();
  }

  @Test
  public void shouldReturnNullForTenantParameter_whenURIHasBackSlashes() {
    when(request.getRequestURI()).thenReturn(String.format("\\api\\admin\\tenants\\%s", TENANT_NAME));

    String tenant = AdminTenantFilter.getTenantParameterFromPath(request);

    assertThat(tenant).isNull();
  }

  @Test
  public void shouldReturnNullForTenantParameter_whenURIIsNotValid() {
    when(request.getRequestURI()).thenReturn(String.format("/api\\admin/tenants/%s", TENANT_NAME));

    String tenant = AdminTenantFilter.getTenantParameterFromPath(request);

    assertThat(tenant).isNull();
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
