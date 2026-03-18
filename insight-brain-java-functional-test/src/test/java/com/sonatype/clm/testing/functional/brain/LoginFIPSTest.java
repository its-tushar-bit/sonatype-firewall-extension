/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.security.FIPSConfig;
import com.sonatype.insight.brain.security.SamlDeploymentManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

public class LoginFIPSTest
    extends LoginTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  @Override
  public void clearSamlDeployment() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the updateFromConfiguration is called.
    insertBouncyCastleFipsProvider();

    // Set the environment variable to enable FIPS mode.
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");

    testCLMServer.getCLMServer().getInstance(SamlDeploymentManager.class).updateFromConfiguration();
  }

  @After
  @Override
  public void afterTest() throws Exception {
    super.afterTest();

    // Ensure that the Bouncy Castle FIPS provider is removed after the tests as
    // some providers are accessed in the afterTest parent method.
    removeBouncyCastleFipsProvider();
  }
}
