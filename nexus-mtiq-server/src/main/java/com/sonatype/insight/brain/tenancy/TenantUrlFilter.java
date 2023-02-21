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

import com.google.common.net.InetAddresses;
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
      if (InetAddresses.isInetAddress(serverName)) {
        // the application health check comes in as an IP Address
        tenantUtil.setGlobalTenant();
      }
      else {
        String tenantName = tenantUtil.getTenantName(serverName);

        tenantManager.setTenant(tenantName);
      }

      log.debug("Tenant context set to: {}", tenantManager.getTenant().tenantSlug);

      chain.doFilter(request, response);
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

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void destroy() {
    // noop
  }
}
