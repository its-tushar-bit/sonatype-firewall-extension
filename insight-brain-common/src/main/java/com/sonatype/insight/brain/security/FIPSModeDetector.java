/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import static com.sonatype.insight.brain.security.FIPSConfig.isFipsEnabledByEnvironment;
import static com.sonatype.insight.brain.security.FIPSConfig.isFipsModeEnabledVariableSet;

public class FIPSModeDetector
{
  private static final Logger log = LoggerFactory.getLogger(FIPSModeDetector.class);

  private static final String LINUX_FIPS_ENABLED_PATH = "/proc/sys/crypto/fips_enabled";

  private static final String WINDOWS_REGISTRY_FIPS_KEY =
      "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Lsa\\FIPSAlgorithmPolicy";

  private FIPSModeDetector() {
    // no-op. utility class
  }

  /**
   * This method attempts to detect if FIPS mode is enabled on the host OS. <br /><br /> This is either forced by
   * environment variable {@link FIPSConfig#FIPS_MODE_ENABLED_ENV} or by host detection. Currently, only Linux and
   * Windows are supported, detection is based on
   * <ahref="https://learn.microsoft.com/en-us/azure/aks/enable-fips-nodes">this</a>,
   * and is not guaranteed to work.
   *
   * @return true if we detect that FIPS mode is enabled by environment variable or on the host OS, false if we detect
   * that FIPS mode is disabled.
   */
  public static boolean isEnabled() {
    // before anything check if we are forcing FIPS mode
    if (isFipsModeEnabledVariableSet()) {
      if (isFipsEnabledByEnvironment()) {
        log.debug("FIPS mode is enabled through environment variable FIPS_MODE_ENABLED");
        return true;
      }
      log.debug("FIPS mode is disabled through environment variable FIPS_MODE_ENABLED");
      return false;
    }
    // if we are not forcing FIPS mode, check if it is enabled by the OS
    return isEnabledByOS();
  }

  // Visible for testing
  static boolean isEnabledByOS() {
    if (isLinux()) {
      return isEnabledOnLinux();
    }
    if (isWindows()) {
      return isEnabledOnWindows();
    }
    return false;
  }

  // Visible for testing
  static boolean isLinux() {
    return SystemUtils.IS_OS_LINUX;
  }

  // Visible for testing
  static boolean isWindows() {
    return SystemUtils.IS_OS_WINDOWS;
  }

  // Visible for testing
  static String getLinuxFIPSEnabledPath() {
    return LINUX_FIPS_ENABLED_PATH;
  }

  // Visible for testing
  static List<String> executeCommand(final String... command)
      throws IOException, InterruptedException, TimeoutException
  {
    return new ProcessExecutor()
        .command(command)
        .readOutput(true)
        .execute()
        .getOutput()
        .getLines();
  }

  private static boolean isEnabledOnLinux() {
    Path path = Path.of(getLinuxFIPSEnabledPath());
    try {
      String content = Files.readString(path).trim();
      if (content.equals("1")) {
        log.debug("FIPS mode is enabled on the Linux OS level");
        return true;
      }
      if (content.equals("0")) {
        return false;
      }
    }
    catch (Exception e) {
      log.debug("Unable to detect FIPS mode.", e);
    }
    return false;
  }

  private static boolean isEnabledOnWindows() {
    try {
      String output = executeCommand("reg", "query", WINDOWS_REGISTRY_FIPS_KEY).stream()
          .map(String::trim)
          .collect(Collectors.joining());
      if (output.contains(WINDOWS_REGISTRY_FIPS_KEY)) {
        if (output.contains("0x1")) {
          log.debug("FIPS mode is enabled on the Windows OS level");
          return true;
        }
        if (output.contains("0x0")) {
          return false;
        }
      }
    }
    catch (Exception e) {
      log.debug("Unable to detect FIPS mode.", e);
    }
    return false;
  }
}
