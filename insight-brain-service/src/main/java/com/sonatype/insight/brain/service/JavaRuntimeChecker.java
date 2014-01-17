/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether the current JRE is supported and exits the process if not.
 */
class JavaRuntimeChecker
{
  private static Logger log = LoggerFactory.getLogger(JavaRuntimeChecker.class);

  static final String PROP_DISABLE = "clm.disableJreCheck";

  public static void checkJreIsSupported() {
    String vendor = System.getProperty("java.vendor");
    if (!isSupportedJre(vendor)) {
      logError();
      System.exit(1);
    }
  }

  static boolean isSupportedJre(String vendor) {
    if (Boolean.getBoolean(PROP_DISABLE)) {
      return true;
    }
    return vendor != null && vendor.toLowerCase(Locale.ENGLISH).contains("oracle");
  }

  private static void logError() {
    log.error("===============================================================================");
    log.error("The CLM server appears to be run using an unsupported Java runtime:");
    log.error("  Home:    {}", System.getProperty("java.home"));
    log.error("  Vendor:  {}", System.getProperty("java.vendor"));
    log.error("  Version: {}", System.getProperty("java.version"));
    log.error("Please verify you launched the server using an Oracle Java runtime as detailed");
    log.error("in the accompanying README. If you have troubles setting up the required Java");
    log.error("runtime, feel free to contact Sonatype support.");
    log.error("===============================================================================");
  }
}
