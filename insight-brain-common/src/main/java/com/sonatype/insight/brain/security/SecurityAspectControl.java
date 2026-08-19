/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Static control for CTW security aspects.
 *
 * <p>
 * Compile-time-woven (CTW) aspects are embedded in the bytecode and fire on every method call,
 * even when the caller holds a direct reference to the target bean (i.e., not through a proxy).
 * Tests that bypass Spring proxies therefore need a way to disable security enforcement.
 * </p>
 *
 * <p>
 * Call {@link #disableEnforcement()} in test setup and {@link #enableEnforcement()} in teardown.
 * Production code should never call these methods — the default is always <em>enabled</em>.
 * </p>
 *
 * <p>
 * Uses a JVM-wide static flag (not thread-local) so that enforcement is disabled across all threads
 * during a test method, including thread pools used by libraries like Awaitility.
 * </p>
 */
public final class SecurityAspectControl
{
  private static final AtomicBoolean DISABLED = new AtomicBoolean(false);

  private static final boolean TEST_FRAMEWORK_PRESENT = isTestFrameworkPresent();

  private SecurityAspectControl() {
    // utility class
  }

  private static boolean isTestFrameworkPresent() {
    try {
      Class.forName("org.junit.Test");
      return true;
    }
    catch (ClassNotFoundException e) {
      // fall through
    }
    try {
      Class.forName("org.junit.jupiter.api.Test");
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Disable CTW security aspect enforcement JVM-wide.
   * Aspects will skip authorization checks and proceed directly to the target method.
   * Only works when a test framework is on the classpath; throws in production.
   */
  public static void disableEnforcement() {
    if (!TEST_FRAMEWORK_PRESENT) {
      throw new IllegalStateException("Security aspect enforcement can only be disabled in test environments");
    }
    DISABLED.set(true);
  }

  /**
   * Re-enable CTW security aspect enforcement JVM-wide (the default state).
   */
  public static void enableEnforcement() {
    DISABLED.set(false);
  }

  /**
   * Returns {@code true} if enforcement is currently disabled.
   */
  public static boolean isEnforcementDisabled() {
    return DISABLED.get();
  }
}
