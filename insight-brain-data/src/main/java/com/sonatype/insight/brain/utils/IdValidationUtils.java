/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.regex.Pattern;

import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.15.0
 */
public class IdValidationUtils
{
  private static final Pattern pattern = Pattern.compile("^[-_A-Za-z0-9]*");

  /**
   * Validate that the given value matched the pattern to prevent path traversal attacks
   */
  public static void validate(final String value) {
    if (!pattern.matcher(value).matches()) {
      throw new BadRequestException("Invalid value: " + value);
    }
  }
}
