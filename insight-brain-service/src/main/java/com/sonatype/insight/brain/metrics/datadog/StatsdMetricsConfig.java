/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics.datadog;

public class StatsdMetricsConfig
{
  private boolean enabled;

  private boolean buffered;

  private String metricsPrefix;

  private String metricsTeam;

  private String host;

  private int port;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isBuffered() {
    return buffered;
  }

  public void setBuffered(boolean buffered) {
    this.buffered = buffered;
  }

  public String getMetricsPrefix() {
    return metricsPrefix;
  }

  public void setMetricsPrefix(String metricsPrefix) {
    this.metricsPrefix = metricsPrefix;
  }

  public String getMetricsTeam() {
    return metricsTeam;
  }

  public void setMetricsTeam(String metricsTeam) {
    this.metricsTeam = metricsTeam;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
