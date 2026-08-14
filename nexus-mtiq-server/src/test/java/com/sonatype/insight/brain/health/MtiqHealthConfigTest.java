/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sonatype.insight.brain.health.MtiqHealthConfig.MtiqHealthCheckConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class MtiqHealthConfigTest
{
  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void testParseProductionConfig() throws Exception {
    String yaml = """
        type: mtiq-health
        healthCheckUrlPaths:
          - /healthcheck
        initialOverallState: false
        healthChecks:
          - name: tenant-registration
            critical: true
            type: READY
            schedule:
              initialDelay: 3s
              checkInterval: 90s
              downtimeInterval: 2s
              successAttempts: 1
              failureAttempts: 1
          - name: work-directories
            critical: true
            type: ALIVE
            initialState: true
            schedule:
              initialDelay: 10s
              checkInterval: 15s
              downtimeInterval: 10s
          - name: deadlocks
            critical: true
            type: ALIVE
            initialState: true
        """;

    MtiqHealthConfig config = mapper.readValue(yaml, MtiqHealthConfig.class);

    assertThat(config.isEnabled()).isTrue();
    assertThat(config.isInitialOverallState()).isFalse();
    assertThat(config.getHealthCheckUrlPaths()).containsExactly("/healthcheck");
    assertThat(config.getHealthChecks()).hasSize(3);

    MtiqHealthCheckConfig tenantReg = config.getHealthChecks().get(0);
    assertThat(tenantReg.getName()).isEqualTo("tenant-registration");
    assertThat(tenantReg.isCritical()).isTrue();
    assertThat(tenantReg.getType()).isEqualTo("READY");
    assertThat(tenantReg.isInitialState()).isFalse();
    assertThat(tenantReg.getSchedule().getInitialDelay()).isEqualTo(Duration.ofSeconds(3));
    assertThat(tenantReg.getSchedule().getCheckInterval()).isEqualTo(Duration.ofSeconds(90));
    assertThat(tenantReg.getSchedule().getDowntimeInterval()).isEqualTo(Duration.ofSeconds(2));
    assertThat(tenantReg.getSchedule().getSuccessAttempts()).isEqualTo(1);
    assertThat(tenantReg.getSchedule().getFailureAttempts()).isEqualTo(1);

    MtiqHealthCheckConfig workDirs = config.getHealthChecks().get(1);
    assertThat(workDirs.getName()).isEqualTo("work-directories");
    assertThat(workDirs.getType()).isEqualTo("ALIVE");
    assertThat(workDirs.isInitialState()).isTrue();
    assertThat(workDirs.getSchedule().getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
    assertThat(workDirs.getSchedule().getCheckInterval()).isEqualTo(Duration.ofSeconds(15));
    assertThat(workDirs.getSchedule().getDowntimeInterval()).isEqualTo(Duration.ofSeconds(10));

    MtiqHealthCheckConfig deadlocks = config.getHealthChecks().get(2);
    assertThat(deadlocks.getName()).isEqualTo("deadlocks");
    assertThat(deadlocks.isInitialState()).isTrue();
    assertThat(deadlocks.getSchedule().getCheckInterval()).isEqualTo(Duration.ofSeconds(5));
    assertThat(deadlocks.getSchedule().getDowntimeInterval()).isEqualTo(Duration.ofSeconds(30));
    assertThat(deadlocks.getSchedule().getFailureAttempts()).isEqualTo(3);
    assertThat(deadlocks.getSchedule().getSuccessAttempts()).isEqualTo(2);
  }

  @Test
  public void testParseDisabledConfig() throws Exception {
    String yaml = """
        type: mtiq-health
        enabled: false
        """;

    MtiqHealthConfig config = mapper.readValue(yaml, MtiqHealthConfig.class);

    assertThat(config.isEnabled()).isFalse();
  }

  @Test
  public void testDefaultValues() throws Exception {
    String yaml = """
        type: mtiq-health
        healthChecks:
          - name: test-check
        """;

    MtiqHealthConfig config = mapper.readValue(yaml, MtiqHealthConfig.class);

    assertThat(config.isEnabled()).isTrue();
    assertThat(config.isInitialOverallState()).isFalse();

    MtiqHealthCheckConfig check = config.getHealthChecks().get(0);
    assertThat(check.isCritical()).isTrue();
    assertThat(check.getType()).isEqualTo("READY");
    assertThat(check.isInitialState()).isFalse();
    assertThat(check.getSchedule().getCheckInterval()).isEqualTo(Duration.ofSeconds(5));
    assertThat(check.getSchedule().getDowntimeInterval()).isEqualTo(Duration.ofSeconds(30));
    assertThat(check.getSchedule().getInitialDelay()).isEqualTo(Duration.ofSeconds(5));
    assertThat(check.getSchedule().getFailureAttempts()).isEqualTo(3);
    assertThat(check.getSchedule().getSuccessAttempts()).isEqualTo(2);
  }

  @Test
  public void testIgnoresUnknownFields() throws Exception {
    String yaml = """
        type: mtiq-health
        unknownField: value
        healthChecks:
          - name: test-check
            schedule:
              initialState: false
              unknownScheduleField: value
        """;

    MtiqHealthConfig config = mapper.readValue(yaml, MtiqHealthConfig.class);

    assertThat(config.getHealthChecks()).hasSize(1);
    assertThat(config.getHealthChecks().get(0).getName()).isEqualTo("test-check");
  }
}
