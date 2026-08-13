/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

/**
 * Runs the full SAML response-consumption suite (key generation, RSA-SHA256 signing and OpenSAML5 signature
 * verification) with the Bouncy Castle FIPS provider active, guarding against crypto-library regressions in
 * FIPS mode after the Keycloak&rarr;Spring Security SAML migration (CLM-42790).
 */
public class SpringSamlResponseConsumptionFIPSTest
    extends SpringSamlResponseConsumptionTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @Override
  @BeforeEach
  public void generateIdpKeys() throws Exception {
    // Insert the Bouncy Castle FIPS provider and enable FIPS mode before any keys or signatures are created.
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    super.generateIdpKeys();
  }

  @AfterEach
  public void removeFipsProvider() {
    removeBouncyCastleFipsProvider();
    environmentVariables.restore();
  }
}
