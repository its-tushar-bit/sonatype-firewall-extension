/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.security.FIPSConfig.isFipsEnabledByEnvironment;
import static com.sonatype.insight.brain.security.FIPSConfig.isFipsModeEnabledVariableSet;

public class FIPSModeDetector
{
  private static final Logger log = LoggerFactory.getLogger(FIPSModeDetector.class);

  private FIPSModeDetector() {
    // no-op. utility class
  }

  /**
   * This method attempts to detect if FIPS mode is enabled. <br />
   * <br />
   * This is controlled by
   * environment variable {@link FIPSConfig#FIPS_MODE_ENABLED_ENV}.
   *
   * @return true if we detect that FIPS mode is enabled by environment variable, false if we detect
   *         that FIPS mode is disabled.
   */
  public static boolean isEnabled() {
    if (isFipsModeEnabledVariableSet()) {
      if (isFipsEnabledByEnvironment()) {
        log.trace("FIPS mode is enabled through environment variable FIPS_MODE_ENABLED");
        return true;
      }
      log.trace("FIPS mode is disabled through environment variable FIPS_MODE_ENABLED");
      return false;
    }
    return false;
  }
}
