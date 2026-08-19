/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Signals that a per-component input in a remediation request is invalid — a malformed identifier or
 * package URL, or a component that the data source does not recognise. Distinct from a generic
 * {@link BadRequestException} so the bulk endpoint can turn it into a per-item error while letting
 * batch-level {@code BadRequestException}s propagate as HTTP 400.
 * <p>
 * Because this class {@code extends BadRequestException}, callers of the single-component endpoint
 * (and anything else that catches or maps {@code BadRequestException}) continue to see the same HTTP
 * 400 behaviour they saw before this class existed.
 *
 * @since 1.205
 */
public class InvalidComponentException
    extends BadRequestException
{
  public InvalidComponentException(String message) {
    super(message);
  }

  public InvalidComponentException(String message, Throwable cause) {
    super(message, cause);
  }
}
