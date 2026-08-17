/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;

import org.junit.jupiter.api.AfterEach;

import static com.sonatype.insight.brain.security.FipsTestUtil.enableFipsMode;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

public class UserTokenRealmFIPSTest
    extends UserTokenRealmTest
{
  @AfterEach
  @Override
  public void afterTest() {
    super.afterTest();

    // Remove the Bouncy Castle FIPS provider after the test; the parent afterTest() accesses providers.
    removeBouncyCastleFipsProvider();
  }

  @Override
  public TemporaryEntity createTemporaryEntity() {
    // Enable FIPS mode (insert the BouncyCastle FIPS provider + set FIPS_MODE_ENABLED) before the Spring
    // context and TemporaryEntity are created.
    enableFipsMode();

    return super.createTemporaryEntity();
  }
}
