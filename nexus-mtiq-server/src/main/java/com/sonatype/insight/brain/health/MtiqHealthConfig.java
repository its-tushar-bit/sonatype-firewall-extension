/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.brain.spring.config.DropwizardDurationParser;
import java.time.Duration;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MtiqHealthConfig
{
  @JsonProperty
  private String type;

  @JsonProperty
  private boolean enabled = true;

  @JsonProperty
  private boolean initialOverallState = false;

  @JsonProperty
  private List<String> healthCheckUrlPaths = List.of("/healthcheck");

  @JsonProperty
  private List<MtiqHealthCheckConfig> healthChecks = List.of();

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isInitialOverallState() {
    return initialOverallState;
  }

  public List<String> getHealthCheckUrlPaths() {
    return healthCheckUrlPaths;
  }

  public List<MtiqHealthCheckConfig> getHealthChecks() {
    return healthChecks;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MtiqHealthCheckConfig
  {
    @JsonProperty
    private String name;

    @JsonProperty
    private boolean critical = true;

    @JsonProperty
    private String type = "READY";

    @JsonProperty
    private boolean initialState = false;

    @JsonProperty
    private MtiqHealthScheduleConfig schedule = new MtiqHealthScheduleConfig();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isCritical() {
      return critical;
    }

    public String getType() {
      return type;
    }

    public boolean isInitialState() {
      return initialState;
    }

    public void setInitialState(boolean initialState) {
      this.initialState = initialState;
    }

    public MtiqHealthScheduleConfig getSchedule() {
      return schedule;
    }

    public void setSchedule(MtiqHealthScheduleConfig schedule) {
      this.schedule = schedule;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MtiqHealthScheduleConfig
  {
    private static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(5);

    private static final Duration DEFAULT_DOWNTIME_INTERVAL = Duration.ofSeconds(30);

    @JsonProperty
    private String checkInterval;

    @JsonProperty
    private String downtimeInterval;

    @JsonProperty
    private String initialDelay;

    @JsonProperty
    private int failureAttempts = 3;

    @JsonProperty
    private int successAttempts = 2;

    public Duration getCheckInterval() {
      return parseDuration(checkInterval, DEFAULT_CHECK_INTERVAL);
    }

    public void setCheckInterval(String checkInterval) {
      this.checkInterval = checkInterval;
    }

    public Duration getDowntimeInterval() {
      return parseDuration(downtimeInterval, DEFAULT_DOWNTIME_INTERVAL);
    }

    public void setDowntimeInterval(String downtimeInterval) {
      this.downtimeInterval = downtimeInterval;
    }

    public Duration getInitialDelay() {
      return parseDuration(initialDelay, getCheckInterval());
    }

    public void setInitialDelay(String initialDelay) {
      this.initialDelay = initialDelay;
    }

    public int getFailureAttempts() {
      return failureAttempts;
    }

    public void setFailureAttempts(int failureAttempts) {
      this.failureAttempts = failureAttempts;
    }

    public int getSuccessAttempts() {
      return successAttempts;
    }

    public void setSuccessAttempts(int successAttempts) {
      this.successAttempts = successAttempts;
    }

    private static Duration parseDuration(String value, Duration defaultValue) {
      if (value == null || value.isBlank()) {
        return defaultValue;
      }
      return DropwizardDurationParser.parse(value);
    }
  }
}
