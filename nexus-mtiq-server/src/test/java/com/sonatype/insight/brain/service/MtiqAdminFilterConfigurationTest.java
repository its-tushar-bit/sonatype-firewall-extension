/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkAuth0Provider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkLocalProvider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.api.admin.service.MultiTenantActiveRequestCounterFilter;
import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.backup.DbBackupTask;
import com.sonatype.insight.brain.filter.ThrowableHandler;
import com.sonatype.insight.brain.operational.check.ClusterDirectoryAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ExistingDbConnectionAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.NewDbConnectionAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ThreadDeadlockAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.WorkDirectoryAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.policy.waiver.WaiverExpirationDetectionTask;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.FrameOptionsHeaderFilter;
import com.sonatype.insight.brain.security.HstsHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.shutdown.ShutdownTask;
import com.sonatype.insight.brain.spring.config.SingleTenantAdminFilterConfiguration;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.lang.reflect.Field;
import java.util.Map;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

public class MtiqAdminFilterConfigurationTest
{
  @Test
  public void shouldRegisterAdminFiltersWithLegacyOrderAndScope() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestFilterBeans.class, MtiqAdminFilterConfiguration.class);
      context.refresh();

      FilterRegistrationBean<?> tenantFilter =
          context.getBean("mtiqAdminTenantFilterRegistration", FilterRegistrationBean.class);
      FilterRegistrationBean<?> tasksFilter =
          context.getBean("mtiqAdminTasksTenantFilterRegistration", FilterRegistrationBean.class);
      FilterRegistrationBean<?> authorizationFilter =
          context.getBean("mtiqAdminAuthorizationFilterRegistration", FilterRegistrationBean.class);

      assertThat(tenantFilter.getOrder()).isLessThan(tasksFilter.getOrder());
      assertThat(tasksFilter.getOrder()).isLessThan(authorizationFilter.getOrder());

      assertThat(tenantFilter.getUrlPatterns())
          .containsExactly(MtiqAdminJerseyConfiguration.ADMIN_API_SERVLET_PATH);
      assertThat(tasksFilter.getUrlPatterns())
          .containsExactlyInAnyOrder("/api/admin/tenants/*", "/tasks/*");
      assertThat(authorizationFilter.getUrlPatterns())
          .containsExactly(MtiqAdminJerseyConfiguration.ADMIN_API_SERVLET_PATH);
    }
  }

  @Test
  public void shouldExposeAdminResourceConfigUnderMtiqBeanName() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestAdminJerseyBeans.class, MtiqAdminJerseyConfiguration.class);
      context.refresh();

      assertThat(context.containsBean("mtiqAdminResourceConfig")).isTrue();
      assertThat(context.containsBean("resourceConfig")).isFalse();

      ResourceConfig adminResourceConfig = context.getBean("mtiqAdminResourceConfig", ResourceConfig.class);
      ServletRegistrationBean<?> registration =
          context.getBean("mtiqAdminJerseyServlet", ServletRegistrationBean.class);

      assertThat(registration.getUrlMappings()).containsExactly(MtiqAdminJerseyConfiguration.ADMIN_API_SERVLET_PATH);
      assertThat(registration.getServlet()).isInstanceOf(ServletContainer.class);
      assertThat(extractResourceConfig((ServletContainer) registration.getServlet())).isSameAs(adminResourceConfig);
    }
  }

  @Test
  public void shouldResolveAdminTaskBeansFromParentManagementContext() {
    try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext())
    {
      parent.register(TestAdminJerseyBeans.class);
      parent.refresh();

      child.setParent(parent);
      child.register(MtiqAdminJerseyConfiguration.class);
      child.refresh();

      assertThat(child.containsLocalBean("waiverExpirationDetectionTask")).isFalse();
      assertThat(child.getBean(WaiverExpirationDetectionTask.class))
          .isSameAs(parent.getBean(WaiverExpirationDetectionTask.class));
    }
  }

  @Test
  public void shouldCreateThrowableHandlerFromConfiguredExceptionMapperWhenNoBeanExists() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestFilterBeansWithoutThrowableHandler.class, MtiqAdminFilterConfiguration.class);
      context.refresh();

      FilterRegistrationBean<?> throwableHandlerRegistration =
          context.getBean("mtiqAdminThrowableHandlerRegistration", FilterRegistrationBean.class);
      ThrowableHandler throwableHandler = (ThrowableHandler) throwableHandlerRegistration.getFilter();

      assertThat(throwableHandlerRegistration.getFilter()).isInstanceOf(ThrowableHandler.class);
      assertThat(ReflectionTestUtils.getField(throwableHandler, "jaxRsExceptionMapper"))
          .isSameAs(context.getBean(MtiqAdminJaxRsErrorConfiguration.CONFIGURED_JAX_RS_EXCEPTION_MAPPER_BEAN_NAME));
    }
  }

  @Test
  public void shouldCreateAuthorizationFilterFromConfiguredJwkProvidersWhenNoBeanExists() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestFilterBeansWithoutJwtHttpAuthorizationFilter.class, MtiqAdminFilterConfiguration.class);
      context.refresh();

      FilterRegistrationBean<?> authorizationFilterRegistration =
          context.getBean("mtiqAdminAuthorizationFilterRegistration", FilterRegistrationBean.class);

      assertThat(authorizationFilterRegistration.getFilter()).isInstanceOf(JwtHttpAuthorizationFilter.class);
    }
  }

  @Test
  public void shouldSelectAuth0JwkProviderForRemoteDomain() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestFilterBeansWithRemoteAuth0Domain.class, MtiqAdminFilterConfiguration.class);
      context.refresh();

      assertThat(context.getBean(MultiTenantJwkProvider.class))
          .isSameAs(context.getBean(MultiTenantJwkAuth0Provider.class));
    }
  }

  @Test
  public void shouldFailWithActionableErrorWhenSingleTenantConfigIsUsedForMtiqAuthFilter() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestFilterBeansWithWrongInsightConfig.class, MtiqAdminFilterConfiguration.class);

      assertThatThrownBy(context::refresh)
          .rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("MtiqAdminFilterConfiguration")
          .hasMessageContaining(MultiTenantInsightConfig.class.getName())
          .hasMessageContaining(InsightConfig.class.getName())
          .hasMessageContaining("config.class=" + MultiTenantInsightConfig.class.getName());
    }
  }

  @Test
  public void shouldReuseAncestorAuthorizationFilterWhenAvailable() {
    try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext())
    {
      parent.register(ParentAuthorizationFilterBeans.class);
      parent.refresh();

      child.setParent(parent);
      child.register(TestFilterBeansWithoutJwtHttpAuthorizationFilter.class, MtiqAdminFilterConfiguration.class);
      child.refresh();

      FilterRegistrationBean<?> authorizationFilterRegistration =
          child.getBean("mtiqAdminAuthorizationFilterRegistration", FilterRegistrationBean.class);

      assertThat(authorizationFilterRegistration.getFilter())
          .isSameAs(parent.getBean(JwtHttpAuthorizationFilter.class));
    }
  }

  @Test
  public void shouldNotRegisterSingleTenantAdminFiltersWhenMtiqModeIsEnabled() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment()
          .getPropertySources()
          .addFirst(new MapPropertySource("test", Map.of("sonatype.mtiq.enabled", "true")));
      context.register(SingleTenantAdminFilterConfiguration.class);
      context.refresh();

      assertThat(context.containsBean("adminActiveRequestCounterFilterRegistration")).isFalse();
      assertThat(context.containsBean("adminShiroFilterRegistration")).isFalse();
    }
  }

  private ResourceConfig extractResourceConfig(ServletContainer container) {
    try {
      Field field = ServletContainer.class.getDeclaredField("resourceConfig");
      field.setAccessible(true);
      return (ResourceConfig) field.get(container);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  @Configuration
  static class TestAdminJerseyBeans
  {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    MetricRegistry metricRegistry() {
      return new MetricRegistry();
    }

    @Bean
    HealthCheckRegistry healthCheckRegistry() {
      return new HealthCheckRegistry();
    }

    @Bean
    WaiverExpirationDetectionTask waiverExpirationDetectionTask() {
      WaiverExpirationDetectionTask task = mock(WaiverExpirationDetectionTask.class);
      when(task.getPath()).thenReturn(WaiverExpirationDetectionTask.PATH);
      return task;
    }

    @Bean
    ExistingDbConnectionAdminHealthCheckEndpoint existingDbConnectionAdminHealthCheckEndpoint() {
      return mock(ExistingDbConnectionAdminHealthCheckEndpoint.class);
    }

    @Bean
    NewDbConnectionAdminHealthCheckEndpoint newDbConnectionAdminHealthCheckEndpoint() {
      return mock(NewDbConnectionAdminHealthCheckEndpoint.class);
    }

    @Bean
    ThreadDeadlockAdminHealthCheckEndpoint threadDeadlockAdminHealthCheckEndpoint() {
      return mock(ThreadDeadlockAdminHealthCheckEndpoint.class);
    }

    @Bean
    WorkDirectoryAdminHealthCheckEndpoint workDirectoryAdminHealthCheckEndpoint() {
      return mock(WorkDirectoryAdminHealthCheckEndpoint.class);
    }

    @Bean
    ClusterDirectoryAdminHealthCheckEndpoint clusterDirectoryAdminHealthCheckEndpoint() {
      return mock(ClusterDirectoryAdminHealthCheckEndpoint.class);
    }

    @Bean
    ShutdownTask shutdownTask() {
      ShutdownTask task = mock(ShutdownTask.class);
      when(task.getPath()).thenReturn(ShutdownTask.PATH);
      return task;
    }

    @Bean
    CopyStorageTask copyStorageTask() {
      CopyStorageTask task = mock(CopyStorageTask.class);
      when(task.getPath()).thenReturn(CopyStorageTask.PATH);
      return task;
    }

    @Bean
    PopulateSearchIndexTask populateSearchIndexTask() {
      PopulateSearchIndexTask task = mock(PopulateSearchIndexTask.class);
      when(task.getPath()).thenReturn(PopulateSearchIndexTask.PATH);
      return task;
    }

    @Bean
    DbBackupTask dbBackupTask() {
      return mock(DbBackupTask.class);
    }
  }

  @Configuration
  abstract static class BaseFilterBeans
  {
    protected MultiTenantInsightConfig createMultiTenantInsightConfig(String domain) {
      MultiTenantInsightConfig config = mock(MultiTenantInsightConfig.class);
      Auth0Config auth0Config = new Auth0Config();
      auth0Config.setDomain(domain);
      Mockito.when(config.getAuth0Config()).thenReturn(auth0Config);
      return config;
    }

    @Bean
    MultiTenantJwkAuth0Provider multiTenantJwkAuth0Provider() {
      return mock(MultiTenantJwkAuth0Provider.class);
    }

    @Bean
    MultiTenantJwkLocalProvider multiTenantJwkLocalProvider() {
      return mock(MultiTenantJwkLocalProvider.class);
    }

    @Bean(name = "mtiqAdminJaxRsExceptionMapper")
    JaxRsExceptionMapper jaxRsExceptionMapper() {
      return mock(JaxRsExceptionMapper.class);
    }

    @Bean
    MultiTenantActiveRequestCounterFilter activeRequestCounterFilter() {
      return mock(MultiTenantActiveRequestCounterFilter.class);
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
    AdminTenantFilter adminTenantFilter() {
      return mock(AdminTenantFilter.class);
    }

    @Bean
    AdminTasksTenantFilter adminTasksTenantFilter() {
      return mock(AdminTasksTenantFilter.class);
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

  @Configuration
  static class TestFilterBeans
      extends BaseFilterBeans
  {
    @Bean
    InsightConfig insightConfig() {
      return createMultiTenantInsightConfig("local/");
    }

    @Bean
    ThrowableHandler throwableHandler() {
      return mock(ThrowableHandler.class);
    }

    @Bean
    JwtHttpAuthorizationFilter jwtHttpAuthorizationFilter() {
      return mock(JwtHttpAuthorizationFilter.class);
    }
  }

  @Configuration
  static class TestFilterBeansWithoutThrowableHandler
      extends TestFilterBeansWithoutJwtHttpAuthorizationFilter
  {
    @Bean
    JwtHttpAuthorizationFilter jwtHttpAuthorizationFilter() {
      return mock(JwtHttpAuthorizationFilter.class);
    }
  }

  @Configuration
  static class TestFilterBeansWithoutJwtHttpAuthorizationFilter
      extends BaseFilterBeans
  {
    @Bean
    InsightConfig insightConfig() {
      return createMultiTenantInsightConfig("local/");
    }
  }

  @Configuration
  static class TestFilterBeansWithRemoteAuth0Domain
      extends BaseFilterBeans
  {
    @Bean
    InsightConfig insightConfig() {
      return createMultiTenantInsightConfig("sonatype.auth0.com");
    }
  }

  @Configuration
  static class TestFilterBeansWithWrongInsightConfig
      extends BaseFilterBeans
  {
    @Bean
    InsightConfig insightConfig() {
      return new InsightConfig();
    }
  }

  @Configuration
  static class ParentAuthorizationFilterBeans
  {
    @Bean
    MultiTenantJwkProvider multiTenantJwkProvider() {
      return mock(MultiTenantJwkProvider.class);
    }

    @Bean
    JwtHttpAuthorizationFilter jwtHttpAuthorizationFilter(MultiTenantJwkProvider multiTenantJwkProvider) {
      return new JwtHttpAuthorizationFilter(multiTenantJwkProvider);
    }
  }
}
