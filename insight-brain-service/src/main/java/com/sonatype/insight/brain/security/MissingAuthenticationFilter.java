/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternal;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.security.SamlFilter.SAML_REQUEST_PATH;

/**
 * Filter to be used at the end of the authentication chain to block access if the subject was not authenticated by any
 * of the preceding filters.
 */
@Named
@Singleton
class MissingAuthenticationFilter
    extends AuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(MissingAuthenticationFilter.class);

  public static final String AUTH_HEADER_SAML = "SAML";

  public static final String AUTH_HEADER_OIDC = "OIDC";

  public static final String SSO_LOGIN_URL = "X-SSO-Login-URL";

  private final SamlConfigurationInternalDAO samlConfigurationInternalDAO;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  @Inject
  public MissingAuthenticationFilter(
      SamlConfigurationInternalDAO samlConfigurationInternalDAO,
      OidcConfigurationDAO oidcConfigurationDAO)
  {
    this.samlConfigurationInternalDAO = samlConfigurationInternalDAO;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Defensive check: Skip header logic for SAML/OIDC endpoints
    // Note: This filter should NOT be in the filter chain for /saml/** or /oidc/** paths
    // (they have their own dedicated filter chains), but we check defensively
    String requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

    if (!requestPath.startsWith(SAML_REQUEST_PATH) && !requestPath.startsWith("/oidc")) {
      // Determine available SSO methods and set WWW-Authenticate header
      Optional<String> ssoMethod = getAvailableSsoMethods();
      if (ssoMethod.isPresent()) {
        String method = ssoMethod.get();
        httpResponse.setHeader("WWW-Authenticate", method);

        // Set the SSO login URL for the frontend to redirect to
        // Include context path to ensure it works in environments with non-root context paths
        String ssoLoginUrl = getSsoLoginUrl(method);
        if (ssoLoginUrl != null) {
          httpResponse.setHeader(SSO_LOGIN_URL, getFullSsoLoginUrl(httpRequest, ssoLoginUrl));
        }

        // Note: We don't set X-SAML-IdP header here for the following reason:
        // The current React-based LoginModal doesn't use this header - it was only used in the
        //    legacy AngularJS login modal (pre-CLM-20634) to display text like
        //    "Single sign-on via {IdP name} by clicking the Single Sign-On button"
        // but it was removed with migration to React (CLM-20152)
      }
    }

    LoginErrorResponseHandler.sendError(httpResponse,
        new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS));
    return false;
  }

  private String getFullSsoLoginUrl(final HttpServletRequest httpRequest, final String ssoLoginUrl) {
    String contextPath = httpRequest.getContextPath();
    return contextPath + ssoLoginUrl;
  }

  private Optional<String> getAvailableSsoMethods() {
    if (SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();
      if (oidcConfiguration != null) {
        return Optional.of(AUTH_HEADER_OIDC);
      }
    }

    SamlConfigurationInternal samlConfigurationInternal = samlConfigurationInternalDAO.get();
    if (samlConfigurationInternal != null) {
      return Optional.of(AUTH_HEADER_SAML);
    }

    return Optional.empty();
  }

  /**
   * Generates the SSO login URL based on the authentication method. Returns the login endpoint path (e.g.,
   * "/saml/login" or "/oidc/login").
   *
   * @param authMethod The authentication method (SAML or OIDC)
   * @return The SSO login URL path, or null if method is unknown
   */
  private String getSsoLoginUrl(String authMethod) {
    if (AUTH_HEADER_OIDC.equals(authMethod)) {
      return "/oidc/login";
    }
    else if (AUTH_HEADER_SAML.equals(authMethod)) {
      return "/saml/login";
    }
    return null;
  }
}
