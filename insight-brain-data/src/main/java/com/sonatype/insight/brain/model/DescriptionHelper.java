/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

public class DescriptionHelper
{
  public static final int MAX_DESC_LENGTH = 255;

  public static void validate(String description) {
    if (StringUtils.isBlank(description)) {
      throw new BadRequestException("The description is required.");
    }
    if (description.length() > MAX_DESC_LENGTH) {
      throw new BadRequestException("The description cannot be longer than " + MAX_DESC_LENGTH
          + " characters, the one supplied has " + description.length() + " characters.");
    }
  }
}
