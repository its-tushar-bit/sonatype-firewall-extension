/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.health.MtiqHealthConfig.MtiqHealthCheckConfig;
import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MtiqHealthCheckConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(MtiqHealthCheckConfiguration.class);

  static final String MTIQ_HEALTHCHECK_SERVLET_BEAN = "mtiqHealthCheckServlet";

  private final List<ScheduledHealthCheckRunner> scheduledRunners = new ArrayList<>();

  private final List<MtiqHealthCheckConfig> scheduledCheckConfigs = new ArrayList<>();

  private boolean mtiqHealthEnabled;

  private boolean initialOverallState;

  private ScheduledExecutorService executor;

  private ShutdownHandler shutdownHandler;

  @Bean
  @Primary
  public HealthCheckRegistry mtiqHealthCheckRegistry(
      MultiTenantInsightConfig mtiqConfig,
      Set<AbstractOperationalCheck> operationalChecks,
      ShutdownHandler shutdownHandler)
  {
    this.shutdownHandler = shutdownHandler;

    MtiqHealthConfig healthConfig = parseMtiqHealthConfig(mtiqConfig.getMtiqHealth());
    mtiqHealthEnabled = healthConfig != null && healthConfig.isEnabled();
    initialOverallState = healthConfig != null && healthConfig.isInitialOverallState();

    HealthCheckRegistry registry = new HealthCheckRegistry();

    Map<String, AbstractOperationalCheck> checksByName = new LinkedHashMap<>();
    for (AbstractOperationalCheck check : operationalChecks) {
      checksByName.put(check.getName(), check);
    }
    checksByName.put("deadlocks", new DeadlockOperationalCheck());

    if (healthConfig != null && healthConfig.isEnabled()) {
      log.info("Configuring scheduled health checks from mtiq-health config");

      for (MtiqHealthCheckConfig checkConfig : healthConfig.getHealthChecks()) {
        AbstractOperationalCheck check = checksByName.remove(checkConfig.getName());
        if (check != null) {
          ScheduledHealthCheckRunner runner = new ScheduledHealthCheckRunner(check, checkConfig);
          scheduledRunners.add(runner);
          scheduledCheckConfigs.add(checkConfig);
          registry.register(check.getName(), new HealthCheck()
          {
            @Override
            protected Result check() {
              return runner.getCachedResult();
            }
          });
        }
        else {
          log.warn("Health check '{}' configured in mtiq-health but no matching health check bean found",
              checkConfig.getName());
        }
      }
    }

    for (AbstractOperationalCheck check : checksByName.values()) {
      registry.register(check.getName(), createOnDemandHealthCheck(check));
    }

    return registry;
  }

  @Bean(name = MTIQ_HEALTHCHECK_SERVLET_BEAN)
  HttpServlet mtiqHealthCheckServlet() {
    return new HttpServlet()
    {
      private final ObjectMapper mapper = new ObjectMapper();

      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> allViews = new ArrayList<>();
        for (int i = 0; i < scheduledRunners.size(); i++) {
          ScheduledHealthCheckRunner runner = scheduledRunners.get(i);
          MtiqHealthCheckConfig config = scheduledCheckConfigs.get(i);
          Map<String, Object> view = new LinkedHashMap<>();
          view.put("name", config.getName());
          view.put("healthy", runner.getCachedResult().isHealthy());
          view.put("type", config.getType());
          view.put("critical", config.isCritical());
          allViews.add(view);
        }
        allViews.sort(Comparator.comparing(v -> (String) v.get("name")));

        String[] names = req.getParameterValues("name");
        List<Map<String, Object>> filtered;
        if (names != null && names.length > 0) {
          Set<String> nameSet = Set.of(names);
          if (nameSet.contains("all")) {
            filtered = allViews;
          }
          else {
            filtered = allViews.stream()
                .filter(v -> nameSet.contains(v.get("name")))
                .toList();
          }
        }
        else {
          filtered = List.of();
        }

        boolean healthy = initialOverallState;
        if (!allViews.isEmpty()) {
          healthy = allViews.stream()
              .filter(v -> (boolean) v.get("critical"))
              .allMatch(v -> (boolean) v.get("healthy"));
        }

        resp.setContentType("application/json");
        resp.setStatus(healthy ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        mapper.writeValue(resp.getOutputStream(), filtered);
      }
    };
  }

  @Bean
  SmartInitializingSingleton startScheduledHealthChecks() {
    return this::startScheduler;
  }

  private void startScheduler() {
    if (scheduledRunners.isEmpty()) {
      return;
    }

    log.info("Starting {} scheduled health check(s)", scheduledRunners.size());
    executor = Executors.newScheduledThreadPool(1, r -> {
      Thread t = new Thread(r, "mtiq-health-scheduler");
      t.setDaemon(true);
      return t;
    });
    shutdownHandler.add(executor, ShutdownPriority.HEALTH_CHECK_SCHEDULER);

    for (ScheduledHealthCheckRunner runner : scheduledRunners) {
      runner.start(executor);
    }
  }

  @PreDestroy
  public void stop() {
    if (executor != null && !executor.isShutdown()) {
      executor.shutdownNow();
    }
  }

  private MtiqHealthConfig parseMtiqHealthConfig(Object rawConfig) {
    if (rawConfig == null) {
      return null;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return mapper.convertValue(rawConfig, MtiqHealthConfig.class);
    }
    catch (Exception e) {
      log.warn("Failed to parse mtiq-health config, health checks will run on-demand", e);
      return null;
    }
  }

  private static HealthCheck createOnDemandHealthCheck(AbstractOperationalCheck check) {
    return new HealthCheck()
    {
      @Override
      protected Result check() throws Exception {
        Health health = check.check();
        ResultBuilder builder = health.getStatus() == Status.UP
            ? Result.builder().healthy()
            : Result.builder().unhealthy();
        health.getDetails().forEach(builder::withDetail);
        return builder.build();
      }
    };
  }

  private static class DeadlockOperationalCheck
      extends AbstractOperationalCheck
  {
    private final ThreadDeadlockDetector detector = new ThreadDeadlockDetector();

    DeadlockOperationalCheck() {
      super("deadlocks");
    }

    @Override
    public Health check() {
      Set<String> deadlocks = detector.getDeadlockedThreads();
      if (deadlocks.isEmpty()) {
        return Health.up().build();
      }
      return Health.down()
          .withDetail("deadlocks", String.join(System.lineSeparator(), deadlocks))
          .build();
    }
  }
}
