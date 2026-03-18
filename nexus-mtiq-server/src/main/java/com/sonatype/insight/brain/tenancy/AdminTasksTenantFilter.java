/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin Tasks Tenant servlet filter captures the tenant aware tasks incoming requests from
 * /api/admin/tenants/<tenant_name>/tasks/<task> and rewrites to the fixed dropwizard tasks url /tasks/<task>.
 * Admin Tasks Tenant filter will block requests to /tasks/ and respond with a helpful error message.
 * Any other requests will be passed on by the servlet filter chain unmodified.
 * The {@link com.sonatype.insight.brain.tenancy.AdminTenantFilter} must be run first to set the tenant for the request.
 */
@Named
public class AdminTasksTenantFilter
    implements Filter
{
  public static final String TASKS_API_ERROR_MGS =
      "Tasks API must be accessed on tenant URL /api/admin/tenants/<tenant_name>/tasks/<task>";

  private static final Logger log = LoggerFactory.getLogger(AdminTasksTenantFilter.class.getName());

  private static final String TASKS_URL = "/tasks/";

  private static final Pattern TENANT_TASKS_URL_REGEX = Pattern.compile("^/api/admin/tenants/[^/]+(/tasks/.+)");

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    String requestURI = req.getRequestURI();

    Matcher matcher = TENANT_TASKS_URL_REGEX.matcher(requestURI);
    if (matcher.find()) {
      String newURI = matcher.group(1);
      log.debug("Task url for Tenant {} set to: {}", TenantThreadLocal.getTenant().tenantSlug, newURI);
      req.getRequestDispatcher(newURI).forward(request, response);
    }
    else if (requestURI.startsWith(TASKS_URL)) {
      createErrorResponse(response);
    }
    else {
      chain.doFilter(request, response);
    }
  }

  private void createErrorResponse(final ServletResponse response) throws IOException {
    log.warn(TASKS_API_ERROR_MGS);

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    httpResponse.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    httpResponse.getWriter().print(TASKS_API_ERROR_MGS);
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
