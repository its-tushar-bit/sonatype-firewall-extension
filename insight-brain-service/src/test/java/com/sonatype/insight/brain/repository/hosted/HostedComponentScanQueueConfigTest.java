/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HostedComponentScanQueueConfigTest
{
  @Test
  public void defaultConfig_returnsExpectedValues() {
    HostedComponentScanQueueConfig config = HostedComponentScanQueueConfig.defaultConfig();

    assertThat(config.enabled()).isEqualTo(HostedComponentScanQueueConfig.DEFAULT_ENABLED);
    assertThat(config.workerThreadsPerTenant())
        .isEqualTo(HostedComponentScanQueueConfig.DEFAULT_WORKER_THREADS_PER_TENANT);
    assertThat(config.pollIntervalMilliseconds())
        .isEqualTo(HostedComponentScanQueueConfig.DEFAULT_POLL_INTERVAL_MILLISECONDS);
    assertThat(config.maxQueuedRows()).isEqualTo(HostedComponentScanQueueConfig.DEFAULT_MAX_QUEUED_ROWS);
    assertThat(config.maxRetries()).isEqualTo(HostedComponentScanQueueConfig.DEFAULT_MAX_RETRIES);
  }

  @Test
  public void merge_withEmptyOverrides_returnsBaseConfigUnchanged() {
    HostedComponentScanQueueConfig base = HostedComponentScanQueueConfig.defaultConfig();

    HostedComponentScanQueueConfig result = HostedComponentScanQueueConfig.merge(base, Map.of());

    assertThat(result.enabled()).isEqualTo(base.enabled());
    assertThat(result.workerThreadsPerTenant()).isEqualTo(base.workerThreadsPerTenant());
    assertThat(result.pollIntervalMilliseconds()).isEqualTo(base.pollIntervalMilliseconds());
    assertThat(result.maxQueuedRows()).isEqualTo(base.maxQueuedRows());
    assertThat(result.maxRetries()).isEqualTo(base.maxRetries());
  }

  @Test
  public void merge_withPartialOverrides_onlyChangesSpecifiedFields() {
    HostedComponentScanQueueConfig base = HostedComponentScanQueueConfig.defaultConfig();

    HostedComponentScanQueueConfig result = HostedComponentScanQueueConfig.merge(base,
        Map.of("maxRetries", 7));

    assertThat(result.maxRetries()).isEqualTo(7);
    assertThat(result.enabled()).isEqualTo(base.enabled());
    assertThat(result.workerThreadsPerTenant()).isEqualTo(base.workerThreadsPerTenant());
    assertThat(result.pollIntervalMilliseconds()).isEqualTo(base.pollIntervalMilliseconds());
    assertThat(result.maxQueuedRows()).isEqualTo(base.maxQueuedRows());
  }

  @Test
  public void merge_withAllFieldsOverridden_appliesAllOverrides() {
    HostedComponentScanQueueConfig base = HostedComponentScanQueueConfig.defaultConfig();

    HostedComponentScanQueueConfig result = HostedComponentScanQueueConfig.merge(base, Map.of(
        "enabled", false,
        "workerThreadsPerTenant", 3,
        "pollIntervalMilliseconds", 60_000L,
        "maxQueuedRows", 20,
        "maxRetries", 5));

    assertThat(result.enabled()).isFalse();
    assertThat(result.workerThreadsPerTenant()).isEqualTo(3);
    assertThat(result.pollIntervalMilliseconds()).isEqualTo(60_000L);
    assertThat(result.maxQueuedRows()).isEqualTo(20);
    assertThat(result.maxRetries()).isEqualTo(5);
  }

  @Test
  public void merge_withDisabledOverride_disablesConsumer() {
    HostedComponentScanQueueConfig base = HostedComponentScanQueueConfig.defaultConfig();
    assertThat(base.enabled()).isTrue();

    HostedComponentScanQueueConfig result = HostedComponentScanQueueConfig.merge(base,
        Map.of("enabled", false));

    assertThat(result.enabled()).isFalse();
  }

  @Test
  public void jsonDeserialization_ignoresUnknownFields() throws Exception {
    String json = "{\"enabled\":true,\"workerThreadsPerTenant\":2,\"pollIntervalMilliseconds\":30000," +
        "\"maxQueuedRows\":10,\"maxRetries\":3,\"unknownField\":\"ignored\",\"anotherUnknown\":42}";

    HostedComponentScanQueueConfig config = new ObjectMapper()
        .readValue(json, HostedComponentScanQueueConfig.class);

    assertThat(config.enabled()).isTrue();
    assertThat(config.workerThreadsPerTenant()).isEqualTo(2);
    assertThat(config.pollIntervalMilliseconds()).isEqualTo(30_000L);
    assertThat(config.maxQueuedRows()).isEqualTo(10);
    assertThat(config.maxRetries()).isEqualTo(3);
  }

  @Test
  public void compactConstructor_rejectsZeroWorkerThreads() {
    assertThatThrownBy(() -> new HostedComponentScanQueueConfig(true, 0, 30_000L, 10, 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("workerThreadsPerTenant");
  }

  @Test
  public void compactConstructor_rejectsWorkerThreadsAboveMax() {
    assertThatThrownBy(() -> new HostedComponentScanQueueConfig(
        true, HostedComponentScanQueueConfig.MAX_WORKER_THREADS_PER_TENANT + 1, 30_000L, 10, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("workerThreadsPerTenant");
  }

  @Test
  public void compactConstructor_rejectsZeroPollInterval() {
    assertThatThrownBy(() -> new HostedComponentScanQueueConfig(true, 1, 0L, 10, 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pollIntervalMilliseconds");
  }

  @Test
  public void compactConstructor_rejectsNegativePollInterval() {
    assertThatThrownBy(() -> new HostedComponentScanQueueConfig(true, 1, -1L, 10, 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pollIntervalMilliseconds");
  }

  @Test
  public void compactConstructor_rejectsZeroMaxQueuedRows() {
    assertThatThrownBy(() -> new HostedComponentScanQueueConfig(true, 1, 30_000L, 0, 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxQueuedRows");
  }

  @Test
  public void compactConstructor_rejectsNegativeMaxRetries() {
    assertThatThrownBy(() -> new HostedComponentScanQueueConfig(true, 1, 30_000L, 10, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxRetries");
  }

  @Test
  public void compactConstructor_acceptsZeroMaxRetries() {
    HostedComponentScanQueueConfig config = new HostedComponentScanQueueConfig(true, 1, 30_000L, 10, 0);
    assertThat(config.maxRetries()).isEqualTo(0);
  }
}
