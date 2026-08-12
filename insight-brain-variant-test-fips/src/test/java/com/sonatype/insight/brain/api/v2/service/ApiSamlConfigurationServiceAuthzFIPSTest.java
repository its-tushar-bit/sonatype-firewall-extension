/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;

import org.junit.After;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

public class ApiSamlConfigurationServiceAuthzFIPSTest
    extends ApiSamlConfigurationServiceAuthzTest
{
  @Rule
  public EnvironmentVariables environmentVariables;

  @After
  @Override
  public void afterTest() {
    super.afterTest();

    // Ensure that the Bouncy Castle FIPS provider is removed after the tests as
    // some providers are accessed in the afterTest parent method.
    removeBouncyCastleFipsProvider();
  }

  @Override
  public TemporaryEntity createTemporaryEntity() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the TemporaryEntity is created.
    insertBouncyCastleFipsProvider();

    // initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables = new EnvironmentVariables();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    return super.createTemporaryEntity();
  }
}
