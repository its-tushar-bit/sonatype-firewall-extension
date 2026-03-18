/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.security.FIPSConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiSamlConfigurationResourceFIPSTest
    extends ApiSamlConfigurationResourceTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void init() throws Exception {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Set the environment variable to enable FIPS mode.
    environmentVariables.set(FIPSConfig.FIPS_MODE_ENABLED_ENV, "true");
  }

  @After
  @Override
  public void cleanup() {
    super.cleanup();

    removeBouncyCastleFipsProvider();
  }
}
