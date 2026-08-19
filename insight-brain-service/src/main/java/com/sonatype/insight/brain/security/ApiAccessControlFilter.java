/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

@Named
@Singleton
public class ApiAccessControlFilter
    implements Filter
{
  private final CurrentUser currentUser;

  private final Configuration configuration;

  private final MembershipMappingDAO membershipMappingDAO;

  @Inject
  public ApiAccessControlFilter(
      CurrentUser currentUser,
      Configuration configuration,
      MembershipMappingDAO membershipMappingDAO)
  {
    this.currentUser = currentUser;
    this.configuration = configuration;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // no op
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (isUiRequest(httpRequest)) {
      // UI originated request - pass through
      chain.doFilter(request, response);
      return;
    }

    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    if (userPrincipal == null) {
      // reject the response - 401 - this filter is supposed to be used only for authenticated requests
      rejectResponse(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
      return;
    }

    if (userInApiAccessAllowList(userPrincipal)) {
      // user is allowed to access public API - pass through
      chain.doFilter(request, response);
      return;
    }

    if (userIsSystemAdmin(userPrincipal)) {
      // sys admins are always allowed - pass through
      chain.doFilter(request, response);
      return;
    }

    // reject the response - 403
    rejectResponse(httpResponse, HttpServletResponse.SC_FORBIDDEN, "Access denied");
  }

  private void rejectResponse(HttpServletResponse httpResponse, int statsCode, String message) throws IOException {
    httpResponse.setStatus(statsCode);
    httpResponse.setContentType(MediaType.TEXT_PLAIN);
    try (PrintWriter writer = httpResponse.getWriter()) {
      writer.print(message);
    }
  }

  private boolean isUiRequest(HttpServletRequest httpRequest) {
    return httpRequest.getHeader(AntiCsrfFilter.CSRF_HEADER_NAME) != null;
  }

  private boolean userInApiAccessAllowList(UserPrincipal user) {
    List<String> apiAccessAllowList = configuration.getApiAccessAllowList();
    if (apiAccessAllowList == null || apiAccessAllowList.isEmpty()) {
      return true;
    }
    for (String id : apiAccessAllowList) {
      if (user.getUsername().equals(id)) {
        return true;
      }
    }
    return false;
  }

  private boolean userIsSystemAdmin(final UserPrincipal user) {
    return membershipMappingDAO.isSystemAdmin(user.getUsername());
  }

  @Override
  public void destroy() {
    // no op
  }
}
