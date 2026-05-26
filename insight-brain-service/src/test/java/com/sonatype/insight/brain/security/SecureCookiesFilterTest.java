/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class SecureCookiesFilterTest
{
  private static final String SET_COOKIE = "Set-Cookie";

  private static final String SESSION_COOKIE_NAME = SecurityConfiguration.SESSION_COOKIE_NAME;

  private static final String COOKIE_2_INSECURE = "simple=cookie";

  private static final String COOKIE_3_SECURE = SecurityConfiguration.SESSION_COOKIE_NAME
      + "=98a766bc-bc33-4b3c-9d9f-d3bb85b0cf00; Path=/; HttpOnly" + SecureCookiesFilter.SECURE_FLAGS;

  private static final String COOKIE_4_SECURE =
      "rememberMe=deleteMe; Path=/; HttpOnly" + SecureCookiesFilter.SECURE_FLAGS;

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private FilterChain filterChain;

  @Mock
  private ServletResponse notInstanceOfHttpServletResponse;

  @Mock
  private FrameEmbeddingDetector frameEmbeddingDetector;

  private SecureCookiesFilter cookieFilter;

  @Before
  public void setUp() {
    this.cookieFilter = new SecureCookiesFilter(frameEmbeddingDetector);
  }

  // ---- CLMSESSIONID on non-SAML path ----

  @Test
  public void secureNonSamlRequest_sessionCookieGetsSecureAndSameSiteLax() throws Exception {
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("; Secure");
    assertThat(written).containsIgnoringCase("SameSite=Lax");
    assertThat(written).doesNotContainIgnoringCase("SameSite=None");
  }

  @Test
  public void secureNonSamlRequest_sessionCookieHasExactlyOneSameSiteAttribute() throws Exception {
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(countOccurrences(written.toLowerCase(), "samesite=")).isEqualTo(1);
    assertThat(written).containsIgnoringCase("SameSite=Lax");
  }

  // ---- CLMSESSIONID on SAML path ----

  @Test
  public void secureSamlPath_sessionCookieGetsSecureAndSameSiteNone() throws Exception {
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/saml/login");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("; Secure");
    assertThat(written).containsIgnoringCase("SameSite=None");
    assertThat(written).doesNotContainIgnoringCase("SameSite=Lax");
  }

  @Test
  public void secureSamlPath_existingSameSiteReplacedWithNone() throws Exception {
    // Cookie already has SameSite=Lax — filter must replace it, not append a second one
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly; SameSite=Lax";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/saml/callback");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(countOccurrences(written.toLowerCase(), "samesite=")).isEqualTo(1);
    assertThat(written).containsIgnoringCase("SameSite=None");
  }

  @Test
  public void secureRequest_bareSamlPath_getsSameSiteLax() throws Exception {
    // /saml (bare, no trailing slash) IS the SAML ACS endpoint that receives the IdP's cross-site POST.
    // SameSite=Lax is still safe here: the cross-site POST has already been handled by the time the
    // response sets the session cookie, and the subsequent redirect back to IQ Server is same-site.
    // Documents the current behavior to prevent accidental regressions.
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/saml"); // bare /saml, no trailing slash
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("SameSite=Lax");
    assertThat(written).doesNotContainIgnoringCase("SameSite=None");
  }

  // ---- Non-session cookies ----

  @Test
  public void secureRequest_nonSessionCookieWithoutSameSite_getsSecureNoSameSiteInjected() throws Exception {
    String otherCookie = "rememberMe=deleteMe; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/dashboard");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(otherCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("; Secure");
    assertThat(written).doesNotContainIgnoringCase("SameSite");
  }

  @Test
  public void secureRequest_nonSessionCookieWithExistingSameSite_preservesSameSite() throws Exception {
    String otherCookie = "some-cookie=value; Path=/; SameSite=Strict";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/dashboard");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(otherCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("; Secure");
    assertThat(written).containsIgnoringCase("SameSite=Strict");
    assertThat(written).doesNotContainIgnoringCase("SameSite=None");
  }

  // ---- Already-secure cookie (idempotency) ----

  @Test
  public void secureRequest_cookieAlreadyHasSecure_doesNotDuplicateSecure() throws Exception {
    String alreadySecureCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly; Secure";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(alreadySecureCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(countOccurrences(written.toLowerCase(), "; secure")).isEqualTo(1);
  }

  // ---- Mixed-case attributes ----

  @Test
  public void secureRequest_mixedCaseSameSiteAndSecure_normalizesWithoutDuplication() throws Exception {
    // Cookie arrives with mixed-case SECURE and SAMESITE attributes
    String mixedCaseCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; SECURE; SAMESITE=lax";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(mixedCaseCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(countOccurrences(written.toLowerCase(), "samesite=")).isEqualTo(1);
    assertThat(written).containsIgnoringCase("SameSite=Lax");
    assertThat(countOccurrences(written.toLowerCase(), "; secure")).isEqualTo(1);
  }

  // ---- Non-secure request ----

  @Test
  public void nonSecureRequest_responseIsUnchanged() throws Exception {
    when(request.isSecure()).thenReturn(false);

    cookieFilter.doFilter(request, response, filterChain);

    verifyNoInteractions(response);
  }

  // ---- Not HttpServletResponse ----

  @Test
  public void notHttpServletResponse_responseIsUnchanged() throws Exception {
    when(request.isSecure()).thenReturn(true); // Ensure the instanceof guard is actually exercised

    cookieFilter.doFilter(request, notInstanceOfHttpServletResponse, filterChain);

    verifyNoInteractions(notInstanceOfHttpServletResponse);
  }

  // ---- Empty cookie collection ----

  @Test
  public void secureRequest_noCookieHeaders_noHeadersWritten() throws Exception {
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(emptyList());

    cookieFilter.doFilter(request, response, filterChain);

    verify(response, times(0)).setHeader(anyString(), anyString());
    verify(response, times(0)).addHeader(anyString(), anyString());
  }

  // ---- Multiple cookies: first uses setHeader, rest use addHeader ----

  @Test
  public void secureRequest_multipleCookies_firstUsesSetHeaderRestUseAddHeader() throws Exception {
    String sessionCookie = SESSION_COOKIE_NAME + "=abc; Path=/; HttpOnly";
    String otherCookie = "other=val; Path=/";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(asList(sessionCookie, otherCookie));

    cookieFilter.doFilter(request, response, filterChain);

    verify(response, times(1)).setHeader(eq(SET_COOKIE), anyString());
    verify(response, times(1)).addHeader(eq(SET_COOKIE), anyString());
  }

  // ---- Context path stripping ----

  @Test
  public void secureRequest_withContextPath_stripsPrefixBeforePathCheck() throws Exception {
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/iq; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/iq/saml/login");
    when(request.getContextPath()).thenReturn("/iq");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("SameSite=None");
  }

  @Test
  public void secureRequest_withContextPath_nonSamlPath_getsSameSiteLax() throws Exception {
    // Symmetric counterpart: non-SAML request under a context path must strip the prefix and resolve to Lax
    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/iq; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/iq/rest/dashboard");
    when(request.getContextPath()).thenReturn("/iq");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("SameSite=Lax");
    assertThat(written).doesNotContainIgnoringCase("SameSite=None");
  }

  // ---- Compact cookie attributes (no spaces after semicolons) ----

  @Test
  public void secureRequest_compactCookieAttributes_preservesHttpOnly() throws Exception {
    // Edge case: compact format with no spaces after semicolons (still valid per RFC 6265)
    String sessionCookie = SESSION_COOKIE_NAME + "=abc;Path=/;HttpOnly;SameSite=Lax";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/user/session");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("HttpOnly"); // Must NOT be stripped along with SameSite
    assertThat(written).containsIgnoringCase("SameSite=Lax");
    assertThat(written).containsIgnoringCase("; Secure"); // ensureSecure must still append Secure
  }

  // ---- Cross-origin iframe embedding (frameAncestorsAllowlist non-empty) ----

  @Test
  public void iframeEmbeddingEnabled_nonSamlPath_sessionCookieGetsSameSiteNone() throws Exception {
    // When iframe embedding is enabled, IQ may be hosted in third-party iframes (e.g. Jenkins
    // plugin, IDE plugins). Cross-site iframe requests need SameSite=None so the session cookie is
    // sent.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);

    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/dashboard");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("SameSite=None");
    assertThat(written).doesNotContainIgnoringCase("SameSite=Lax");
  }

  @Test
  public void iframeEmbeddingEnabled_samlPath_sessionCookieStillGetsSameSiteNone() throws Exception {
    // Iframe embedding enabled and request is a SAML path: result is still None (same value,
    // different reason). Asserts no regression on SAML flow when iframe embedding is also enabled.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);

    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/saml/login");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("SameSite=None");
  }

  @Test
  public void iframeEmbeddingEnabled_replacesExistingSameSiteLaxWithNone() throws Exception {
    // Cookie already carries SameSite=Lax (set upstream by Shiro or similar). Iframe mode must
    // overwrite it with None, not append a second SameSite attribute.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);

    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly; SameSite=Lax";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/dashboard");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(countOccurrences(written.toLowerCase(), "samesite=")).isEqualTo(1);
    assertThat(written).containsIgnoringCase("SameSite=None");
  }

  @Test
  public void iframeEmbeddingDisabled_nonSamlPath_usesLax() throws Exception {
    // Explicit sanity check: when the detector reports iframe embedding disabled, non-SAML
    // paths fall back to SameSite=Lax. Covers the default happy path with an explicit stub.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(false);

    String sessionCookie = SESSION_COOKIE_NAME + "=abc123; Path=/; HttpOnly";
    when(request.isSecure()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/rest/dashboard");
    when(request.getContextPath()).thenReturn("");
    when(response.getHeaders(SET_COOKIE)).thenReturn(Collections.singletonList(sessionCookie));

    cookieFilter.doFilter(request, response, filterChain);

    String written = captureFirstSetHeader();
    assertThat(written).containsIgnoringCase("SameSite=Lax");
  }

  // ---- Helpers ----

  private String captureFirstSetHeader() {
    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(nameCaptor.capture(), valueCaptor.capture());
    assertThat(nameCaptor.getValue()).isEqualTo(SET_COOKIE);
    return valueCaptor.getValue();
  }

  private static int countOccurrences(String text, String target) {
    int count = 0;
    int idx = 0;
    while ((idx = text.indexOf(target, idx)) >= 0) {
      count++;
      idx += target.length();
    }
    return count;
  }
}
