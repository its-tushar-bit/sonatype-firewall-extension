/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

@Ignore("CLM-35281")
public class MultiTenantAdministratorsFIPSTest
    extends MultiTenantAdministratorsTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  @Override
  public void startup() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    super.startup();
  }

  @After
  @Override
  public void prepareToLeavePage() {
    super.prepareToLeavePage();

    // Ensure that the Bouncy Castle FIPS provider is removed after the tests as
    // some providers are accessed in the afterTest parent method.
    removeBouncyCastleFipsProvider();
  }
}
