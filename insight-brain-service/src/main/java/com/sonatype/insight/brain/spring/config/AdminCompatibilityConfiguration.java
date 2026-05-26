/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ClusterDirectoryAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ExistingDbConnectionAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.NewDbConnectionAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.ThreadDeadlockAdminHealthCheckEndpoint;
import com.sonatype.insight.brain.operational.check.WorkDirectoryAdminHealthCheckEndpoint;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.sonatype.insight.brain.service.AdminTaskServlet;
import com.sonatype.insight.error.exception.BadRequestException;
import io.dropwizard.metrics.servlets.CpuProfileServlet;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import io.dropwizard.metrics.servlets.PingServlet;
import io.dropwizard.metrics.servlets.ThreadDumpServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class AdminCompatibilityConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(AdminCompatibilityConfiguration.class);

  @Bean
  public ServletRegistrationBean<HttpServlet> existingDbConnectionHealthcheckServlet(
      ExistingDbConnectionAdminHealthCheckEndpoint endpoint)
  {
    return registerHealthcheckServlet("existingDbConnectionHealthcheckServlet", endpoint);
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> newDbConnectionHealthcheckServlet(
      NewDbConnectionAdminHealthCheckEndpoint endpoint)
  {
    return registerHealthcheckServlet("newDbConnectionHealthcheckServlet", endpoint);
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> threadDeadlockHealthcheckServlet(
      ThreadDeadlockAdminHealthCheckEndpoint endpoint)
  {
    return registerHealthcheckServlet("threadDeadlockHealthcheckServlet", endpoint);
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> workDirectoryHealthcheckServlet(
      WorkDirectoryAdminHealthCheckEndpoint endpoint)
  {
    return registerHealthcheckServlet("workDirectoryHealthcheckServlet", endpoint);
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> clusterDirectoryHealthcheckServlet(
      ClusterDirectoryAdminHealthCheckEndpoint endpoint)
  {
    return registerHealthcheckServlet("clusterDirectoryHealthcheckServlet", endpoint);
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> adminIndexServlet() {
    HttpServlet servlet = new HttpServlet()
    {
      @Override
      protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SC_OK);
        response.setContentType("text/html");
        try (var in = getClass().getClassLoader().getResourceAsStream("admin-index.html")) {
          if (in != null) {
            in.transferTo(response.getOutputStream());
          }
        }
      }
    };
    return registerServlet("adminIndexServlet", servlet, "/");
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> pingServlet() {
    return registerServlet("pingServlet", new PingServlet(), "/ping");
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> metricsServlet() {
    return registerServlet("metricsServlet", new MetricsServlet(), "/metrics");
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> healthcheckServlet(
      @Qualifier("mtiqHealthCheckServlet") Optional<HttpServlet> mtiqServlet)
  {
    HttpServlet servlet = mtiqServlet.orElseGet(HealthCheckServlet::new);
    return registerServlet("healthcheckServlet", servlet, "/healthcheck");
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> threadsServlet() {
    return registerServlet("threadsServlet", new ThreadDumpServlet(), "/threads");
  }

  @Bean
  public ServletRegistrationBean<HttpServlet> cpuProfileServlet() {
    return registerServlet("cpuProfileServlet", new CpuProfileServlet(), "/pprof");
  }

  @Bean
  public ServletContextInitializer metricsServletContextInitializer(
      MetricRegistry metricRegistry,
      HealthCheckRegistry healthCheckRegistry)
  {
    return servletContext -> {
      servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
      servletContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
    };
  }

  /**
   * Dynamically registers a servlet for every {@link AdminTask} bean in the application context.
   * New AdminTask implementations are picked up automatically without changes to this class.
   */
  @Bean
  public ServletContextInitializer adminTaskServletInitializer(List<AdminTask> tasks) {
    return servletContext -> {
      for (AdminTask task : tasks) {
        String path = "/tasks/" + task.getPath();
        var registration = servletContext.addServlet(task.getPath() + "TaskServlet", new AdminTaskServlet(task));
        if (registration != null) {
          registration.addMapping(path);
        }
        else {
          log.warn("Servlet '{}TaskServlet' already registered, skipping mapping for path: {}", task.getPath(), path);
        }
      }
    };
  }

  @Bean
  public AdminTask logLevelTask() {
    return new AdminTask("log-level")
    {
      @Override
      public void execute(Map<String, List<String>> parameters, java.io.PrintWriter output) {
        updateLogLevels(parameters);
      }
    };
  }

  private ServletRegistrationBean<HttpServlet> registerHealthcheckServlet(
      String name,
      AdminHealthCheckEndpoint endpoint)
  {
    return registerServlet(name, AdminHealthCheckEndpoint.createServlet(endpoint), endpoint.getPath());
  }

  private void updateLogLevels(Map<String, List<String>> parameters) {
    List<String> loggerNames = parameters.getOrDefault("logger", List.of());
    List<String> levels = parameters.getOrDefault("level", List.of());

    if (loggerNames.size() != levels.size()) {
      throw new BadRequestException("Expected matching logger and level parameters");
    }

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (int index = 0; index < loggerNames.size(); index++) {
      String loggerName = loggerNames.get(index);
      if (loggerName == null || loggerName.isBlank()) {
        throw new BadRequestException("Logger name must not be blank");
      }
      String levelName = levels.get(index);

      Level level = null;
      if (levelName != null && !levelName.isBlank()) {
        level = Level.toLevel(levelName, null);
        if (level == null) {
          throw new BadRequestException("Invalid log level: " + levelName);
        }
      }

      loggerContext.getLogger(loggerName).setLevel(level);
    }
  }

  private ServletRegistrationBean<HttpServlet> registerServlet(String name, HttpServlet servlet, String path) {
    ServletRegistrationBean<HttpServlet> registration = new ServletRegistrationBean<>(servlet, path);
    registration.setName(name);
    registration.setLoadOnStartup(1);
    return registration;
  }

}
