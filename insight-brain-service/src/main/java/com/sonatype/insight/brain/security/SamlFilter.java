/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.landing.LandingService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.ecp.EcpAuthenticationHandler;
import org.keycloak.adapters.servlet.ServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that will initiate a SAML-based login if the calling subject has not been authenticated previously via other
 * means.
 */
@Named
@Singleton
class SamlFilter
    extends AuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(SamlFilter.class);

  static final String MSG_SAML_FAILURE =
      "Authentication failed due to SAML error. Please contact your IT administrator.";

  static final String MSG_SAML_INTERNAL_ERROR =
      "Internal error in SAML authentication process initiation. Please contact your IT administrator.";

  public static final String SAML_REQUEST_PATH = "/saml";

  private final SamlDeploymentManager samlDeploymentManager;

  private final LandingService landingService;

  private final SamlSessionIdMapper samlSessionIdMapper;

  private final IdPLogoutUrlBuilder idPLogoutUrlBuilder;

  private final Configuration configuration;

  @Inject
  public SamlFilter(
      SamlDeploymentManager samlDeploymentManager,
      LandingService landingService,
      SamlSessionIdMapper samlSessionIdMapper,
      IdPLogoutUrlBuilder idPLogoutUrlBuilder,
      Configuration configuration)
  {
    this.samlDeploymentManager = samlDeploymentManager;
    this.landingService = landingService;
    this.samlSessionIdMapper = samlSessionIdMapper;
    this.idPLogoutUrlBuilder = idPLogoutUrlBuilder;
    this.configuration = configuration;
  }

  // Visible for testing
  SamlSessionIdMapper getSamlSessionIdMapper() {
    return samlSessionIdMapper;
  }

  @Override
  public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
    SamlDeployment samlDeployment = samlDeploymentManager.get();

    if (samlDeployment == null) {
      return true;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
    boolean samlEndpoint = requestPath.equals(SAML_REQUEST_PATH);

    ServletHttpFacade httpFacade = newServletHttpFacade(httpRequest, httpResponse);
    SamlSessionStore samlSessionStore = newSamlSessionStore(httpRequest, httpFacade, samlDeployment);
    SamlAuthenticator samlAuthenticator =
        newSamlAuthenticator(samlEndpoint, httpFacade, samlDeployment, samlSessionStore);

    AuthOutcome outcome;
    try {
      outcome = samlAuthenticator.authenticate();
    }
    catch (Exception e) {
      log.error("SAML authentication failed: {}", e.getMessage(), e);
      outcome = AuthOutcome.FAILED;
    }
    if (outcome == AuthOutcome.AUTHENTICATED) {
      return !samlEndpoint;
    }
    if (outcome == AuthOutcome.NOT_ATTEMPTED && isAccessAllowed(request, response, mappedValue)) {
      return !httpFacade.isEnded();
    }
    if (outcome == AuthOutcome.LOGGED_OUT) {
      samlSessionStore.logoutAccount();
      getSubject(request, response).logout();
      String homePage = httpRequest.getContextPath();
      if (!homePage.endsWith("/")) {
        homePage += "/";
      }
      httpResponse.sendRedirect(homePage);
      return false;
    }
    if (outcome == AuthOutcome.FAILED) {
      URI idpLogoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();

      if (idpLogoutURI != null) {
        ((HttpServletResponse) response).sendRedirect(idpLogoutURI.toString());
      }
      else {
        LoginErrorResponseHandler.sendError(httpResponse,
            new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MSG_SAML_FAILURE));
      }
      return false;
    }

    AuthChallenge challenge = samlAuthenticator.getChallenge();
    if (challenge != null) {
      // there's no point in sending out a SAML challenge to a client which is not prepared for it
      if (requestPath.startsWith(SAML_REQUEST_PATH) || EcpAuthenticationHandler.canHandle(httpFacade)) {
        request.removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);

        if (configuration.isCspEnabled()) {
          String frameAncestorsHeader = "";
          List<String> allowList = configuration.getFrameAncestorsAllowList();
          if (allowList != null && !allowList.isEmpty()) {
            frameAncestorsHeader = "frame-ancestors " + String.join(" ", allowList) + ";";
          }

          httpResponse.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; " +
              "img-src 'self'; style-src 'self';" + frameAncestorsHeader);
        }

        log.debug("Initiating SAML authentication via identity provider");
        try {
          challenge.challenge(httpFacade);
        }
        catch (Exception e) {
          log.error("Error initiating SAML authentication request", e);
          LoginErrorResponseHandler.sendError(httpResponse,
              new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MSG_SAML_INTERNAL_ERROR));
        }
      }
      else {
        // For non-SAML paths: delegate to MissingAuthenticationFilter to set WWW-Authenticate header
        // Continue to the next filter in the chain which will handle the 401 response
        return true;
      }
    }

    return false;
  }

  @VisibleForTesting
  String getDestinationOrDefault(HttpServletRequest httpServletRequest) {
    String hash = httpServletRequest.getParameter("hash");
    if (hash == null) {
      hash = "";
    }
    else if (hash.startsWith("#")) {
      hash = hash.substring(1);
    }
    URI uri = UriBuilder.fromUri(landingService.getDestination()).replaceQuery("").fragment(hash).build();
    uri = URI.create(uri.toString().replaceAll("%2F", "/"));
    return uri.toString();
  }

  @VisibleForTesting
  ServletHttpFacade newServletHttpFacade(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    return new ServletHttpFacade(httpRequest, httpResponse);
  }

  @VisibleForTesting
  SamlSessionStore newSamlSessionStore(
      HttpServletRequest httpRequest,
      HttpFacade httpFacade,
      SamlDeployment samlDeployment)
  {
    return new SamlSessionStoreForRedirect(httpRequest, httpFacade, 0, samlSessionIdMapper, samlDeployment,
        getDestinationOrDefault(httpRequest));
  }

  @VisibleForTesting
  SamlAuthenticator newSamlAuthenticator(
      boolean samlEndpoint,
      HttpFacade httpFacade,
      SamlDeployment samlDeployment,
      SamlSessionStore samlSessionStore)
  {
    return samlEndpoint ? new SamlAuthenticatorForSamlEndpoint(httpFacade, samlDeployment, samlSessionStore)
        : new SamlAuthenticatorForNonSamlEndpoint(httpFacade, samlDeployment, samlSessionStore);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) {
    throw new IllegalStateException();
  }
}
