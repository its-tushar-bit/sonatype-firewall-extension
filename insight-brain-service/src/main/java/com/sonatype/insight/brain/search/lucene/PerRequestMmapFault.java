/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.jaxrs.error.NonFatalRequestFault;

/**
 * A per-request search failure raised when a memory-mapped Lucene index read hits a mapped-page
 * SIGBUS, which HotSpot surfaces as an {@link InternalError} ("a fault occurred in an unsafe memory
 * access operation"). By implementing {@link NonFatalRequestFault} it opts the wrapped
 * {@link InternalError} out of the JVM's automatic-shutdown-on-fatal-error handling
 * ({@code JavaLangErrorHandler}), so a user-supplied advanced-search query over a corrupt or
 * truncated segment fails the offending request with an HTTP 5xx instead of terminating the process
 * (DoS -- CLM-44515). The {@link InternalError} is kept as the chained cause for diagnostics.
 * <p>
 * The marker is only ever applied to a fault this class has matched via
 * {@link #isUnsafeMemoryAccessFault}, and only at the Lucene search-read boundary
 * ({@link SearchMmapFaultAspect}). Any other {@link InternalError} -- including one from the Lucene
 * writer/merge path or any non-search mmap/Unsafe consumer -- is never wrapped and stays fatal.
 */
public class PerRequestMmapFault
    extends SearchIndexException
    implements NonFatalRequestFault
{
  PerRequestMmapFault(final InternalError cause) {
    super(cause);
  }

  /**
   * Detects the memory-mapped read fault HotSpot surfaces as an {@link InternalError}. The current
   * wording is either "a fault occurred in an unsafe memory access operation" (interpreted) or "a
   * fault occurred in a recent unsafe memory access operation in compiled Java code" (JIT-compiled);
   * both contain the substring {@code "unsafe memory access"}. The match is message-only: it is
   * scoped to the search-read path by where it is applied ({@link SearchMmapFaultAspect}), not by
   * sniffing the stack, which is brittle across Lucene/JIT versions. Fail-safe: a future JDK reword
   * returns {@code false} and the error is treated as fatal again (the pre-CLM-44515 behavior).
   */
  static boolean isUnsafeMemoryAccessFault(final Throwable throwable) {
    if (!(throwable instanceof InternalError)) {
      return false;
    }
    String message = throwable.getMessage();
    return message != null && message.contains("unsafe memory access");
  }
}
