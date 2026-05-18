/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import java.io.StringWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class McpLicenseFilterTest
{
  private final TenantUtil singleTenantUtil = createTenantUtil(false);

  private final TenantUtil multiTenantUtil = createTenantUtil(true);

  private static TenantUtil createTenantUtil(boolean multiTenant) {
    TenantUtil tenantUtil = mock(TenantUtil.class);
    when(tenantUtil.isMultiTenant()).thenReturn(multiTenant);
    return tenantUtil;
  }

  @Test
  public void testAccessAllowed_WhenLicensedAndSingleTenant() throws Exception {
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_MCP)).thenReturn(true);

    McpLicenseFilter filter = new McpLicenseFilter(productLicense, singleTenantUtil);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void testAccessDenied_WhenNotLicensed() throws Exception {
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_MCP)).thenReturn(false);

    McpLicenseFilter filter = new McpLicenseFilter(productLicense, singleTenantUtil);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);

    filter.doFilter(request, response, chain);

    verify(chain, never()).doFilter(request, response);
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    printWriter.flush();
    assertThat(stringWriter.toString()).isEqualTo(McpLicenseFilter.ACCESS_DENIED_MSG);
  }

  @Test
  public void testAccessDenied_WhenMultiTenantEvenIfLicensed() throws Exception {
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_MCP)).thenReturn(true);

    McpLicenseFilter filter = new McpLicenseFilter(productLicense, multiTenantUtil);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);

    filter.doFilter(request, response, chain);

    verify(chain, never()).doFilter(request, response);
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    printWriter.flush();
    assertThat(stringWriter.toString()).isEqualTo(McpLicenseFilter.ACCESS_DENIED_MSG);
  }
}
