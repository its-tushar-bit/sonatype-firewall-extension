/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.service.ServerHeaderFilter;
import com.sonatype.insight.brain.service.consumption.ConsumptionContextFilter;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SingleTenantMainFilterConfiguration
{

  @Bean
  public FilterRegistrationBean<ActiveRequestCounterFilter> activeRequestCounterFilterRegistration(
      ActiveRequestCounterFilter activeRequestCounterFilter)
  {
    return registerFilter(activeRequestCounterFilter, FilterOrder.ACTIVE_REQUEST_COUNTER, "/*");
  }

  @Bean
  public FilterRegistrationBean<ConsumptionContextFilter> consumptionContextFilterRegistration(
      ConsumptionContextFilter consumptionContextFilter)
  {
    return registerFilter(consumptionContextFilter, FilterOrder.CONSUMPTION_CONTEXT, "/*");
  }

  @Bean
  public FilterRegistrationBean<ServerHeaderFilter> serverHeaderFilterRegistration(
      ServerHeaderFilter serverHeaderFilter)
  {
    return registerFilter(serverHeaderFilter, FilterOrder.SERVER_HEADER, ServerHeaderFilter.URL_PATTERNS);
  }

  private <T extends Filter> FilterRegistrationBean<T> registerFilter(T filter, int order, String... urlPatterns) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns(urlPatterns);
    registration.setOrder(order);
    return registration;
  }
}
