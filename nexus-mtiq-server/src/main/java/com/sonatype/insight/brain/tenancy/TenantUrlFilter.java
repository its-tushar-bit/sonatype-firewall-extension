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

import com.google.common.net.InetAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantUtil.getTenantName;

/**
 * Servlet filter that captures the application's base URL from the incoming request.
 */
@Named
public class TenantUrlFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(TenantUrlFilter.class.getName());

  private final TenantManager tenantManager;

  @Inject
  public TenantUrlFilter(final TenantManager tenantManager) {
    this.tenantManager = tenantManager;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    final String serverName = request.getServerName();

    if (InetAddresses.isInetAddress(serverName)) {
      // the application health check comes in as an IP Address
      TenantManager.initGlobalTenant();
    }
    else {
      String tenantName = getTenantName(serverName);

      tenantManager.setTenant(tenantName);
    }

    log.debug("Tenant context set to: " + tenantManager.getTenant().tenantSlug);

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
}
