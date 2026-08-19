/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.audit.AuditContainerRequestFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import jakarta.annotation.Priority;
import jakarta.inject.Provider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class JerseyConfigurationTest
{
  @Test
  public void shouldRegisterResourceConfigInsteadOfProviderPackageScanning() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class, JerseyConfiguration.class);
      context.refresh();

      ResourceConfig config = context.getBean(ResourceConfig.class);

      assertThat(config.getInstances())
          .extracting(Object::getClass)
          .contains(TestResource.class, TestParamConverterProvider.class, TestExceptionMapper.class,
              TestRequestFilter.class)
          .doesNotContain(NonJerseyBean.class);
      assertThat(config.getInstances()).anySatisfy(instance -> {
        if (instance instanceof AuditContainerRequestFilter) {
          assertThat(instance.getClass().getAnnotation(Priority.class))
              .isNotNull()
              .extracting(Priority::value)
              .isEqualTo(AuditContainerRequestFilter.PRIORITY);
        }
      });
      InsightJacksonMessageBodyProvider provider = context.getBean(InsightJacksonMessageBodyProvider.class);
      assertThat(config.getInstances()).anyMatch(AuditContainerRequestFilter.class::isInstance);
      assertThat(config.getInstances()).contains(provider);
      assertThat(config.getClasses()).contains(MultiPartFeature.class);
      assertThat(config.getProperty("jersey.config.server.provider.packages")).isNull();
      assertThat(config.getProperty("jersey.config.servlet.filter.forwardOn404")).isEqualTo(true);
    }
  }

  @Test
  public void shouldReturnNullWhenResourceInfoProviderIsMissing() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class, JerseyConfiguration.class);
      context.refresh();

      AuditContainerRequestFilter filter = context.getBean(AuditContainerRequestFilter.class);
      Provider<ResourceInfo> resourceInfoProvider = extractResourceInfoProvider(filter);

      assertThat(resourceInfoProvider.get()).isNull();

      setDelegate(resourceInfoProvider, () -> null);
      assertThat(resourceInfoProvider.get()).isNull();
    }
  }

  @Test
  public void shouldRegisterFilterUsingSelectiveJerseyDelegation() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class, JerseyConfiguration.class);
      context.refresh();

      ResourceConfig resourceConfig = context.getBean(ResourceConfig.class);
      FilterRegistrationBean<?> registration = context.getBean("jerseyFilter", FilterRegistrationBean.class);

      assertThat(registration.getFilter()).isInstanceOf(SelectiveJerseyFilter.class);
      SelectiveJerseyFilter filter = (SelectiveJerseyFilter) registration.getFilter();
      assertThat(filter.getDelegate()).isInstanceOf(ServletContainer.class);
      assertThat(extractResourceConfig(filter.getDelegate())).isSameAs(resourceConfig);
      assertThat(filter.getMatcher().matches("/assets/index.html")).isFalse();
      assertThat(filter.getMatcher().matches("/actuator/health")).isFalse();
      assertThat(registration.getUrlPatterns()).containsExactly("/*");
      assertThat(registration.getInitParameters()).doesNotContainKey("jersey.config.server.provider.packages");
    }
  }

  @SuppressWarnings("unchecked")
  private Provider<ResourceInfo> extractResourceInfoProvider(AuditContainerRequestFilter filter) {
    try {
      Field field = AuditContainerRequestFilter.class.getDeclaredField("resourceInfoProvider");
      field.setAccessible(true);
      return (Provider<ResourceInfo>) field.get(filter);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private void setDelegate(Provider<ResourceInfo> resourceInfoProvider, Provider<ResourceInfo> delegate) {
    try {
      Field field = resourceInfoProvider.getClass().getDeclaredField("delegate");
      field.setAccessible(true);
      field.set(resourceInfoProvider, delegate);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
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
  static class TestJerseyBeans
  {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    TestResource testResource() {
      return new TestResource();
    }

    @Bean
    TestParamConverterProvider testParamConverterProvider() {
      return new TestParamConverterProvider();
    }

    @Bean
    TestExceptionMapper testExceptionMapper() {
      return new TestExceptionMapper();
    }

    @Bean
    TestRequestFilter testRequestFilter() {
      return new TestRequestFilter();
    }

    @Bean
    ApplicationDAO applicationDAO() {
      return Mockito.mock(ApplicationDAO.class);
    }

    @Bean
    OrganizationDAO organizationDAO() {
      return Mockito.mock(OrganizationDAO.class);
    }

    @Bean
    RepositoryDAO repositoryDAO() {
      return Mockito.mock(RepositoryDAO.class);
    }

    @Bean
    RepositoryManagerDAO repositoryManagerDAO() {
      return Mockito.mock(RepositoryManagerDAO.class);
    }

    @Bean
    MetricRegistry metricRegistry() {
      return new MetricRegistry();
    }

    @Bean
    NonJerseyBean nonJerseyBean() {
      return new NonJerseyBean();
    }
  }

  @Path("test")
  static class TestResource
  {
    @GET
    public String get() {
      return "ok";
    }
  }

  static class TestParamConverterProvider
      implements ParamConverterProvider
  {
    @Override
    public <T> ParamConverter<T> getConverter(
        Class<T> rawType,
        Type genericType,
        Annotation[] annotations)
    {
      return null;
    }
  }

  static class TestExceptionMapper
      implements ExceptionMapper<RuntimeException>
  {
    @Override
    public Response toResponse(RuntimeException exception) {
      return Response.serverError().build();
    }
  }

  static class TestRequestFilter
      implements ContainerRequestFilter
  {
    @Override
    public void filter(ContainerRequestContext requestContext) {
      // no-op
    }
  }

  static class NonJerseyBean
  {
    // intentionally empty
  }
}
