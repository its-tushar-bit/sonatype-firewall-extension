/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import com.sonatype.insight.error.HttpStatusCode;

/**
 * Thrown when a {@code sort} value or paging combination is rejected. Mapped to HTTP 400 by
 * {@link FilterValidationExceptionMapper}, which serializes only {@code {code, message}} where
 * {@code message} is the static {@link Code#clientMessage()}. Clients key off {@link #getCode()}.
 * The {@code @HttpStatusCode} is a fallback status only for the (unregistered-mapper) edge case.
 *
 * <p>
 * {@link #getMessage()} deliberately returns only the static {@link Code#clientMessage()}, never
 * the caller-supplied {@code detail}: the generic error fallback echoes {@code getMessage()} into the
 * body, so any user input must be kept off it. Server-side, the input-bearing {@code detail} is
 * available via {@link #getDetail()} for logging only.
 */
@HttpStatusCode(400)
public class FilterValidationException
    extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  /**
   * Machine-readable rejection reason surfaced in the response body. Adding a value is
   * backward-compatible; renaming or removing one is not. Each carries a static, input-free client
   * message; {@code getMessage()} returns exactly this text so nothing user-supplied can leak.
   */
  public enum Code
  {
    SORT_NOT_ALLOWED("The requested sort is not allowed for this entity type."),

    /** Offset exceeds the page+pageSize threshold; caller must use {@code searchAfter}. */
    DEEP_PAGINATION_NOT_SUPPORTED("Paging beyond the first page requires a searchAfter cursor."),

    INVALID_FILTER("One or more filter values are invalid."),

    /** Generic fall-back; new callers must pick a specific value. */
    FILTER_VALIDATION_FAILED("The request could not be validated.");

    private final String clientMessage;

    Code(final String clientMessage) {
      this.clientMessage = clientMessage;
    }

    /** Static, input-free message safe to return in the response body. */
    public String clientMessage() {
      return clientMessage;
    }
  }

  private final Code code;

  /** Caller-supplied, possibly user-valued text for server logs only; never reaches the wire body. */
  private final transient String detail;

  public FilterValidationException(final Code code, final String detail) {
    // Super message is the static client message, never the caller-supplied detail: the generic error
    // fallback echoes getMessage() to the body, so raw input must not reach it.
    super((code == null ? Code.FILTER_VALIDATION_FAILED : code).clientMessage());
    this.code = code == null ? Code.FILTER_VALIDATION_FAILED : code;
    this.detail = detail;
  }

  public Code getCode() {
    return code;
  }

  /** Input-bearing detail for server-side logging only; MUST NOT be returned in the response body. */
  public String getDetail() {
    return detail;
  }
}
