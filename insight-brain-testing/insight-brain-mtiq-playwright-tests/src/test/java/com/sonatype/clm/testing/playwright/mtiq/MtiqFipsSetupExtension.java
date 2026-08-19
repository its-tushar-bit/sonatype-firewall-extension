/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.mtiq;

import com.sonatype.insight.brain.security.TestEnvironmentVariables;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;

/**
 * JUnit 5 replacement for the ordered {@code @Rule} stack on {@code AbstractMtiqFipsUiTest}: inserts
 * the BouncyCastle FIPS JCE provider and sets {@code FIPS_MODE_ENABLED=true} before each test, then
 * undoes both afterwards.
 *
 * <p>
 * Implemented as a {@link BeforeEachCallback} so the FIPS provider and environment variable are in
 * place before the tenant-provisioning {@code @BeforeEach} of {@link AbstractMtiqUiTest} runs (all
 * {@code BeforeEachCallback}s execute before any {@code @BeforeEach} method). The
 * {@link TestEnvironmentVariables} instance is kept in the extension {@code Store} so
 * {@link #afterEach} can restore the original environment, mirroring the system-rules
 * {@code EnvironmentVariables} rule's automatic per-test restore.
 */
public final class MtiqFipsSetupExtension
    implements BeforeEachCallback, AfterEachCallback
{
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(MtiqFipsSetupExtension.class);

  private static final String ENV_VARS = "environmentVariables";

  @Override
  public void beforeEach(final ExtensionContext context) {
    insertBouncyCastleFipsProvider();
    TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    context.getStore(NAMESPACE).put(ENV_VARS, environmentVariables);
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    TestEnvironmentVariables environmentVariables =
        context.getStore(NAMESPACE).get(ENV_VARS, TestEnvironmentVariables.class);
    if (environmentVariables != null) {
      environmentVariables.restore();
    }
    removeBouncyCastleFipsProvider();
  }
}
