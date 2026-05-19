/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import java.util.Arrays;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.BaseUrlConfiguration;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.google.inject.Binder;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.servlet.ServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.LOGOUT_AUTH0_ON_LOGOUT;
import static com.sonatype.insight.brain.security.SamlFilter.MSG_SAML_INTERNAL_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlFilterTest
    extends AbstractComponentTest
{
  @Inject
  public SamlDeploymentManager samlDeploymentManager;

  @Inject
  public SamlFilter samlFilter;

  @Inject
  private BaseUrl baseUrl;

  @Mock
  private HttpServletRequest mockHttpServletRequest;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private SamlSessionStoreForRedirect mockSamlSessionStore;

  @Mock
  private SamlAuthenticator mockSamlAuthenticator;

  @Mock
  private AuthChallenge mockAuthChallenge;

  @Mock
  private Configuration mockConfiguration;

  private ServletHttpFacade spyServletHttpFacade;

  @Before
  public void before() {
    spyServletHttpFacade = spy(new ServletHttpFacade(mockHttpServletRequest, mockHttpServletResponse));
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(Configuration.class).toInstance(mockConfiguration);
    super.configure(binder);
  }

  @After
  public void exit() {
    baseUrl.release();
  }

  @Test
  public void testInstantiation() {
    assertThat(samlFilter.getSamlSessionIdMapper()).isInstanceOf(SamlSessionIdMapper.class);
  }

  @Test
  public void testOnPrehandle_NullSamlDeployment_ReturnsTrue() throws Exception {
    assertThat(samlDeploymentManager.get()).isNull();
    assertThat(samlFilter.onPreHandle(null, null, null)).isTrue();
  }

  @Test
  public void testOnPrehandle_NonSamlEndpointAuthenticated_ReturnsTrue() throws Exception {
    testOnPrehandle("http://localhost:8070/assets/index.html", "/rest/product/features", "", AuthOutcome.AUTHENTICATED,
        true);
  }

  @Test
  public void testOnPrehandle_SamlEndpointAuthenticated_ReturnsFalse() throws Exception {
    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", AuthOutcome.AUTHENTICATED, false);
  }

  @Test
  public void testOnPrehandle_NotAttemptedAndAllowed_ReturnsTrue() throws Exception {
    when(subject.isAuthenticated()).thenReturn(true);

    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", AuthOutcome.NOT_ATTEMPTED, true);
  }

  @Test
  public void testOnPrehandle_NotAttemptedAndAllowed_ResponseNotEnded_ReturnsTrue() throws Exception {
    when(subject.isAuthenticated()).thenReturn(true);
    when(spyServletHttpFacade.isEnded()).thenReturn(false);

    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", AuthOutcome.NOT_ATTEMPTED, true);
  }

  @Test
  public void testOnPrehandle_LoggedOut_HomePageWithoutForwardSlash_ReturnsFalse() throws Exception {
    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", AuthOutcome.LOGGED_OUT, false);

    verify(mockSamlSessionStore).logoutAccount();
    verify(subject).logout();
    verify(mockHttpServletResponse).sendRedirect("/");
  }

  @Test
  public void testOnPrehandle_LoggedOut_HomePageWithForwardSlash_ReturnsFalse() throws Exception {
    testOnPrehandle("http://localhost:8070/assets/index.html", "/context/saml", "/context/", AuthOutcome.LOGGED_OUT,
        false);

    verify(mockSamlSessionStore).logoutAccount();
    verify(subject).logout();
    verify(mockHttpServletResponse).sendRedirect("/context/");
  }

  @Test
  public void testOnPrehandle_Failed_RedirectsToIdPLogoutUrl_ReturnsFalse() throws Exception {
    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070/", true));

    testOnPrehandle("http://localhost:8070/assets/index.html", "/context/saml", "/context/", AuthOutcome.FAILED,
        false);

    verify(mockHttpServletResponse).sendRedirect(
        "https://http://idp-entity-id/v2/logout?client_id=/localhost&returnTo=http://localhost:8070/");
  }

  @Test
  public void testOnPrehandle_Failed_ShowErrorMessage_ReturnsFalse() throws Exception {
    testOnPrehandle("http://localhost:8070/assets/index.html", "/context/saml", "/context/", AuthOutcome.FAILED,
        false);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(mockHttpServletResponse).setContentType(ErrorResponse.CONTENT_TYPE);
  }

  @Test
  public void testOnPrehandle_Default_ReturnsFalse() throws Exception {
    mockAuthChallenge = null;

    testOnPrehandle("http://localhost:8070/some/path/something.html", "/saml", "", null, false);
  }

  @Test
  public void testOnPrehandle_ChallengeSamlPath_ReturnsFalse() throws Exception {
    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", null, false);

    verify(mockHttpServletRequest).removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);
    verify(mockAuthChallenge).challenge(any(HttpFacade.class));
    verify(mockHttpServletResponse, times(0)).setHeader("Content-Security-Policy", "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline'; img-src 'self'; style-src 'self';");
  }

  @Test
  public void testOnPrehandle_ChallengeSamlPath_ReturnsFalse_CspEnabled() throws Exception {
    when(mockConfiguration.isCspEnabled()).thenReturn(true);

    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", null, false);

    verify(mockHttpServletRequest).removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);
    verify(mockAuthChallenge).challenge(any(HttpFacade.class));
    verify(mockHttpServletResponse).setHeader("Content-Security-Policy", "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline'; img-src 'self'; style-src 'self';");
  }

  @Test
  public void testOnPrehandle_ChallengeSamlPath_ReturnsFalse_CspEnabledAndFrameAncestors() throws Exception {
    when(mockConfiguration.isCspEnabled()).thenReturn(true);
    when(mockConfiguration.getFrameAncestorsAllowList()).thenReturn(Arrays.asList("ancestor1", "ancestor2"));

    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", null, false);

    verify(mockHttpServletRequest).removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);
    verify(mockAuthChallenge).challenge(any(HttpFacade.class));
    verify(mockHttpServletResponse).setHeader("Content-Security-Policy", "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline'; img-src 'self'; style-src 'self';frame-ancestors ancestor1 ancestor2;");
  }

  @Test
  public void testOnPreHandle_ChallengeSamlPath_InternalError() throws Exception {
    when(mockAuthChallenge.challenge(any(HttpFacade.class))).thenThrow(new RuntimeException("serious error"));
    testOnPrehandle("http://localhost:8070/assets/index.html", "/saml", "", null, false);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(mockHttpServletResponse).setContentType(ErrorResponse.CONTENT_TYPE);
    verify(mockHttpServletResponse.getWriter()).print(MSG_SAML_INTERNAL_ERROR);
  }

  @Test
  public void testOnPrehandle_ChallengeNonSamlPath_ReturnsTrue() throws Exception {
    // SamlFilter now returns true (continues to next filter) for non-SAML paths with challenge
    // The MissingAuthenticationFilter will handle setting WWW-Authenticate header
    testOnPrehandle("http://localhost:8070/assets/index.html", "/rest/product/features", "", null, true);

    // Verify that SamlFilter does NOT set these headers anymore
    verify(mockHttpServletResponse, times(0)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockHttpServletResponse, times(0)).setHeader("WWW-Authenticate", "SAML");
    verify(mockHttpServletResponse, times(0)).setHeader("X-SAML-IdP", "identity provider");
  }

  @Test
  public void testNewSamlSessionStore_IsSamlSessionStoreForRedirect() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("https://host.test:1234/iq", true));
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.newSamlSessionStore(mockHttpServletRequest, null, null))
        .isInstanceOf(SamlSessionStoreForRedirect.class);
  }

  @Test
  public void testNewSamlAuthenticator_GivenSamlEndpoint_ReturnsSamlAuthenticatorForSamlEndpoint() {
    HttpFacade mockHttpFacade = mock(HttpFacade.class);
    when(mockHttpFacade.getRequest()).thenReturn(mock(Request.class));

    assertThat(samlFilter.newSamlAuthenticator(true, mockHttpFacade, samlDeploymentManager.get(), null))
        .isInstanceOf(SamlAuthenticatorForSamlEndpoint.class);
  }

  @Test
  public void testNewSamlAuthenticator_GivenNonSamlEndpoint_ReturnsSamlAuthenticatorForNonSamlEndpoint() {
    HttpFacade mockHttpFacade = mock(HttpFacade.class);
    when(mockHttpFacade.getRequest()).thenReturn(mock(Request.class));

    assertThat(samlFilter.newSamlAuthenticator(false, mockHttpFacade, samlDeploymentManager.get(), null))
        .isInstanceOf(SamlAuthenticatorForNonSamlEndpoint.class);
  }

  @Test
  public void testOnAccessDenied_ThrowsIllegalStateException() {
    assertThatThrownBy(() -> samlFilter.onAccessDenied(null, null)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testGetDestinationOrDefault_NoHash_ReturnsDefault() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070", true));
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("http://localhost:8070/assets/index.html");
  }

  @Test
  public void testGetDestinationOrDefault_Hash_ReturnsWithHash() {
    String hash = "#/some/example";
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockHttpServletRequest.getQueryString()).thenReturn("hash=" + hash);
    when(mockHttpServletRequest.getParameter("hash")).thenReturn(hash);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070", true));
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("http://localhost:8070/assets/index.html" + hash);
  }

  @Test
  public void testGetDestinationOrDefault_ForcedBaseUrl() {
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("https://host.test:1234/iq", true));
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("https://host.test:1234/iq/assets/index.html");
  }

  @Test
  public void testGetDestinationOrDefault_GuideOrigin_ReturnsGuideUrl() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070", true));
    lenient().when(mockHttpServletRequest.getParameter("hash")).thenReturn(null);
    when(mockHttpServletRequest.getParameter("origin")).thenReturn("guide");
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("http://localhost:8070/assets/guide/index.html");
  }

  @Test
  public void testGetDestinationOrDefault_GuideOriginWithHash_ReturnsGuideUrlWithHash() {
    String hash = "#/dashboard";
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockHttpServletRequest.getQueryString()).thenReturn("hash=" + hash);
    lenient().when(mockHttpServletRequest.getParameter("hash")).thenReturn(hash);
    when(mockHttpServletRequest.getParameter("origin")).thenReturn("guide");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070", true));
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("http://localhost:8070/assets/guide/index.html" + hash);
  }

  @Test
  public void testGetDestinationOrDefault_GuideOriginWithHashContainingQueryString_PreservesQuestionMark() {
    String hash = "#/vulnerabilities?severities=critical";
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockHttpServletRequest.getQueryString()).thenReturn("hash=" + hash);
    lenient().when(mockHttpServletRequest.getParameter("hash")).thenReturn(hash);
    when(mockHttpServletRequest.getParameter("origin")).thenReturn("guide");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070", true));
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("http://localhost:8070/assets/guide/index.html" + hash);
  }

  @Test
  public void testGetDestinationOrDefault_InvalidOrigin_ReturnsIqUrl() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    lenient().when(mockHttpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost:8070/context/place"));
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn("/context/place");
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn("/context");
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(
        new BaseUrlConfiguration("http://localhost:8070", true));
    lenient().when(mockHttpServletRequest.getParameter("hash")).thenReturn(null);
    when(mockHttpServletRequest.getParameter("origin")).thenReturn("evil");
    baseUrl.capture(mockHttpServletRequest);

    assertThat(samlFilter.getDestinationOrDefault(mockHttpServletRequest))
        .isEqualTo("http://localhost:8070/assets/index.html");
  }

  private void testOnPrehandle(
      String referer,
      String requestUri,
      String contextPath,
      AuthOutcome authOutcome,
      boolean expectedResult) throws Exception
  {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    samlDeploymentManager.updateFromConfiguration();
    lenient().when(mockHttpServletRequest.getHeader("Referer")).thenReturn(referer);
    lenient().when(mockHttpServletRequest.getRequestURI()).thenReturn(requestUri);
    lenient().when(mockHttpServletRequest.getContextPath()).thenReturn(contextPath);
    lenient().when(mockHttpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));
    SamlFilter spySamlFilter = spy(this.samlFilter);
    lenient().doReturn(mockSamlSessionStore)
        .when(spySamlFilter)
        .newSamlSessionStore(any(HttpServletRequest.class), any(HttpFacade.class), any(SamlDeployment.class));
    lenient().when(mockSamlAuthenticator.getChallenge()).thenReturn(mockAuthChallenge);
    lenient().when(mockSamlAuthenticator.authenticate()).thenReturn(authOutcome);
    lenient().doReturn(mockSamlAuthenticator)
        .when(spySamlFilter)
        .newSamlAuthenticator(anyBoolean(),
            any(HttpFacade.class), any(SamlDeployment.class), any(SamlSessionStoreForRedirect.class));
    lenient().when(spySamlFilter.newServletHttpFacade(any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenReturn(spyServletHttpFacade);
    assertThat(spySamlFilter.onPreHandle(mockHttpServletRequest, mockHttpServletResponse, null))
        .isSameAs(expectedResult);
  }
}
