/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.backup.DbBackupTask;
import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint.HealthCheckResponse;
import com.sonatype.insight.brain.operational.check.ClusterDirectoryAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ExistingDbConnectionAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.NewDbConnectionAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ThreadDeadlockAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.WorkDirectoryAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.policy.waiver.WaiverExpirationDetectionTask;
import com.sonatype.insight.brain.shutdown.ShutdownTask;
import com.sonatype.insight.brain.spring.config.InsightJacksonMessageBodyProvider;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class MtiqAdminJerseyConfigurationTest
{
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  @Test
  public void shouldRegisterParentAdminResourcesInChildManagementContext() {
    try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext())
    {
      parent.register(ParentAdminResourceConfiguration.class);
      parent.refresh();

      child.setParent(parent);
      child.register(ChildAdminJerseyConfiguration.class, MtiqAdminJerseyConfiguration.class);
      child.refresh();

      AdminResourceBundle adminResourceBundle = child.getBean(AdminResourceBundle.class);
      ResourceConfig resourceConfig = child.getBean("mtiqAdminResourceConfig", ResourceConfig.class);
      JaxRsExceptionMapper configuredJaxRsExceptionMapper = child.getBean(
          MtiqAdminJaxRsErrorConfiguration.CONFIGURED_JAX_RS_EXCEPTION_MAPPER_BEAN_NAME,
          JaxRsExceptionMapper.class);

      assertThat(adminResourceBundle.getRegisteredResources())
          .anyMatch(resource -> AopUtils.getTargetClass(resource).equals(ParentAdminResource.class));
      assertThat(resourceConfig.getInstances())
          .anyMatch(resource -> AopUtils.getTargetClass(resource).equals(ParentAdminResource.class));
      assertThat(resourceConfig.getInstances()).contains(
          child.getBean(InsightJacksonMessageBodyProvider.class),
          configuredJaxRsExceptionMapper);
    }
  }

  @Test
  public void shouldIgnoreUnknownJsonPropertiesForMtiqAdminJerseyRequestBodies() throws Exception {
    try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext())
    {
      parent.register(ParentAdminResourceConfiguration.class);
      parent.refresh();

      child.setParent(parent);
      child.register(ChildAdminJerseyConfiguration.class, MtiqAdminJerseyConfiguration.class);
      child.refresh();

      InsightJacksonMessageBodyProvider provider = child.getBean(InsightJacksonMessageBodyProvider.class);

      TestDto dto = readDto(provider, "{\"name\":\"tenant-a\",\"unknown\":\"ignored\"}");

      assertThat(dto.name).isEqualTo("tenant-a");
      assertThat(child.getBean("objectMapper", ObjectMapper.class)
          .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
      assertThatThrownBy(() -> child.getBean("objectMapper", ObjectMapper.class)
          .readValue("{\"name\":\"tenant-a\",\"unknown\":\"rejected\"}", TestDto.class))
              .hasMessageContaining("unknown");
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

  @Configuration
  static class ParentAdminResourceConfiguration
  {
    @Bean
    ParentAdminResource parentAdminResource() {
      return new ParentAdminResource();
    }
  }

  @Configuration
  static class ChildAdminJerseyConfiguration
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
    ExistingDbConnectionAdminHealthCheckEndpoint existingDbConnectionAdminHealthCheckEndpoint() {
      return mockHealthCheckEndpoint(ExistingDbConnectionAdminHealthCheckEndpoint.class, "/healthcheck/database");
    }

    @Bean
    NewDbConnectionAdminHealthCheckEndpoint newDbConnectionAdminHealthCheckEndpoint() {
      return mockHealthCheckEndpoint(NewDbConnectionAdminHealthCheckEndpoint.class, "/healthcheck/newDatabase");
    }

    @Bean
    ThreadDeadlockAdminHealthCheckEndpoint threadDeadlockAdminHealthCheckEndpoint() {
      return mockHealthCheckEndpoint(ThreadDeadlockAdminHealthCheckEndpoint.class, "/healthcheck/deadlocks");
    }

    @Bean
    WorkDirectoryAdminHealthCheckEndpoint workDirectoryAdminHealthCheckEndpoint() {
      return mockHealthCheckEndpoint(WorkDirectoryAdminHealthCheckEndpoint.class, "/healthcheck/workDirectory");
    }

    @Bean
    ClusterDirectoryAdminHealthCheckEndpoint clusterDirectoryAdminHealthCheckEndpoint() {
      return mockHealthCheckEndpoint(ClusterDirectoryAdminHealthCheckEndpoint.class, "/healthcheck/clusterDirectory");
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
    WaiverExpirationDetectionTask waiverExpirationDetectionTask() {
      WaiverExpirationDetectionTask task = mock(WaiverExpirationDetectionTask.class);
      when(task.getPath()).thenReturn(WaiverExpirationDetectionTask.PATH);
      return task;
    }

    @Bean
    DbBackupTask dbBackupTask() {
      return mock(DbBackupTask.class);
    }

    private static <T extends com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint> T mockHealthCheckEndpoint(
        Class<T> endpointType,
        String path)
    {
      T endpoint = mock(endpointType);
      when(endpoint.getPath()).thenReturn(path);
      when(endpoint.getHealthCheckResponse()).thenReturn(new HealthCheckResponse(true));
      return endpoint;
    }
  }

  static class TestDto
  {
    public String name;
  }

  @Path("/admin/parent")
  @MtiqAdminEndpoint
  static class ParentAdminResource
  {
    @GET
    public String get() {
      return "ok";
    }
  }
}
