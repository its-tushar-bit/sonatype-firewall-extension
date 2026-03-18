/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import datadog.trace.api.Trace;
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

  static final Pattern TENANT_PARAMETER_REGEX = Pattern.compile("/admin/tenants/([^/]+)");

  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  @Inject
  public AdminTenantFilter(final TenantManager tenantManager, final TenantUtil tenantUtil) {
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
  }

  @Override
  @Trace(operationName = "admin.request")
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    final String tenantName = getTenantParameter(request);

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
      setTenant(tenantName);
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

  private void setTenant(String tenantName) {
    try {
      tenantManager.setTenant(tenantName);
    }
    catch (IllegalArgumentException exception) {
      tenantManager.setTenantForAdminRequest(tenantName);
    }
  }

  private static String getTenantParameter(final ServletRequest request) {
    String tenantName = getTenantParameterFromPath(request);
    if (StringUtils.isBlank(tenantName)) {
      tenantName = request.getParameter(TENANT_PARAMETER);
    }
    return tenantName;
  }

  static String getTenantParameterFromPath(final ServletRequest request) {
    HttpServletRequest req = (HttpServletRequest) request;

    Matcher matcher = TENANT_PARAMETER_REGEX.matcher(req.getRequestURI());

    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }

  private void createErrorResponse(final ServletResponse response) throws IOException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    httpResponse.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    httpResponse.getWriter().print("Invalid tenant");
  }
}
