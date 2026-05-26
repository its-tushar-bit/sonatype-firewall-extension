/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.service.consumption.ConsumptionContextFilter;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.spring.config.FilterOrder;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MtiqMainFilterConfiguration
{

  @Bean
  public FilterRegistrationBean<ActiveRequestCounterFilter> mtiqMainActiveRequestCounterFilterRegistration(
      @Qualifier("activeRequestCounterFilter") ActiveRequestCounterFilter activeRequestCounterFilter)
  {
    return registerFilter(activeRequestCounterFilter, FilterOrder.ACTIVE_REQUEST_COUNTER, "/*");
  }

  @Bean
  public FilterRegistrationBean<ConsumptionContextFilter> mtiqMainConsumptionContextFilterRegistration(
      ConsumptionContextFilter consumptionContextFilter)
  {
    return registerFilter(consumptionContextFilter, FilterOrder.CONSUMPTION_CONTEXT, "/*");
  }

  @Bean
  public FilterRegistrationBean<TenantUrlFilter> mtiqMainTenantUrlFilterRegistration(
      TenantUrlFilter tenantUrlFilter)
  {
    return registerFilter(tenantUrlFilter, FilterOrder.TENANT_URL, "/*");
  }

  @Bean
  public FilterRegistrationBean<MultiTenantServerHeaderFilter> mtiqMainServerHeaderFilterRegistration(
      MultiTenantServerHeaderFilter multiTenantServerHeaderFilter)
  {
    return registerFilter(multiTenantServerHeaderFilter, FilterOrder.SERVER_HEADER, ServerHeaderFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<PlatformContextFilter> mtiqMainPlatformContextFilterRegistration(
      PlatformContextFilter platformContextFilter)
  {
    return registerFilter(platformContextFilter, FilterOrder.PLATFORM_CONTEXT, PlatformContextFilter.URL_PATTERNS);
  }

  private <T extends Filter> FilterRegistrationBean<T> registerFilter(T filter, int order, String... urlPatterns) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns(urlPatterns);
    registration.setOrder(order);
    return registration;
  }

}
