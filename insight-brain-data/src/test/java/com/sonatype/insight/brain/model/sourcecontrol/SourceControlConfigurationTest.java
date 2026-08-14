/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.time.DateTimeException;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SourceControlConfigurationTest
{
  @Test
  public void testSourceControlConfiguration_InitialValues() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    assertThat(sourceControlConfiguration.getCloneDirectory()).isEqualTo(
        SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR);
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isNull();
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isNull();
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours()).isEqualTo(
        SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(
        SourceControlConfiguration.DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
    assertThat(sourceControlConfiguration.getGpgSigningKey()).isNull();
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNull();
  }

  @Test
  public void testGetSetDefaultBranchMonitoringStartTime() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    // Initial values
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isNull();
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isNull();

    // Set the String to null
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString(null);
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isNull();
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isNull();

    // Set the String
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString("1:11");
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo("1:11");
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isEqualTo(LocalTime.of(1, 11));

    // Set the String (double digits)
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString("01:11");
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo("01:11");
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isEqualTo(LocalTime.of(1, 11));

    // Set the LocalTime to null
    sourceControlConfiguration.setDefaultBranchMonitoringStartTime(null);
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isNull();
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isNull();

    // Set the LocalTime
    sourceControlConfiguration.setDefaultBranchMonitoringStartTime(LocalTime.of(2, 22));
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo("2:22");
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isEqualTo(LocalTime.of(2, 22));
  }

  @Test
  public void testSetDefaultBranchMonitoringStartTimeString_BadFormat() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    assertThatExceptionOfType(DateTimeException.class).isThrownBy(
        () -> sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString("bad"));
  }

  @Test
  public void testGetSetGpgSigningKey() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    assertThat(sourceControlConfiguration.getGpgSigningKey()).isNull();

    sourceControlConfiguration.setGpgSigningKey("test-key");
    assertThat(sourceControlConfiguration.getGpgSigningKey()).isEqualTo("test-key");

    sourceControlConfiguration.setGpgSigningKey(null);
    assertThat(sourceControlConfiguration.getGpgSigningKey()).isNull();
  }

  @Test
  public void testGetSetGpgPassphrase() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNull();

    sourceControlConfiguration.setGpgPassphrase("test-passphrase");
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isEqualTo("test-passphrase");

    sourceControlConfiguration.setGpgPassphrase(null);
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNull();
  }
}
