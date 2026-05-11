/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeEnvironmentTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private RuntimeEnvironmentTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData() {
    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.RUNTIME_ENVIRONMENT);

    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.JVM_NAME))
        .isEqualTo(System.getProperty("java.vm.name"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.JVM_VERSION))
        .isEqualTo(System.getProperty("java.version"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.JVM_VENDOR))
        .isEqualTo(System.getProperty("java.vendor"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.OS_NAME)).isEqualTo(System.getProperty("os.name"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.OS_VERSION))
        .isEqualTo(System.getProperty("os.version"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.OS_ARCHITECTURE))
        .isEqualTo(System.getProperty("os.arch"));
  }

  @Test
  public void testCollectDataIncludesGitVersionAttributes() {
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();

    assertThat(attributes).containsKey(RuntimeEnvironmentTelemetryCollector.NATIVE_GIT_AVAILABLE);
    assertThat(attributes).containsKey(RuntimeEnvironmentTelemetryCollector.GIT_VERSION);
    assertThat(attributes).containsKey(RuntimeEnvironmentTelemetryCollector.GIT_PARTIAL_CLONE_SUPPORTED);
    assertThat(attributes).containsKey(RuntimeEnvironmentTelemetryCollector.GIT_IMPLEMENTATION_CONFIGURED);

    // git is available in CI and dev environments
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.NATIVE_GIT_AVAILABLE)).isEqualTo(true);
    assertThat((String) attributes.get(RuntimeEnvironmentTelemetryCollector.GIT_VERSION)).matches("\\d+\\.\\d+\\.\\d+");
  }

  @Test
  public void testParseVersion() {
    assertThat(RuntimeEnvironmentTelemetryCollector.parseVersion("git version 2.39.1")).isEqualTo("2.39.1");
    assertThat(RuntimeEnvironmentTelemetryCollector.parseVersion("git version 2.39.1 (Apple Git-154)"))
        .isEqualTo("2.39.1");
    assertThat(RuntimeEnvironmentTelemetryCollector.parseVersion(null)).isNull();
    assertThat(RuntimeEnvironmentTelemetryCollector.parseVersion("")).isNull();
    assertThat(RuntimeEnvironmentTelemetryCollector.parseVersion("not a version")).isNull();
  }

  @Test
  public void testSupportsPartialClone() {
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 2.39.1")).isTrue();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 2.39.1 (Apple Git-154)"))
        .isTrue();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 2.27.0")).isTrue();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 3.0.0")).isTrue();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 2.26.9")).isFalse();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 2.16.0")).isFalse();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("git version 1.9.0")).isFalse();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone(null)).isFalse();
    assertThat(RuntimeEnvironmentTelemetryCollector.supportsPartialClone("not a version")).isFalse();
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isFalse();
  }
}
