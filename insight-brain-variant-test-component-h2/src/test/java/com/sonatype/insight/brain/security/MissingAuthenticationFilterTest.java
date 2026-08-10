/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class MissingAuthenticationFilterTest
    extends AbstractComponentH2Test
{
  @Inject
  public SamlConfigurationInternalDAO samlConfigurationInternalDAO;

  @Mock
  public HttpServletRequest mockHttpServletRequest;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private PrintWriter mockPrintWriter;

  @Inject
  public MissingAuthenticationFilter filter;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    when(mockHttpServletResponse.getWriter()).thenReturn(mockPrintWriter);
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/rest/api/endpoint");
    when(mockHttpServletRequest.getContextPath()).thenReturn("");
  }

  @Test
  public void testOnAccessDenied_NoSsoConfigured_NoWwwAuthenticateHeader() {
    // No SAML or OIDC configured
    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    verify(mockHttpServletResponse, never()).setHeader("WWW-Authenticate", "SAML");
    verify(mockHttpServletResponse, never()).setHeader("WWW-Authenticate", "OIDC");
  }

  @Test
  public void testOnAccessDenied_OnlySamlConfigured_SetsSamlWwwAuthenticateHeader() {
    // SAML configured, OIDC not configured
    tempEntity.newSamlConfigurationInternal();

    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    verify(mockHttpServletResponse).setHeader("WWW-Authenticate", "SAML");
    verify(mockHttpServletResponse).setHeader("X-SSO-Login-URL", "/saml/login");
    // X-SAML-IdP header is no longer set to avoid keystore loading issues
  }

  @Test
  public void testOnAccessDenied_OnlyOidcConfigured_SetsOidcWwwAuthenticateHeader() {
    // OIDC configured (and feature enabled), SAML not configured
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tempEntity.newOidcConfiguration(
        "https://issuer.example.com",
        "test-client-id",
        "test-client-secret",
        "https://issuer.example.com/auth",
        "https://issuer.example.com/token");

    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    verify(mockHttpServletResponse).setHeader("WWW-Authenticate", "OIDC");
    verify(mockHttpServletResponse).setHeader("X-SSO-Login-URL", "/oidc/login");
    verify(mockHttpServletResponse, never()).setHeader("X-SAML-IdP", "identity provider");
  }

  @Test
  public void testOnAccessDenied_BothSamlAndOidcConfigured_SetsOnlyOIDCAuthenticateHeader() {
    // Both SAML and OIDC configured - OIDC takes precedence
    tempEntity.newSamlConfigurationInternal();
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tempEntity.newOidcConfiguration(
        "https://issuer.example.com",
        "test-client-id",
        "test-client-secret",
        "https://issuer.example.com/auth",
        "https://issuer.example.com/token");

    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    verify(mockHttpServletResponse).setHeader("WWW-Authenticate", "OIDC");
    verify(mockHttpServletResponse).setHeader("X-SSO-Login-URL", "/oidc/login");
  }

  @Test
  public void testOnAccessDenied_OidcConfiguredButFeatureDisabled_OnlySetsHeaders() {
    // OIDC configured but OAUTH2_ENABLED feature is disabled
    tempEntity.newSamlConfigurationInternal();
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);
    tempEntity.newOidcConfiguration(
        "https://issuer.example.com",
        "test-client-id",
        "test-client-secret",
        "https://issuer.example.com/auth",
        "https://issuer.example.com/token");

    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    // Only SAML should be in the header since OAUTH2_ENABLED is disabled
    verify(mockHttpServletResponse).setHeader("WWW-Authenticate", "SAML");
    verify(mockHttpServletResponse).setHeader("X-SSO-Login-URL", "/saml/login");
  }

  @Test
  public void testOnAccessDenied_SamlEndpoint_NoWwwAuthenticateHeader() {
    // SAML endpoint should not set WWW-Authenticate header
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/saml");
    when(mockHttpServletRequest.getContextPath()).thenReturn("");

    tempEntity.newSamlConfigurationInternal();

    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    verify(mockHttpServletResponse, never()).setHeader("WWW-Authenticate", "SAML");
  }

  @Test
  public void testOnAccessDenied_OidcEndpoint_NoWwwAuthenticateHeader() {
    // OIDC endpoint should not set WWW-Authenticate header
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/oidc/login");
    when(mockHttpServletRequest.getContextPath()).thenReturn("");

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tempEntity.newOidcConfiguration(
        "https://issuer.example.com",
        "test-client-id",
        "test-client-secret",
        "https://issuer.example.com/auth",
        "https://issuer.example.com/token");

    filter.onAccessDenied(mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    verify(mockHttpServletResponse, never()).setHeader("WWW-Authenticate", "OIDC");
  }
}
