/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

public class FIPSModeDetectorTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  private static final String LINUX_FIPS_MODE_ENABLED_CONTENT = "1";

  private static final String LINUX_FIPS_MODE_DISABLED_CONTENT = "0";

  private static final List<String> WINDOWS_FIPS_MODE_ENABLED_OUTPUT = List.of("\r\n",
      "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy\r\n",
      "    Enabled    REG_DWORD    0x1\r\n",
      "\r\n");

  private static final List<String> WINDOWS_FIPS_MODE_DISABLED_OUTPUT = List.of("\r\n",
      "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy\r\n",
      "    Enabled    REG_DWORD    0x0\r\n",
      "\r\n");

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testIsEnabled_Linux_Enabled() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.writeString(path, LINUX_FIPS_MODE_ENABLED_CONTENT);

    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(true);
      mocked.when(FIPSModeDetector::getLinuxFIPSEnabledPath).thenReturn(path.toString());

      assertThat(FIPSModeDetector.isEnabled()).isTrue();
    }
  }

  @Test
  public void testIsEnabled_Linux_Disabled() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.writeString(path, LINUX_FIPS_MODE_DISABLED_CONTENT);

    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(true);
      mocked.when(FIPSModeDetector::getLinuxFIPSEnabledPath).thenReturn(path.toString());

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_Linux_FileDoesNotExist() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(true);
      mocked.when(FIPSModeDetector::getLinuxFIPSEnabledPath).thenReturn("doesNotExist");

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_Linux_FileHasNoContent() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(true);
      mocked.when(FIPSModeDetector::getLinuxFIPSEnabledPath).thenReturn(path.toString());

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_Linux_FileHasWrongContent() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.writeString(path, "wrongContent");
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(true);
      mocked.when(FIPSModeDetector::getLinuxFIPSEnabledPath).thenReturn(path.toString());

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_Windows_Enabled() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(true);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(false);
      mocked.when(() -> FIPSModeDetector.executeCommand(
          "reg",
          "query",
          "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
      )).thenReturn(WINDOWS_FIPS_MODE_ENABLED_OUTPUT);

      assertThat(FIPSModeDetector.isEnabled()).isTrue();
    }
  }

  @Test
  public void testIsEnabled_Windows_Disabled() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(true);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(false);
      mocked.when(() -> FIPSModeDetector.executeCommand(
          "reg",
          "query",
          "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
      )).thenReturn(WINDOWS_FIPS_MODE_DISABLED_OUTPUT);

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_Windows_CommandHasNoOutput() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(true);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(false);
      mocked.when(() -> FIPSModeDetector.executeCommand(
          "reg",
          "query",
          "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
      )).thenReturn(List.of());

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_Windows_CommandHasWrongOutput() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(true);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(false);
      mocked.when(() -> FIPSModeDetector.executeCommand(
          "reg",
          "query",
          "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
      )).thenReturn(List.of("wrongOutput"));

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_OtherOS() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(false);

      assertThat(FIPSModeDetector.isEnabled()).isFalse();
    }
  }

  @Test
  public void testIsEnabled_EnvironmentVariable() {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      mocked.when(FIPSModeDetector::isWindows).thenReturn(false);
      mocked.when(FIPSModeDetector::isLinux).thenReturn(false);

      // default to null nothing is set
      assertThat(FIPSModeDetector.isEnabled()).isFalse();
      mocked.verify(FIPSModeDetector::isLinux);
      mocked.verify(FIPSModeDetector::isWindows);

      environmentVariables.set(FIPS_MODE_ENABLED_ENV, "notaboolean");
      assertThat(FIPSModeDetector.isEnabled()).isFalse();

      // verify we did call isLinux or isWindows again as FIPS is not forced
      mocked.verify(FIPSModeDetector::isLinux, times(2));
      mocked.verify(FIPSModeDetector::isWindows, times(2));

      environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
      assertThat(FIPSModeDetector.isEnabled()).isTrue();
      // verify we didn't call isLinux or isWindows again as FIPS is forced to be enabled
      mocked.verify(FIPSModeDetector::isLinux, times(2));
      mocked.verify(FIPSModeDetector::isWindows, times(2));

      environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
      assertThat(FIPSModeDetector.isEnabled()).isFalse();

      // verify we didn't call isLinux or isWindows again as FIPS is forced to be disabled
      mocked.verify(FIPSModeDetector::isLinux, times(2));
      mocked.verify(FIPSModeDetector::isWindows, times(2));
    }
  }
}
