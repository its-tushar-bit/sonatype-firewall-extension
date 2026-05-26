/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.FrameOptionsHeaderFilter;
import com.sonatype.insight.brain.security.HstsHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.service.BaseUrlFilter;
import com.sonatype.insight.brain.service.ServerHeaderFilter;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import org.junit.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class SingleTenantAdminFilterConfigurationTest
{
  @Test
  public void shouldRegisterAdminSecurityHeaderFiltersWithoutLegacyWebCompatibilityFilters() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestAdminFilterBeans.class, SingleTenantAdminFilterConfiguration.class);
      context.refresh();

      FilterRegistrationBean<?> contentTypeOptionsRegistration = context.getBean(
          "adminContentTypeOptionsHeaderFilterRegistration",
          FilterRegistrationBean.class);
      FilterRegistrationBean<?> hstsRegistration = context.getBean(
          "adminHstsHeaderFilterRegistration",
          FilterRegistrationBean.class);
      FilterRegistrationBean<?> frameOptionsRegistration = context.getBean(
          "adminFrameOptionsHeaderFilterRegistration",
          FilterRegistrationBean.class);

      assertThat(contentTypeOptionsRegistration.getUrlPatterns()).containsExactly("/*");
      assertThat(hstsRegistration.getUrlPatterns()).containsExactly("/*");
      assertThat(frameOptionsRegistration.getUrlPatterns()).containsExactly("/*");
      assertThat(contentTypeOptionsRegistration.getOrder()).isLessThan(hstsRegistration.getOrder());
      assertThat(hstsRegistration.getOrder()).isLessThan(frameOptionsRegistration.getOrder());
      assertThat(context.containsBean("adminShiroFilterRegistration")).isFalse();
      assertThat(context.containsBean("adminLegacyWebHeaderFilterRegistration")).isFalse();
      assertThat(context.containsBean("adminLegacyWebCorsFilterRegistration")).isFalse();
    }
  }

  @Configuration
  static class TestAdminFilterBeans
  {
    @Bean
    ActiveRequestCounterFilter activeRequestCounterFilter() {
      return mock(ActiveRequestCounterFilter.class);
    }

    @Bean
    ServerHeaderFilter serverHeaderFilter() {
      return mock(ServerHeaderFilter.class);
    }

    @Bean
    BaseUrlFilter baseUrlFilter() {
      return mock(BaseUrlFilter.class);
    }

    @Bean
    AuditFilter auditFilter() {
      return mock(AuditFilter.class);
    }

    @Bean
    HttpHeaderValidatorFilter httpHeaderValidatorFilter() {
      return mock(HttpHeaderValidatorFilter.class);
    }

    @Bean
    ContentTypeOptionsHeaderFilter contentTypeOptionsHeaderFilter() {
      return mock(ContentTypeOptionsHeaderFilter.class);
    }

    @Bean
    HstsHeaderFilter hstsHeaderFilter() {
      return mock(HstsHeaderFilter.class);
    }

    @Bean
    FrameOptionsHeaderFilter frameOptionsHeaderFilter() {
      return mock(FrameOptionsHeaderFilter.class);
    }
  }
}
