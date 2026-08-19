/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Test helper that mutates and restores the process-wide
 * {@link GlobalSearchCursor#currentGenerationToken()}. The generation token is global mutable state, so a
 * test that calls {@link GlobalSearchCursor#bumpGenerationToken(String)} must restore it before another
 * test runs in the same JVM.
 */
public final class GlobalSearchCursorTokenTestSupport
{
  private GlobalSearchCursorTokenTestSupport() {
  }

  /**
   * Save the current generation token, bump to {@code transientToken}, run {@code body}, restore. The
   * restore is guaranteed even when {@code body} throws.
   */
  public static void withGenerationToken(final String transientToken, final Runnable body) {
    String original = GlobalSearchCursor.currentGenerationToken();
    GlobalSearchCursor.bumpGenerationToken(transientToken);
    try {
      body.run();
    }
    finally {
      GlobalSearchCursor.bumpGenerationToken(original);
    }
  }
}
