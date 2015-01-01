/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether the -XX:MaxPermSize JVM argument is configured to at least 128MB on Java 7 and exits the process if
 * not. In Java 8 this is no-op since -XX:MaxPermSize is no longer needed.
 * 
 * @since 1.12
 */
class JavaXXMaxPermSizeChecker
{
  private static Logger log = LoggerFactory.getLogger(JavaXXMaxPermSizeChecker.class);

  static final long MEGABYTE_IN_BYTES = 1024L * 1024;

  static final String PROP_DISABLE = "clm.disableMaxPermSizeCheck";

  public static void check() {
    String javaVersionString = System.getProperty("java.version");
    Long maxPermGenInBytes = getMaxPermGenInBytes();
    if (!isValid(javaVersionString, maxPermGenInBytes)) {
      logError(maxPermGenInBytes);
      System.exit(1);
    }
  }

  static boolean isValid(String javaVersionString, Long maxPermGenInBytes) {
    if (Boolean.getBoolean(PROP_DISABLE)) {
      log.info("The check for -XX:MaxPermSize was disabled using the {} property.", PROP_DISABLE);
      return true;
    }

    int javaVersion = getJavaVersionFromString(javaVersionString);
    if (javaVersion >= 8) {
      return true;
    }

    if (maxPermGenInBytes == null) {
      log.error("Cannot get the value for max Perm Gen size.");
      return false;
    }

    return maxPermGenInBytes >= 128L * MEGABYTE_IN_BYTES;
  }

  private static Long getMaxPermGenInBytes() {
    for (MemoryPoolMXBean memoryPoolMXBean : ManagementFactory.getMemoryPoolMXBeans()) {
      String memoryPoolName = memoryPoolMXBean.getName();
      if (memoryPoolName == null || !memoryPoolName.endsWith("Perm Gen")) {
        log.trace("Ignoring MemoryPoolMXBean with name '{}'.", memoryPoolName);
        continue;
      }
      
      log.trace("Found MemoryPoolMXBean with name '{}'.", memoryPoolName);
      long maxPermGenInBytes = memoryPoolMXBean.getUsage().getMax();
      log.trace("Found maxPermGenInBytes={}.", maxPermGenInBytes);
      if (maxPermGenInBytes < 0) {
        // The max value for Perm Gen is not defined
        return null;
      }

      return maxPermGenInBytes;
    }

    return null;
  }

  static int getJavaVersionFromString(String javaVersionString) {
    if (javaVersionString == null) {
      throw new RuntimeException("The java.version property is null.");
    }

    String[] versionElements = javaVersionString.split("\\.");
    if (versionElements == null || versionElements.length < 2) {
      throw new RuntimeException("Cannot parse the java.version property '" + javaVersionString + "'.");
    }

    try {
      int javaVersion = Integer.parseInt(versionElements[1]);
      return javaVersion;
    }
    catch (NumberFormatException e) {
      throw new RuntimeException("Cannot parse the java.version property '" + javaVersionString + "'.", e);
    }
  }

  private static void logError(Long maxPermSize) {
    log.error("===============================================================================================");
    log.error("The CLM server appears to be running with not enough memory allocated for permanent generation.");
    log.error("  Java Vendor:  {}", System.getProperty("java.vendor"));
    log.error("  Java Version: {}", System.getProperty("java.version"));
    log.error("  Detected MaxPermSize: {} bytes ({} MB)", maxPermSize, maxPermSize == null ? "null" : maxPermSize
        / MEGABYTE_IN_BYTES);
    log.error("Please verify you launched the server with at least 128MB memory for permanent generation,");
    log.error("by using -XX:MaxPermSize=128m as JVM argument.");
    log.error("===============================================================================================");
  }
}
