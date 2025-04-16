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
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class FIPSModeDetectorTest
{
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
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(false);
    when(fipsModeDetector.isLinux()).thenReturn(true);
    when(fipsModeDetector.getLinuxFIPSEnabledPath()).thenReturn(path.toString());

    assertThat(fipsModeDetector.isEnabled()).isTrue();
  }

  @Test
  public void testIsEnabled_Linux_Disabled() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.writeString(path, LINUX_FIPS_MODE_DISABLED_CONTENT);
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(false);
    when(fipsModeDetector.isLinux()).thenReturn(true);
    when(fipsModeDetector.getLinuxFIPSEnabledPath()).thenReturn(path.toString());

    assertThat(fipsModeDetector.isEnabled()).isFalse();
  }

  @Test
  public void testIsEnabled_Linux_FileDoesNotExist() {
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(false);
    when(fipsModeDetector.isLinux()).thenReturn(true);
    when(fipsModeDetector.getLinuxFIPSEnabledPath()).thenReturn("doesNotExist");

    assertThat(fipsModeDetector.isEnabled()).isNull();
  }

  @Test
  public void testIsEnabled_Linux_FileHasNoContent() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(false);
    when(fipsModeDetector.isLinux()).thenReturn(true);
    when(fipsModeDetector.getLinuxFIPSEnabledPath()).thenReturn(path.toString());

    assertThat(fipsModeDetector.isEnabled()).isNull();
  }

  @Test
  public void testIsEnabled_Linux_FileHasWrongContent() throws Exception {
    Path path = temporaryFolder.newFile().toPath();
    Files.writeString(path, "wrongContent");
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(false);
    when(fipsModeDetector.isLinux()).thenReturn(true);
    when(fipsModeDetector.getLinuxFIPSEnabledPath()).thenReturn(path.toString());

    assertThat(fipsModeDetector.isEnabled()).isNull();
  }

  @Test
  public void testIsEnabled_Windows_Enabled() throws Exception {
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(true);
    when(fipsModeDetector.isLinux()).thenReturn(false);
    doReturn(WINDOWS_FIPS_MODE_ENABLED_OUTPUT)
        .when(fipsModeDetector)
        .executeCommand(
            "reg",
            "query",
            "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
        );

    assertThat(fipsModeDetector.isEnabled()).isTrue();
  }

  @Test
  public void testIsEnabled_Windows_Disabled() throws Exception {
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(true);
    when(fipsModeDetector.isLinux()).thenReturn(false);
    doReturn(WINDOWS_FIPS_MODE_DISABLED_OUTPUT)
        .when(fipsModeDetector)
        .executeCommand(
            "reg",
            "query",
            "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
        );

    assertThat(fipsModeDetector.isEnabled()).isFalse();
  }

  @Test
  public void testIsEnabled_Windows_CommandHasNoOutput() throws Exception {
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(true);
    when(fipsModeDetector.isLinux()).thenReturn(false);
    doReturn(List.of())
        .when(fipsModeDetector)
        .executeCommand(
            "reg",
            "query",
            "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
        );

    assertThat(fipsModeDetector.isEnabled()).isNull();
  }

  @Test
  public void testIsEnabled_Windows_CommandHasWrongOutput() throws Exception {
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(true);
    when(fipsModeDetector.isLinux()).thenReturn(false);
    doReturn(List.of("wrongOutput"))
        .when(fipsModeDetector)
        .executeCommand(
            "reg",
            "query",
            "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy"
        );

    assertThat(fipsModeDetector.isEnabled()).isNull();
  }

  @Test
  public void testIsEnabled_OtherOS() {
    FIPSModeDetector fipsModeDetector = spy(new FIPSModeDetector());
    when(fipsModeDetector.isWindows()).thenReturn(false);
    when(fipsModeDetector.isLinux()).thenReturn(false);

    assertThat(fipsModeDetector.isEnabled()).isNull();
  }
}
