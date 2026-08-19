/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;

public class CipherFactoryTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @Test
  public void testCreateCipher_FipsModeEnabled() {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    Object cipher = CipherFactory.createCipher();
    assertThat(cipher).isInstanceOf(FipsCipher.class);

    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testCreateCipher_FipsModeDisabled() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");

    Object cipher = CipherFactory.createCipher();
    assertThat(cipher).isInstanceOf(DefaultPlexusCipher.class);
  }

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }
}
