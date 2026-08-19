/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

/**
 * Like IllegalArgumentException, but for when the situation is caused by bad user input and thus should be
 * checked. Also conceptually similar to BadRequestException, but not REST-specific
 */
public class CheckedIllegalArgumentException
    extends Exception
{
  public CheckedIllegalArgumentException(final String message) {
    super(message);
  }

  public CheckedIllegalArgumentException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
