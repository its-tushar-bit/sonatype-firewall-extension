/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.security.oauth2.JwtAuthenticationFilter;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disables auto-registration of Shiro-managed filters.
 *
 * <p>
 * Spring Boot automatically registers any bean that implements {@link Filter}
 * as a servlet filter. This causes problems for filters managed by Shiro's
 * {@link org.apache.shiro.web.filter.mgt.FilterChainManager} because they would execute
 * OUTSIDE of Shiro's filter chain management.
 *
 * <p>
 * This configuration creates {@link FilterRegistrationBean} beans with {@code enabled=false}
 * for each Shiro-managed filter to prevent Spring Boot's auto-registration.
 *
 * @since 1.203
 */
@Configuration
public class ShiroFilterRegistrationDisabler
{
  @Bean
  public FilterRegistrationBean<AntiCsrfFilter> disableAntiCsrfFilter(AntiCsrfFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<ClientIPAddressFilter> disableClientIPAddressFilter(ClientIPAddressFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<SecureCookiesFilter> disableSecureCookiesFilter(SecureCookiesFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<SessionExpirationCookieFilter> disableSessionExpirationCookieFilter(
      SessionExpirationCookieFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<InvalidRequestFilter> disableInvalidRequestFilter(InvalidRequestFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<MissingAuthenticationFilter> disableMissingAuthenticationFilter(
      MissingAuthenticationFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<SamlFilter> disableSamlFilter(SamlFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<ReverseProxyAuthenticationFilter> disableReverseProxyAuthenticationFilter(
      ReverseProxyAuthenticationFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<UserFriendlyBasicHttpAuthenticationFilter> disableUserFriendlyBasicHttpAuthenticationFilter(
      UserFriendlyBasicHttpAuthenticationFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<ApiAccessControlFilter> disableApiAccessControlFilter(ApiAccessControlFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<ContentTypeOptionsHeaderFilter> disableContentTypeOptionsHeaderFilter(
      ContentTypeOptionsHeaderFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<CspHeaderFilter> disableCspHeaderFilter(CspHeaderFilter filter) {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<AuthenticationLoggingFilter> disableAuthenticationLoggingFilter(
      AuthenticationLoggingFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<HttpHeaderValidatorFilter> disableHttpHeaderValidatorFilter(
      HttpHeaderValidatorFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtAuthenticationFilter(
      JwtAuthenticationFilter filter)
  {
    return disableAutoRegistration(filter);
  }

  @Bean
  public FilterRegistrationBean<OidcLoginFilter> disableOidcLoginFilter(OidcLoginFilter filter) {
    return disableAutoRegistration(filter);
  }

  private static <T extends Filter> FilterRegistrationBean<T> disableAutoRegistration(T filter) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
