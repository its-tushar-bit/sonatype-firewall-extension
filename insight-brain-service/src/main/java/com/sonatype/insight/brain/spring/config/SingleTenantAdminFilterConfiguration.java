/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.FrameOptionsHeaderFilter;
import com.sonatype.insight.brain.security.HstsHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.service.BaseUrlFilter;
import com.sonatype.insight.brain.service.ServerHeaderFilter;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnProperty(name = "sonatype.mtiq.enabled", havingValue = "false", matchIfMissing = true)
public class SingleTenantAdminFilterConfiguration
{
  private static final int ORDER_ACTIVE_REQUEST_COUNTER = 10;

  private static final int ORDER_SERVER_HEADER = 20;

  private static final int ORDER_BASE_URL = 30;

  private static final int ORDER_AUDIT = 40;

  private static final int ORDER_HTTP_HEADER_VALIDATION = 45;

  private static final int ORDER_CONTENT_TYPE_OPTIONS = 46;

  private static final int ORDER_HSTS = 47;

  private static final int ORDER_FRAME_OPTIONS = 48;

  @Bean
  public FilterRegistrationBean<ActiveRequestCounterFilter> adminActiveRequestCounterFilterRegistration(
      ActiveRequestCounterFilter activeRequestCounterFilter)
  {
    return registerFilter(activeRequestCounterFilter, ORDER_ACTIVE_REQUEST_COUNTER, "/*");
  }

  @Bean
  public FilterRegistrationBean<ServerHeaderFilter> adminServerHeaderFilterRegistration(
      ServerHeaderFilter serverHeaderFilter)
  {
    return registerFilter(serverHeaderFilter, ORDER_SERVER_HEADER, ServerHeaderFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<BaseUrlFilter> adminBaseUrlFilterRegistration(BaseUrlFilter baseUrlFilter) {
    return registerFilter(baseUrlFilter, ORDER_BASE_URL, "/*");
  }

  @Bean
  public FilterRegistrationBean<AuditFilter> adminAuditFilterRegistration(AuditFilter auditFilter) {
    return registerFilter(auditFilter, ORDER_AUDIT, AuditFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<HttpHeaderValidatorFilter> adminHttpHeaderValidatorFilterRegistration(
      HttpHeaderValidatorFilter httpHeaderValidatorFilter)
  {
    return registerFilter(httpHeaderValidatorFilter, ORDER_HTTP_HEADER_VALIDATION,
        HttpHeaderValidatorFilter.URL_PATTERN);
  }

  @Bean
  public FilterRegistrationBean<ContentTypeOptionsHeaderFilter> adminContentTypeOptionsHeaderFilterRegistration(
      ContentTypeOptionsHeaderFilter contentTypeOptionsHeaderFilter)
  {
    return registerFilter(contentTypeOptionsHeaderFilter, ORDER_CONTENT_TYPE_OPTIONS, "/*");
  }

  @Bean
  public FilterRegistrationBean<HstsHeaderFilter> adminHstsHeaderFilterRegistration(
      HstsHeaderFilter hstsHeaderFilter)
  {
    return registerFilter(hstsHeaderFilter, ORDER_HSTS, "/*");
  }

  @Bean
  public FilterRegistrationBean<FrameOptionsHeaderFilter> adminFrameOptionsHeaderFilterRegistration(
      FrameOptionsHeaderFilter frameOptionsHeaderFilter)
  {
    return registerFilter(frameOptionsHeaderFilter, ORDER_FRAME_OPTIONS, "/*");
  }

  private <T extends Filter> FilterRegistrationBean<T> registerFilter(T filter, int order, String... urlPatterns) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns(urlPatterns);
    registration.setOrder(order);
    return registration;
  }
}
