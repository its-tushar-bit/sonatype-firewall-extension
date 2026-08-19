/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

public class SearchIndexException
    extends RuntimeException
{
  public SearchIndexException(final Exception e) {
    super(e);
  }

  /**
   * Wraps a non-{@link Exception} {@link Throwable} cause. Used to convert a JVM memory-mapped read
   * fault (an {@link InternalError} surfaced as "unsafe memory access") raised during a Lucene index
   * read into a per-request search failure, keeping the {@link Error} chained as the cause instead
   * of letting it escape the request thread and trip the JVM's automatic-shutdown-on-fatal-error
   * handler (CLM-44515).
   */
  public SearchIndexException(final Throwable cause) {
    super(cause);
  }

  public SearchIndexException(final String message, final Exception e) {
    super(message, e);
  }
}
