/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApiType
{
  PUBLIC("/api/v2/"),
  EXPERIMENTAL("/api/experimental/");

  private final String pathPrefix;

  ApiType(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  public String getPathPrefix() {
    return pathPrefix;
  }

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static ApiType fromString(String name) {
    if (name == null) {
      return null;
    }
    return valueOf(name.toUpperCase(Locale.ROOT));
  }
}
