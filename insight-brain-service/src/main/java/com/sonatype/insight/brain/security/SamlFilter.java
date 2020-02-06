/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightBrainService;
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
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
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

  private final SamlDeploymentManager samlDeploymentManager;

  private final SessionIdMapper idMapper;

  @Inject
  public SamlFilter(SamlDeploymentManager samlDeploymentManager) {
    this.samlDeploymentManager = samlDeploymentManager;
    idMapper = new InMemorySessionIdMapper();
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
    boolean samlEndpoint = requestPath.equals("/saml");

    ServletHttpFacade httpFacade = new ServletHttpFacade(httpRequest, httpResponse);
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
      return true;
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
      LoginErrorResponseHandler.sendError(httpResponse,
          new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MSG_SAML_FAILURE));
      return false;
    }

    AuthChallenge challenge = samlAuthenticator.getChallenge();
    if (challenge != null) {
      // there's no point in sending out a SAML challenge to a client which is not prepared for it
      if (requestPath.startsWith("/saml") || EcpAuthenticationHandler.canHandle(httpFacade)) {
        request.removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);
        log.debug("Initiating SAML authentication via identity provider");
        challenge.challenge(httpFacade);
      }
      else {
        // let the UI know that SAML SSO should be a login option
        httpResponse.setHeader("WWW-Authenticate", "SAML");
        SamlConfiguration samlConfiguration = new SamlConfigurationDAO().get();
        if (samlConfiguration != null) {
          httpResponse.setHeader("X-SAML-IdP", samlConfiguration.getIdentityProviderName());
        }
        LoginErrorResponseHandler.sendError(httpResponse,
            new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS));
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
    URI uri = UriBuilder.fromUri(httpServletRequest.getRequestURL().toString())
        .replacePath(httpServletRequest.getContextPath()).path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html")
        .fragment(hash).build();
    uri = URI.create(uri.toString().replaceAll("%2F", "/"));
    return uri.toString();
  }

  @VisibleForTesting
  SamlSessionStore newSamlSessionStore(
      HttpServletRequest httpRequest,
      HttpFacade httpFacade,
      SamlDeployment samlDeployment)
  {
    return new SamlSessionStoreForRedirect(httpRequest, httpFacade, 0, idMapper, samlDeployment,
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
