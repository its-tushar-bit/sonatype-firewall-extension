/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static org.assertj.core.api.Assertions.assertThat;

public class FIPSModeDetectorTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

  @Test
  public void testIsEnabled_EnvironmentVariable() {
    // default to null nothing is set
    assertThat(FIPSModeDetector.isEnabled()).isFalse();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "notaboolean");
    assertThat(FIPSModeDetector.isEnabled()).isFalse();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    assertThat(FIPSModeDetector.isEnabled()).isTrue();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
    assertThat(FIPSModeDetector.isEnabled()).isFalse();
  }
}
