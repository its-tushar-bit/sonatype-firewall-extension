/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import com.google.inject.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binary-compatibility bridge for legacy test fixtures that still reference the historical Guice-based
 * injected test base from already-built sibling test artifacts.
 *
 * <p>
 * The Spring migration replaced Guice-backed test injection with {@link SpringInjectedTest}, but some
 * installed test JARs are still compiled against {@code GuiceInjectedTest}. Keeping this compatibility shim
 * allows class-scoped downstream test compilation to load those artifacts without resurrecting the legacy
 * Guice harness.
 * </p>
 */
@Deprecated
public abstract class GuiceInjectedTest
    extends SpringInjectedTest
{
  private static final Logger log = LoggerFactory.getLogger(GuiceInjectedTest.class);

  /**
   * Binary-compatibility stub only - the returned Module is NOT applied to the Spring context.
   * Tests that override this must be migrated to use {@code @TestConfiguration} inner classes or
   * {@code getTestConfigurationClasses()} overrides instead.
   */
  protected Module getOverrideModule() {
    return null;
  }

  // No-op setUp/tearDown retained so INVOKESPECIAL from pre-compiled test JARs resolves without NoSuchMethodError
  public void setUp() throws Exception {
    if (getOverrideModule() != null) {
      log.warn("{} overrides getOverrideModule() but the returned Module is ignored in Spring mode"
          + " - migrate to @TestConfiguration", getClass().getName());
    }
  }

  public void tearDown() throws Exception {
  }
}
