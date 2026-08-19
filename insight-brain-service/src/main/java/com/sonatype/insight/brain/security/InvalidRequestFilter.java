/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.Configuration;

@Named
@Singleton
public class InvalidRequestFilter
    extends org.apache.shiro.web.filter.InvalidRequestFilter
{
  private final Configuration configuration;

  @Inject
  public InvalidRequestFilter(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected boolean isAccessAllowed(
      final ServletRequest request,
      final ServletResponse response,
      final Object mappedValue) throws Exception
  {
    return !isTraceOrTrack(request) && super.isAccessAllowed(request, response, mappedValue);
  }

  @Override
  protected boolean onAccessDenied(final ServletRequest request, final ServletResponse response) throws Exception {
    if (isTraceOrTrack(request)) {
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return false;
    }
    return super.onAccessDenied(request, response);
  }

  @Override
  public boolean isBlockSemicolon() {
    return configuration.isBlockSemicolon();
  }

  @Override
  public void setBlockSemicolon(boolean blockSemicolon) {
    configuration.setBlockSemicolon(blockSemicolon);
  }

  @Override
  public boolean isBlockBackslash() {
    return configuration.isBlockBackslash();
  }

  @Override
  public void setBlockBackslash(boolean blockBackslash) {
    configuration.setBlockBackslash(blockBackslash);
  }

  @Override
  public boolean isBlockNonAscii() {
    return configuration.isBlockNonAscii();
  }

  private boolean isTraceOrTrack(final ServletRequest request) {
    if (!(request instanceof HttpServletRequest httpServletRequest)) {
      return false;
    }
    String method = httpServletRequest.getMethod();
    return "TRACE".equalsIgnoreCase(method) || "TRACK".equalsIgnoreCase(method);
  }

  @Override
  public void setBlockNonAscii(boolean blockNonAscii) {
    configuration.setBlockNonAscii(blockNonAscii);
  }
}
