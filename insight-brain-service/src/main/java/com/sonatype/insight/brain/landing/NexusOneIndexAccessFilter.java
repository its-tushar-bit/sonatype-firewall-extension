/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.io.IOException;

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

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.CurrentUser;

/**
 * Gates the Nexus One SPA shell at {@value #URL_PATTERN}. When the master
 * {@link SystemConfigurationPropertyFeature#PREVIEW_NEXUS_ONE_UI} flag is OFF or the caller is
 * anonymous, redirects to the classic IQ shell so customers see no change by default.
 *
 * @since CLM-39548
 */
@Named
public class NexusOneIndexAccessFilter
    implements Filter
{
  public static final String URL_PATTERN = "/assets/nexus-one/index.html";

  private static final String CLASSIC_INDEX_PATH = "/assets/index.html";

  private final CurrentUser currentUser;

  @Inject
  public NexusOneIndexAccessFilter(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    if (!shouldAllowNexusOneIndex()) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      String redirectTarget = httpRequest.getContextPath() + CLASSIC_INDEX_PATH;
      httpResponse.sendRedirect(redirectTarget);
      return;
    }
    chain.doFilter(request, response);
  }

  boolean shouldAllowNexusOneIndex() {
    if (!SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.isEnabled()) {
      return false;
    }
    return currentUser.getUserPrincipal() != null;
  }
}
