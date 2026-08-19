/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.audit.AuditContainerRequestFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.spring.config.InsightJacksonMessageBodyProvider;
import com.sonatype.insight.brain.spring.config.SelectiveJerseyFilter;
import jakarta.inject.Provider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class MtiqJerseyConfigurationTest
{
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  @Test
  public void shouldRegisterMainResourceConfigUnderMtiqBeanName() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class, MtiqJerseyConfiguration.class);
      context.refresh();

      assertThat(context.containsBean("mtiqMainResourceConfig")).isTrue();
      assertThat(context.containsBean("resourceConfig")).isFalse();

      ResourceConfig mainResourceConfig = context.getBean("mtiqMainResourceConfig", ResourceConfig.class);
      FilterRegistrationBean<?> registration = context.getBean("jerseyFilter", FilterRegistrationBean.class);

      assertThat(registration.getFilter()).isInstanceOf(SelectiveJerseyFilter.class);
      SelectiveJerseyFilter filter = (SelectiveJerseyFilter) registration.getFilter();
      ServletContainer container = extractDelegate(filter);
      assertThat(container).isNotNull();
      assertThat(extractResourceConfig(container)).isSameAs(mainResourceConfig);
      assertThat(registration.getUrlPatterns()).containsExactly("/*");
      assertThat(mainResourceConfig.getClasses()).contains(MultiPartFeature.class);
      assertThat(mainResourceConfig.getInstances()).contains(context.getBean(InsightJacksonMessageBodyProvider.class));
      assertThat(mainResourceConfig.getProperty("jersey.config.servlet.filter.forwardOn404")).isEqualTo(true);
    }
  }

  @Test
  public void shouldIgnoreUnknownJsonPropertiesForMtiqJerseyRequestBodies() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class, MtiqJerseyConfiguration.class);
      context.refresh();

      InsightJacksonMessageBodyProvider provider = context.getBean(InsightJacksonMessageBodyProvider.class);

      TestDto dto = readDto(provider, "{\"name\":\"tenant-a\",\"unknown\":\"ignored\"}");

      assertThat(dto.name).isEqualTo("tenant-a");
      assertThat(context.getBean("objectMapper", ObjectMapper.class)
          .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
      assertThatThrownBy(() -> context.getBean("objectMapper", ObjectMapper.class)
          .readValue("{\"name\":\"tenant-a\",\"unknown\":\"rejected\"}", TestDto.class))
              .hasMessageContaining("unknown");
    }
  }

  @Test
  public void shouldReturnNullWhenResourceInfoProviderIsMissing() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class, MtiqJerseyConfiguration.class);
      context.refresh();

      AuditContainerRequestFilter filter = context.getBean(AuditContainerRequestFilter.class);
      Provider<ResourceInfo> resourceInfoProvider = extractResourceInfoProvider(filter);

      assertThat(resourceInfoProvider.get()).isNull();

      setDelegate(resourceInfoProvider, () -> null);
      assertThat(resourceInfoProvider.get()).isNull();
    }
  }

  private TestDto readDto(InsightJacksonMessageBodyProvider provider, String json) throws Exception {
    return (TestDto) provider.readFrom(
        objectClass(TestDto.class),
        TestDto.class,
        NO_ANNOTATIONS,
        MediaType.APPLICATION_JSON_TYPE,
        new MultivaluedHashMap<>(),
        new ByteArrayInputStream(json.getBytes(UTF_8)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Class<Object> objectClass(Class<?> type) {
    return (Class) type;
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

  private ServletContainer extractDelegate(SelectiveJerseyFilter filter) {
    try {
      Field field = SelectiveJerseyFilter.class.getDeclaredField("delegate");
      field.setAccessible(true);
      return (ServletContainer) field.get(filter);
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
    ApplicationDAO applicationDAO() {
      return org.mockito.Mockito.mock(ApplicationDAO.class);
    }

    @Bean
    OrganizationDAO organizationDAO() {
      return org.mockito.Mockito.mock(OrganizationDAO.class);
    }

    @Bean
    RepositoryDAO repositoryDAO() {
      return org.mockito.Mockito.mock(RepositoryDAO.class);
    }

    @Bean
    RepositoryManagerDAO repositoryManagerDAO() {
      return org.mockito.Mockito.mock(RepositoryManagerDAO.class);
    }
  }

  static class TestDto
  {
    public String name;
  }

  @Path("test")
  static class TestResource
  {
    @GET
    public String get() {
      return "ok";
    }
  }
}
