/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Thrown when a caller exceeds the per-user concurrency budget for a Global Search endpoint
 * ({@code /rest/search/results} or {@code /rest/search/suggest}). Both endpoints share this 429 path.
 * Mapped to HTTP 429 by {@link RateLimitedExceptionMapper}.
 */
public class RateLimitedException
    extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  public RateLimitedException(String message) {
    super(message);
  }
}
