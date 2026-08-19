/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.service.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.servlet.Cookie.SameSiteOptions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter using the "cookie-to-header token" approach to prevent cross-site request forgery. To be used in front of
 * another filter that handles logins.
 */
@Named
@Singleton
public class AntiCsrfFilter
    extends AuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(AntiCsrfFilter.class);

  static final String ERROR_MSG = "Invalid cross-site request forgery token";

  public static final String CSRF_COOKIE_NAME = "CLM-CSRF-TOKEN";

  public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

  public static final String EXPLICIT_AUTH_ALLOWED = "explicitAuthAllowed";

  public static final String FORM_POST_ALLOWED = "formPostAllowed";

  private static class PathConfig
  {
    // allow requests that don't use session cookie for auth without CSRF token
    final boolean explicitAuthAllowed;

    // allow requests that post old-school forms to pass the filter for manual validation in a later phase
    final boolean formPostAllowed;

    public PathConfig(String config) {
      String[] flags = StringUtils.split(config);
      explicitAuthAllowed = flags != null && Arrays.asList(flags).contains(EXPLICIT_AUTH_ALLOWED);
      formPostAllowed = flags != null && Arrays.asList(flags).contains(FORM_POST_ALLOWED);
    }
  }

  private final Configuration configuration;

  private final FrameEmbeddingDetector frameEmbeddingDetector;

  @Inject
  AntiCsrfFilter(Configuration configuration, FrameEmbeddingDetector frameEmbeddingDetector) {
    this.configuration = configuration;
    this.frameEmbeddingDetector = frameEmbeddingDetector;
  }

  @Override
  public Filter processPathConfig(String path, String config) {
    appliedPaths.put(path, new PathConfig(config));
    return this;
  }

  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    PathConfig pathConfig = (PathConfig) mappedValue;
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    Cookie csrfCookie = getCsrfCookie(httpRequest);
    if (csrfCookie == null) {
      addCsrfCookie(httpRequest, httpResponse);
    }

    if (isSafeMethod(httpRequest)) {
      return true;
    }
    if (pathConfig.formPostAllowed && isFormPost(httpRequest)) {
      return true;
    }
    if (pathConfig.explicitAuthAllowed && !getSubject(request, response).isAuthenticated()
        && !isReverseProxyAuthenticationWithCsrf(httpRequest))
    {
      return true;
    }

    return isCsrfHeaderValid(httpRequest, csrfCookie);
  }

  private boolean isSafeMethod(HttpServletRequest request) {
    String method = request.getMethod();
    return "GET".equals(method) || "HEAD".equals(method);
  }

  private boolean isFormPost(HttpServletRequest request) {
    return "POST".equals(request.getMethod()) && request.getContentType() != null
        && MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(MediaType.valueOf(request.getContentType()));
  }

  private boolean isCsrfHeaderValid(HttpServletRequest request, Cookie csrfCookie) {
    String csrfHeader = request.getHeader(CSRF_HEADER_NAME);
    return csrfHeader != null && csrfCookie != null && csrfHeader.equals(csrfCookie.getValue());
  }

  private Cookie getCsrfCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
          return cookie;
        }
      }
    }
    return null;
  }

  private void addCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
    SimpleCookie csrfCookie = new SimpleCookie(CSRF_COOKIE_NAME);
    csrfCookie.setValue(UUID.randomUUID().toString());
    csrfCookie.setPath("/");
    csrfCookie.setHttpOnly(false);
    csrfCookie.setSecure(request.isSecure());
    // Use SameSite=None only when IQ is HTTPS AND embedding in third-party iframes is enabled:
    // browsers reject SameSite=None without Secure (Chrome 80+, Firefox 79+). For HTTP or
    // non-iframe deployments, Lax is sufficient (CSRF token is read by same-origin JS only).
    csrfCookie.setSameSite(frameEmbeddingDetector.isFrameEmbeddingEnabled() && request.isSecure()
        ? SameSiteOptions.NONE
        : SameSiteOptions.LAX);
    csrfCookie.saveTo(request, response);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    auditBadToken();
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpResponse.setContentType("text/plain");
    httpResponse.getWriter().print(ERROR_MSG);
    if (getSubject(request, response).isAuthenticated()) {
      log.debug("Rejecting request from {} with valid session due to invalid cross-site request forgery token",
          request.getRemoteAddr());
    }
    else {
      log.debug("Rejecting request from {} without session due to invalid cross-site request forgery token",
          request.getRemoteAddr());
    }
    // abort filter chain, this request ends here
    return false;
  }

  /**
   * Allows manual CSRF validation by the REST resource for no other reason than to support IE9. Once IE9 is no longer
   * supported, nuke this, along with {@link #FORM_POST_ALLOWED}.
   */
  public void validate(String csrfToken, HttpHeaders headers) {
    if (!isEnabled()) {
      return;
    }
    String csrfHeader = headers.getRequestHeaders().getFirst(CSRF_HEADER_NAME);
    if (csrfHeader == null) {
      // if this was indeed a non-AJAX request, the token better be in the form data
      csrfHeader = csrfToken;
    }
    jakarta.ws.rs.core.Cookie csrfCookie = headers.getCookies().get(CSRF_COOKIE_NAME);
    if (csrfHeader == null || csrfCookie == null || !csrfHeader.equals(csrfCookie.getValue())) {
      auditBadToken();
      throw new UnauthenticatedException(ERROR_MSG);
    }
  }

  private void auditBadToken() {
    AuditData.get().setEvent(AuditEvent.AUTHENTICATION_FAILURE);
    AuditData.get().setError("bad-csrf-token");
  }

  private boolean isReverseProxyAuthenticationWithCsrf(final HttpServletRequest httpRequest) {
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        configuration.getReverseProxyAuthenticationConfiguration();
    return reverseProxyAuthenticationConfiguration != null
        && reverseProxyAuthenticationConfiguration.isEnabled()
        && httpRequest.getHeader(reverseProxyAuthenticationConfiguration.getUsernameHeader()) != null
        && !reverseProxyAuthenticationConfiguration.isCsrfProtectionDisabled();
  }

  @Override
  public boolean isEnabled() {
    return configuration.isAntiCsrfEnabled();
  }

  @Override
  public void setEnabled(boolean enabled) {
    configuration.setAntiCsrfEnabled(enabled);
  }
}
