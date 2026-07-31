/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.mtiq;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExternalResource;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

/**
 * FIPS-mode variant of {@link AbstractMtiqUiTest}: inserts the BouncyCastle FIPS JCE provider
 * and sets {@code FIPS_MODE_ENABLED=true} before each test's tenant provisioning runs.
 *
 * <p>
 * {@code order = Integer.MIN_VALUE} / {@code Integer.MIN_VALUE + 1} ensures the rules wrap the
 * test statement before {@code @Before} invokes {@code provisionTenant()}. Subclasses do not
 * need to manage the provider lifecycle.
 */
public abstract class AbstractMtiqFipsUiTest
    extends AbstractMtiqUiTest
{
  @Rule(order = Integer.MIN_VALUE)
  public final ExternalResource fipsRule = new ExternalResource()
  {
    @Override
    protected void before() {
      insertBouncyCastleFipsProvider();
    }

    @Override
    protected void after() {
      removeBouncyCastleFipsProvider();
    }
  };

  @Rule(order = Integer.MIN_VALUE + 1)
  public final EnvironmentVariables fipsEnvVars =
      new EnvironmentVariables().set(FIPS_MODE_ENABLED_ENV, "true");
}
