/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

/**
 * Thrown when an insert or update fails because the supplied name is already taken. Distinct
 * subtype of {@link InvalidNameException} so callers can distinguish duplicate names from other
 * name-validation failures (length, blank) via a {@code catch} branch rather than a message check.
 */
@SuppressWarnings("serial")
public class DuplicateNameException
    extends InvalidNameException
{
  public DuplicateNameException(String message) {
    super(message);
  }

  public DuplicateNameException(String message, Throwable cause) {
    super(message, cause);
  }
}
