/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @since 1.70.0
 */
public enum SourceControlProvider
{
  GITHUB, GITLAB;

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  @JsonCreator
  public static SourceControlProvider fromString(String name) {
    if (name == null) {
      return null;
    }

    try {
      return valueOf(name.toUpperCase(Locale.ENGLISH));
    }
    catch (IllegalArgumentException e) {
      String allowedValues =
          String.join(", ", Arrays.stream(values()).map(SourceControlProvider::toString).collect(Collectors.toList()));
      throw new BadRequestException(String
          .format("SourceControl provider value '%s' is invalid, valid options are: %s", name,
              allowedValues));
    }
  }
}
