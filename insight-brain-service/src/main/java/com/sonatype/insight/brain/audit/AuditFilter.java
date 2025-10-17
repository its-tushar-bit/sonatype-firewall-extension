/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.google.common.annotations.VisibleForTesting;

/**
 * Servlet filter that audits the current HTTP request. Must be used before Shiro filter to capture
 * authentication-related failures.
 */
@Named
public class AuditFilter
    implements Filter
{
  public static final String[] URL_PATTERNS = {"/rest/*", "/api/*", "/saml/*", "/oidc/*"};

  /**
   * Audits the HTTP status code of a response.
   */
  @VisibleForTesting
  static class ResponseWrapper
      extends HttpServletResponseWrapper
  {
    public ResponseWrapper(ServletResponse response) {
      super((HttpServletResponse) response);
    }

    @Override
    public void setStatus(int statusCode) {
      onStatus(statusCode);
      super.setStatus(statusCode);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setStatus(int statusCode, String sm) {
      onStatus(statusCode);
      super.setStatus(statusCode, sm);
    }

    @Override
    public void sendError(int statusCode) throws IOException {
      onStatus(statusCode);
      super.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String msg) throws IOException {
      onStatus(statusCode);
      super.sendError(statusCode, msg);
    }

    private void onStatus(int statusCode) {
      // sometimes errors are conveyed via 2xx responses
      // so do not overwrite a previously set code unless we actually observe an error
      if (statusCode >= 400) {
        AuditData.get().setHttpStatus(statusCode);
      }
    }
  }

  private final AuditRecorder auditRecorder;

  @Inject
  public AuditFilter(AuditRecorder auditRecorder) {
    this.auditRecorder = auditRecorder;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    try (AuditSession auditSession = auditRecorder.recordUserEvent((HttpServletRequest) request)) {
      chain.doFilter(request, new ResponseWrapper(response));
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // noop
  }

  @Override
  public void destroy() {
    // noop
  }
}
