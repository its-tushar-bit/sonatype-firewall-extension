/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import org.junit.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class MtiqMainFilterConfigurationTest
{
  @Test
  public void shouldRelyOnSharedHstsFilterInsteadOfRegisteringAnotherMainFilter() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestFilterBeans.class, MtiqMainFilterConfiguration.class);
      context.refresh();

      FilterRegistrationBean<?> activeRequestCounterFilter =
          context.getBean("mtiqMainActiveRequestCounterFilterRegistration", FilterRegistrationBean.class);
      FilterRegistrationBean<?> tenantUrlFilter =
          context.getBean("mtiqMainTenantUrlFilterRegistration", FilterRegistrationBean.class);
      FilterRegistrationBean<?> serverHeaderFilter =
          context.getBean("mtiqMainServerHeaderFilterRegistration", FilterRegistrationBean.class);
      FilterRegistrationBean<?> platformContextFilter =
          context.getBean("mtiqMainPlatformContextFilterRegistration", FilterRegistrationBean.class);

      assertThat(activeRequestCounterFilter.getOrder()).isLessThan(tenantUrlFilter.getOrder());
      assertThat(tenantUrlFilter.getOrder()).isLessThan(serverHeaderFilter.getOrder());
      assertThat(serverHeaderFilter.getOrder()).isLessThan(platformContextFilter.getOrder());
      assertThat(platformContextFilter.getUrlPatterns()).containsExactly(PlatformContextFilter.URL_PATTERNS);
      assertThat(platformContextFilter.getOrder()).isLessThan(50);
      assertThat(platformContextFilter.getOrder()).isLessThan(100);
      assertThat(platformContextFilter.getUrlPatterns()).doesNotContain("/*");
      assertThat(context.containsBean("mtiqMainHstsHeaderFilterRegistration")).isFalse();
    }
  }

  @Configuration
  static class TestFilterBeans
  {
    @Bean
    ActiveRequestCounterFilter activeRequestCounterFilter() {
      return mock(ActiveRequestCounterFilter.class);
    }

    @Bean
    AdminTenantFilter adminTenantFilter() {
      return mock(AdminTenantFilter.class);
    }

    @Bean
    AdminTasksTenantFilter adminTasksTenantFilter() {
      return mock(AdminTasksTenantFilter.class);
    }

    @Bean
    JwtHttpAuthorizationFilter jwtHttpAuthorizationFilter() {
      return mock(JwtHttpAuthorizationFilter.class);
    }

    @Bean
    TenantUrlFilter tenantUrlFilter() {
      return mock(TenantUrlFilter.class);
    }

    @Bean
    MultiTenantServerHeaderFilter multiTenantServerHeaderFilter() {
      return mock(MultiTenantServerHeaderFilter.class);
    }

    @Bean
    PlatformContextFilter platformContextFilter() {
      return mock(PlatformContextFilter.class);
    }
  }
}
