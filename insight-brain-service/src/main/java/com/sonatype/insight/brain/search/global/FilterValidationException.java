/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Thrown when a {@code sort} value or paging combination is rejected. Mapped to HTTP 400. The message
 * is for server logs only; the response body must NOT echo user input — clients key off the
 * machine-readable {@link #getCode()} instead.
 */
public class FilterValidationException
    extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  /**
   * Machine-readable rejection reason surfaced in the response body. Adding a value is
   * backward-compatible; renaming or removing one is not.
   */
  public enum Code
  {
    SORT_NOT_ALLOWED,

    /** Offset exceeds the page+pageSize threshold; caller must use {@code searchAfter}. */
    DEEP_PAGINATION_NOT_SUPPORTED,

    INVALID_FILTER,

    /** Generic fall-back; new callers must pick a specific value. */
    FILTER_VALIDATION_FAILED
  }

  private final Code code;

  public FilterValidationException(final Code code, final String message) {
    super(message);
    this.code = code == null ? Code.FILTER_VALIDATION_FAILED : code;
  }

  /**
   * @deprecated Prefer {@link #FilterValidationException(Code, String)} so the response body carries a specific code.
   */
  @Deprecated
  public FilterValidationException(final String message) {
    this(Code.FILTER_VALIDATION_FAILED, message);
  }

  public Code getCode() {
    return code;
  }
}
