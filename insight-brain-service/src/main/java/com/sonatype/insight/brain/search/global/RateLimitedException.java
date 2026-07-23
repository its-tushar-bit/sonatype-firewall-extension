/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Thrown when a caller exceeds the per-user concurrency budget for {@code /rest/search/results}.
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
