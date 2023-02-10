/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin Servlet filter that captures the tenant parameter from the incoming request to set the tenant.
 */
@Named
public class AdminTenantFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(AdminTenantFilter.class.getName());

  static final String TENANT_PARAMETER = "tenant";

  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  @Inject
  public AdminTenantFilter(final TenantManager tenantManager, final TenantUtil tenantUtil) {
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    final String tenantName = request.getParameter(TENANT_PARAMETER);

    if (StringUtils.isBlank(tenantName)) {
      createErrorResponse(response);
      log.debug("Invalid tenant parameter");
      return;
    }

    if (tenantUtil.isGlobalTenant(tenantName)) {
      // The admin endpoint will be used for global configuration
      tenantUtil.setGlobalTenant();
    }
    else {
      // The admin endpoint will be used for tenant configuration/maintenance
      tenantManager.setTenantForAdminRequest(tenantName);
    }

    log.debug("Tenant context set to: {}", TenantThreadLocal.getTenant().tenantSlug);

    try {
      chain.doFilter(request, response);
    }
    finally {
      TenantThreadLocal.invalidateTenant();
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void destroy() {
    // noop
  }

  private void createErrorResponse(final ServletResponse response) throws IOException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    httpResponse.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    httpResponse.getWriter().print("Invalid tenant");
  }
}
