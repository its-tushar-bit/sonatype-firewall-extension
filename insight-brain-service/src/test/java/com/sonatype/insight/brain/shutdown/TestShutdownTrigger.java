/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import com.sonatype.insight.error.exception.BadRequestException;
import java.time.Duration;

/**
 * Test-only bridge to {@link ShutdownHandler}'s package-private {@code trigger}. Lets the integration-test
 * infrastructure run the real ordered shutdown (the same path production uses) when a reused-fork test context
 * closes, without exiting the JVM. This is how registered executors/threads get shut down in tests; in production
 * the same shutdown runs via {@code ShutdownTask}.
 */
public final class TestShutdownTrigger
{
  private TestShutdownTrigger() {
  }

  public static void triggerForTest(final ShutdownHandler shutdownHandler, final Duration timeout) {
    // trigger() is synchronized and rejects a second call with BadRequestException. The isTriggered() check skips the
    // redundant call in the normal (single close) case; the catch makes it safe even if a concurrent close races in.
    try {
      if (!shutdownHandler.isTriggered()) {
        shutdownHandler.trigger(timeout, true);
      }
    }
    catch (BadRequestException alreadyTriggered) {
      // Another close already ran the ordered shutdown; nothing more to do.
    }
  }
}
