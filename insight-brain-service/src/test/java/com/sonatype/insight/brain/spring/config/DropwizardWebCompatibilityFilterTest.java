/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.filter.ThrowableHandler;
import com.sonatype.insight.brain.firewall.FirewallRedirectFilter;
import com.sonatype.insight.brain.landing.IndexCacheControlFilter;
import com.sonatype.insight.brain.landing.NexusOneIndexAccessFilter;
import com.sonatype.insight.brain.landing.StaticAssetCacheControlFilter;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.CspHeaderFilter;
import com.sonatype.insight.brain.security.FrameOptionsHeaderFilter;
import com.sonatype.insight.brain.security.HstsHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.security.McpLicenseFilter;
import com.sonatype.insight.brain.service.BaseUrlFilter;
import com.sonatype.insight.brain.service.CspFrameHeaderFilter;

import jakarta.servlet.FilterChain;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DropwizardWebCompatibilityFilterTest
{
  @Test
  public void shouldRegisterLegacyWebFiltersOnConfiguredUriPath() throws Exception {
    DropwizardWebSettings webSettings = DropwizardWebSettings.of(
        "/iq",
        Map.of("X-Legacy", "value"),
        corsSettings(false));
    FilterConfiguration filterConfiguration = filterConfiguration();

    FilterRegistrationBean<LegacyWebCorsFilter> corsRegistration = filterConfiguration
        .legacyWebCorsFilterRegistration(providerFor(webSettings));
    FilterRegistrationBean<LegacyWebHeaderFilter> headerRegistration = filterConfiguration
        .legacyWebHeaderFilterRegistration(providerFor(webSettings));

    assertThat(corsRegistration.isEnabled()).isTrue();
    assertThat(corsRegistration.getUrlPatterns()).containsExactly("/iq/*");
    assertThat(corsRegistration.getOrder()).isLessThan(filterConfiguration.httpHeaderValidatorFilterRegistration()
        .getOrder());

    assertThat(headerRegistration.isEnabled()).isTrue();
    assertThat(headerRegistration.getUrlPatterns()).containsExactly("/iq/*");
    assertThat(headerRegistration.getOrder()).isGreaterThan(filterConfiguration.cspFrameHeaderFilterRegistration()
        .getOrder());
    assertThat(headerRegistration.getOrder()).isLessThan(filterConfiguration.firewallRedirectFilterRegistration()
        .getOrder());
  }

  @Test
  public void shouldDisableLegacyWebFiltersWhenNoLegacySettingsAreConfigured() throws Exception {
    FilterConfiguration filterConfiguration = filterConfiguration();

    FilterRegistrationBean<LegacyWebCorsFilter> corsRegistration = filterConfiguration
        .legacyWebCorsFilterRegistration(providerFor(DropwizardWebSettings.empty()));
    FilterRegistrationBean<LegacyWebHeaderFilter> headerRegistration = filterConfiguration
        .legacyWebHeaderFilterRegistration(providerFor(DropwizardWebSettings.empty()));

    assertThat(corsRegistration.isEnabled()).isFalse();
    assertThat(headerRegistration.isEnabled()).isFalse();
  }

  @Test
  public void shouldEmitLegacyHeadersBeforeContinuingChain() throws Exception {
    DropwizardWebSettings webSettings = DropwizardWebSettings.of(
        "/",
        Map.of("X-Legacy", "value", LegacyWebHeaderFilter.X_XSS_PROTECTION, "1; mode=block"),
        null);
    LegacyWebHeaderFilter filter = new LegacyWebHeaderFilter(webSettings);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("X-Legacy")).isEqualTo("value");
    assertThat(response.getHeader(LegacyWebHeaderFilter.X_XSS_PROTECTION)).isEqualTo("1; mode=block");
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldApplyLegacyCorsPreflightAndStopWhenChainPreflightIsDisabled() throws Exception {
    LegacyWebCorsFilter filter = new LegacyWebCorsFilter(corsSettings(false));
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v2/applications");
    request.addHeader(LegacyWebCorsFilter.ORIGIN, "https://app.example.com");
    request.addHeader(LegacyWebCorsFilter.ACCESS_CONTROL_REQUEST_METHOD, "POST");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(LegacyWebCorsFilter.VARY)).isEqualTo(LegacyWebCorsFilter.ORIGIN);
    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo("https://app.example.com");
    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET,POST,OPTIONS");
    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_ALLOW_HEADERS))
        .isEqualTo("Authorization,Content-Type");
    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_MAX_AGE)).isEqualTo("1800");
    verify(chain, never()).doFilter(request, response);
  }

  @Test
  public void shouldApplyLegacyCorsSimpleResponseAndTimingOrigin() throws Exception {
    LegacyWebCorsFilter filter = new LegacyWebCorsFilter(corsSettings(true));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/applications");
    request.addHeader(LegacyWebCorsFilter.ORIGIN, "https://app.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo("https://app.example.com");
    assertThat(response.getHeader(LegacyWebCorsFilter.ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("X-Result");
    assertThat(response.getHeader(LegacyWebCorsFilter.TIMING_ALLOW_ORIGIN)).isEqualTo("https://app.example.com");
    verify(chain).doFilter(request, response);
  }

  private DropwizardWebSettings.CorsSettings corsSettings(boolean chainPreflight) {
    return DropwizardWebSettings.CorsSettings.of(
        List.of("https://app.example.com"),
        List.of("https://app.example.com"),
        List.of("GET", "POST", "OPTIONS"),
        List.of("Authorization", "Content-Type"),
        Duration.ofMinutes(30),
        true,
        List.of("X-Result"),
        chainPreflight);
  }

  private ObjectProvider<DropwizardWebSettings> providerFor(DropwizardWebSettings webSettings) {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.registerSingleton("dropwizardWebSettings", webSettings);
    return beanFactory.getBeanProvider(DropwizardWebSettings.class);
  }

  private FilterConfiguration filterConfiguration() throws Exception {
    return new FilterConfiguration(
        mock(ThrowableHandler.class),
        mock(BaseUrlFilter.class),
        mock(AuditFilter.class),
        mock(HttpHeaderValidatorFilter.class),
        mock(ContentTypeOptionsHeaderFilter.class),
        mock(HstsHeaderFilter.class),
        mock(FrameOptionsHeaderFilter.class),
        mock(McpLicenseFilter.class),
        mock(IndexCacheControlFilter.class),
        mock(NexusOneIndexAccessFilter.class),
        mock(StaticAssetCacheControlFilter.class),
        mock(AuthenticationLoggingFilter.class),
        mock(CspHeaderFilter.class),
        mock(CspFrameHeaderFilter.class),
        mock(FirewallRedirectFilter.class));
  }
}
