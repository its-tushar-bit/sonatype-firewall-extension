/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.backup.DbBackupTask;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.AdminTaskServlet;
import com.sonatype.insight.brain.spring.config.AdminCompatibilityConfiguration;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class AdminCompatibilityServletConfigurationTest
{
  @Test
  public void shouldApplyToAnyManagementContext() {
    ManagementContextConfiguration annotation =
        AdminCompatibilityConfiguration.class.getAnnotation(ManagementContextConfiguration.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEqualTo(ManagementContextType.CHILD);
  }

  @Test
  public void shouldRegisterLegacyDatabaseHealthcheckServlet() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestCompatibilityBeans.class, AdminCompatibilityConfiguration.class);
      context.refresh();

      ServletRegistrationBean<?> bean =
          context.getBean("existingDbConnectionHealthcheckServlet", ServletRegistrationBean.class);

      assertThat(bean.getUrlMappings()).contains("/healthcheck/database");
    }
  }

  @Test
  public void shouldRegisterLegacyNewDatabaseConnectionsHealthcheckServlet() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestCompatibilityBeans.class, AdminCompatibilityConfiguration.class);
      context.refresh();

      ServletRegistrationBean<?> bean =
          context.getBean("newDbConnectionHealthcheckServlet", ServletRegistrationBean.class);

      assertThat(bean.getUrlMappings()).contains("/healthcheck/newDatabaseConnections");
    }
  }

  @Test
  public void shouldAutoRegisterAdminTaskServlets() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestCompatibilityBeans.class, AdminCompatibilityConfiguration.class);
      context.refresh();

      ServletContextInitializer initializer = context.getBean("adminTaskServletInitializer",
          ServletContextInitializer.class);
      ServletContext servletContext = mock(ServletContext.class);
      ServletRegistration.Dynamic registration = mock(ServletRegistration.Dynamic.class);
      when(servletContext.addServlet(any(String.class), any(AdminTaskServlet.class))).thenReturn(registration);

      initializer.onStartup(servletContext);

      verify(servletContext).addServlet(eq("shutdownTaskServlet"), any(AdminTaskServlet.class));
      verify(registration).addMapping("/tasks/shutdown");
      verify(servletContext).addServlet(eq("copyStorageTaskServlet"), any(AdminTaskServlet.class));
      verify(registration).addMapping("/tasks/copyStorage");
      verify(servletContext).addServlet(eq("backupDbTaskServlet"), any(AdminTaskServlet.class));
      verify(registration).addMapping("/tasks/backupDb");
      verify(servletContext).addServlet(eq("log-levelTaskServlet"), any(AdminTaskServlet.class));
      verify(registration).addMapping("/tasks/log-level");
    }
  }

  @Test
  public void shouldExecuteAdminTaskServletUsingLegacyPostContract() throws Exception {
    AdminTask task = mock(AdminTask.class);
    when(task.getPath()).thenReturn("testTask");
    AdminTaskServlet servlet = new AdminTaskServlet(task);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tasks/testTask");
    request.addParameter("key", "value");
    MockHttpServletResponse response = new MockHttpServletResponse();

    servlet.service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(task).execute(eq(Map.of("key", List.of("value"))), any(PrintWriter.class));
  }

  @Test
  public void shouldMapBadRequestExceptionTo400() throws Exception {
    AdminTask task = mock(AdminTask.class);
    when(task.getPath()).thenReturn("testTask");
    doThrow(new BadRequestException("bad input")).when(task).execute(any(), any());
    AdminTaskServlet servlet = new AdminTaskServlet(task);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tasks/testTask");
    MockHttpServletResponse response = new MockHttpServletResponse();

    servlet.service(request, response);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("bad input");
  }

  @Test
  public void shouldExecuteBackupDbTask() throws Exception {
    DbBackupTask task = mock(DbBackupTask.class);
    when(task.getPath()).thenReturn(DbBackupTask.PATH);
    doCallRealMethod().when(task).execute(any(), any());
    when(task.doBackup()).thenReturn(DbBackupTask.RESPONSE_MESSAGE_PREFIX + "/tmp/backup-dir");
    AdminTaskServlet servlet = new AdminTaskServlet(task);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tasks/backupDb");
    MockHttpServletResponse response = new MockHttpServletResponse();

    servlet.service(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains(DbBackupTask.RESPONSE_MESSAGE_PREFIX + "/tmp/backup-dir");
  }

  @Test
  public void shouldMapUnsupportedBackupDbRequestsToBadRequest() throws Exception {
    DbBackupTask task = mock(DbBackupTask.class);
    when(task.getPath()).thenReturn(DbBackupTask.PATH);
    doCallRealMethod().when(task).execute(any(), any());
    when(task.doBackup()).thenThrow(new BadRequestException("The DB backup task is supported only for h2 databases."));
    AdminTaskServlet servlet = new AdminTaskServlet(task);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tasks/backupDb");
    MockHttpServletResponse response = new MockHttpServletResponse();

    servlet.service(request, response);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("supported only for h2 databases");
  }

  @Configuration
  static class TestCompatibilityBeans
  {
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
      ExistingDbConnectionAdminHealthCheckEndpoint endpoint = mock(ExistingDbConnectionAdminHealthCheckEndpoint.class);
      when(endpoint.getPath()).thenReturn("/healthcheck/database");
      return endpoint;
    }

    @Bean
    NewDbConnectionAdminHealthCheckEndpoint newDbConnectionAdminHealthCheckEndpoint() {
      NewDbConnectionAdminHealthCheckEndpoint endpoint = mock(NewDbConnectionAdminHealthCheckEndpoint.class);
      when(endpoint.getPath()).thenReturn("/healthcheck/newDatabaseConnections");
      return endpoint;
    }

    @Bean
    ThreadDeadlockAdminHealthCheckEndpoint threadDeadlockAdminHealthCheckEndpoint() {
      ThreadDeadlockAdminHealthCheckEndpoint endpoint = mock(ThreadDeadlockAdminHealthCheckEndpoint.class);
      when(endpoint.getPath()).thenReturn("/healthcheck/threadDeadlock");
      return endpoint;
    }

    @Bean
    WorkDirectoryAdminHealthCheckEndpoint workDirectoryAdminHealthCheckEndpoint() {
      WorkDirectoryAdminHealthCheckEndpoint endpoint = mock(WorkDirectoryAdminHealthCheckEndpoint.class);
      when(endpoint.getPath()).thenReturn("/healthcheck/workDirectory");
      return endpoint;
    }

    @Bean
    ClusterDirectoryAdminHealthCheckEndpoint clusterDirectoryAdminHealthCheckEndpoint() {
      ClusterDirectoryAdminHealthCheckEndpoint endpoint = mock(ClusterDirectoryAdminHealthCheckEndpoint.class);
      when(endpoint.getPath()).thenReturn("/healthcheck/clusterDirectory");
      return endpoint;
    }

    @Bean
    AdminTask shutdownTask() {
      AdminTask task = mock(AdminTask.class);
      when(task.getPath()).thenReturn("shutdown");
      return task;
    }

    @Bean
    AdminTask copyStorageTask() {
      AdminTask task = mock(AdminTask.class);
      when(task.getPath()).thenReturn("copyStorage");
      return task;
    }

    @Bean
    DbBackupTask dbBackupTask() {
      DbBackupTask task = mock(DbBackupTask.class);
      when(task.getPath()).thenReturn(DbBackupTask.PATH);
      return task;
    }
  }
}
