/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiSamlConfigurationResourceAuditFIPSTest
    extends ApiSamlConfigurationResourceAuditTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  @Override
  public void before() throws IOException {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    super.before();
  }

  @After
  public void tearDown() {
    removeBouncyCastleFipsProvider();
  }
}
