/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.Configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests proving CLM-CSRF-TOKEN cookie policy without starting a full server.
 */
public class AntiCsrfCookiePolicyTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private Configuration configuration;

  @Mock
  private FrameEmbeddingDetector frameEmbeddingDetector;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private AntiCsrfFilter filter;

  @Before
  public void setUp() {
    filter = new AntiCsrfFilter(configuration, frameEmbeddingDetector);
    filter.processPathConfig("/test", "");
  }

  private void configureFilterEnabled() {
    when(configuration.isAntiCsrfEnabled()).thenReturn(true);
  }

  @Test
  public void httpsRequest_csrfCookieHasSameSiteLax() throws Exception {
    configureFilterEnabled();
    when(request.isSecure()).thenReturn(true);
    when(request.getMethod()).thenReturn("GET");
    when(request.getCookies()).thenReturn(null);
    when(request.getServletPath()).thenReturn("/test");

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = captureSetCookieHeader();
    assertThat(setCookieHeader).contains("CLM-CSRF-TOKEN");
    assertThat(setCookieHeader).containsIgnoringCase("SameSite=Lax");
    assertThat(setCookieHeader).doesNotContainIgnoringCase("SameSite=None");
    assertThat(setCookieHeader).containsIgnoringCase("Secure");
  }

  @Test
  public void httpRequest_csrfCookieHasNoSecureFlag() throws Exception {
    configureFilterEnabled();
    when(request.isSecure()).thenReturn(false);
    when(request.getMethod()).thenReturn("GET");
    when(request.getCookies()).thenReturn(null);
    when(request.getServletPath()).thenReturn("/test");

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = captureSetCookieHeader();
    assertThat(setCookieHeader).contains("CLM-CSRF-TOKEN");
    assertThat(setCookieHeader).doesNotContainIgnoringCase("; Secure");
  }

  @Test
  public void iframeEmbeddingEnabled_csrfCookieHasSameSiteNone() throws Exception {
    // When iframe embedding is enabled, CSRF cookie must be SameSite=None so it flows on
    // cross-site iframe requests from embedding tools (Jenkins, IDE, etc.).
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);
    configureFilterEnabled();
    when(request.isSecure()).thenReturn(true);
    when(request.getMethod()).thenReturn("GET");
    when(request.getCookies()).thenReturn(null);
    when(request.getServletPath()).thenReturn("/test");

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = captureSetCookieHeader();
    assertThat(setCookieHeader).contains("CLM-CSRF-TOKEN");
    assertThat(setCookieHeader).containsIgnoringCase("SameSite=None");
    assertThat(setCookieHeader).doesNotContainIgnoringCase("SameSite=Lax");
  }

  @Test
  public void iframeEmbeddingDisabled_csrfCookieKeepsLax() throws Exception {
    // Default: iframe embedding disabled (detector returns false). Sanity check that the filter
    // emits SameSite=Lax in that default state.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(false);
    configureFilterEnabled();
    when(request.isSecure()).thenReturn(true);
    when(request.getMethod()).thenReturn("GET");
    when(request.getCookies()).thenReturn(null);
    when(request.getServletPath()).thenReturn("/test");

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = captureSetCookieHeader();
    assertThat(setCookieHeader).containsIgnoringCase("SameSite=Lax");
  }

  @Test
  public void iframeEmbeddingEnabled_httpRequest_csrfCookieFallsBackToLax() throws Exception {
    // SameSite=None requires the Secure attribute (Chrome 80+, Firefox 79+). On plain HTTP we cannot
    // set Secure, so even when iframe embedding is enabled we fall back to Lax to avoid the browser
    // silently discarding the cookie.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);
    configureFilterEnabled();
    when(request.isSecure()).thenReturn(false);
    when(request.getMethod()).thenReturn("GET");
    when(request.getCookies()).thenReturn(null);
    when(request.getServletPath()).thenReturn("/test");

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = captureSetCookieHeader();
    assertThat(setCookieHeader).containsIgnoringCase("SameSite=Lax");
    assertThat(setCookieHeader).doesNotContainIgnoringCase("SameSite=None");
  }

  private String captureSetCookieHeader() {
    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    verify(response, atLeastOnce()).addHeader(nameCaptor.capture(), valueCaptor.capture());

    List<String> names = nameCaptor.getAllValues();
    List<String> values = valueCaptor.getAllValues();
    for (int i = 0; i < names.size(); i++) {
      if ("Set-Cookie".equalsIgnoreCase(names.get(i)) && values.get(i).contains("CLM-CSRF-TOKEN")) {
        return values.get(i);
      }
    }
    throw new AssertionError("No Set-Cookie header for CLM-CSRF-TOKEN was written to the response");
  }
}
