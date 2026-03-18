/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.user;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_MANAGEMENT_PAGES;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.NXIQ_ENABLE_SSO_ONLY_ENV_VAR;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

public class MtiqUserManagementFIPSTest
    extends MtiqUserManagementTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  @Override
  public void setupKeycloakUsers() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    // FIPS mode disables SSO-only by default (due to FIPSModeDetector.isEnabled() check), but we want
    // consistent login behavior across FIPS and non-FIPS tests
    environmentVariables.set(NXIQ_ENABLE_SSO_ONLY_ENV_VAR, "true");
    // FIPS mode enables USER_MANAGEMENT_PAGES by default (due to FIPSModeDetector.isEnabled() check),
    // which changes frontend API selection and user data format. Force it too false to ensure
    // consistent multi-tenant user management behavior and proper delete button logic.
    tempEntity.newSystemConfigurationProperty(USER_MANAGEMENT_PAGES, "false");

    super.setupKeycloakUsers();
  }

  @After
  @Override
  public void cleanup() {
    // Ensure that the Bouncy Castle FIPS provider is removed after the tests as
    // some providers are accessed in the afterTest parent method.
    removeBouncyCastleFipsProvider();

    super.cleanup();
  }
}
