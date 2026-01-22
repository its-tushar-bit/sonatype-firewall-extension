/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.tenancy.Tenant.InvalidTenantSlugException;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that captures the application's base URL from the incoming request.
 */
@Named
public class TenantUrlFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(TenantUrlFilter.class.getName());

  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  @Inject
  public TenantUrlFilter(final TenantManager tenantManager, final TenantUtil tenantUtil) {
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    final String serverName = request.getServerName();

    try {
      /*
       * Tenants for admin requests are set using a path param in AdminTenantFilter. We set the tenant here to global
       * as a catch-all to ensure there is always a tenant set. This is important because Jetty reuses threads so a
       * tenant must always be set on every request. AdminTenantFilter can't do this because it only deals with requests
       * on the /api/admin path but there are other admin requests such as /healthcheck. Note that the application
       * healthcheck comes in as an IP Address
       */
      if (tenantUtil.requestShouldUseGlobalTenant(request)) {
        tenantUtil.setGlobalTenant();
      }
      else {
        String tenantName = tenantUtil.getTenantName(serverName);

        tenantManager.setTenant(tenantName);
      }

      log.trace("Tenant context set to: {}", tenantManager.getTenant().tenantSlug);

      chain.doFilter(request, response);
    }
    catch (InvalidTenantSlugException exception) {
      createTenantInvalidSlugResponse(response);
      log.debug("Error registering tenant: {} Error: {}", serverName, exception.getMessage());
    }
    catch (IllegalArgumentException exception) {
      createTenantNotFoundResponse(response);
      log.debug("Error registering tenant: {} Error: {}", serverName, exception.getMessage());
    }
    finally {
      TenantThreadLocal.invalidateTenant();
    }
  }

  private void createTenantNotFoundResponse(final ServletResponse response) throws IOException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
    httpResponse.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    httpResponse.getWriter().print("Not Found");
  }

  private void createTenantInvalidSlugResponse(final ServletResponse response) throws IOException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    httpResponse.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    httpResponse.getWriter().print("Invalid Tenant Slug");
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void destroy() {
    // noop
  }
}
