/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.options;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @since 1.64
 */
public enum ApiVersionChangeOptionType
{
  NEXT_NO_VIOLATIONS("next-no-violations"),
  NEXT_NON_FAILING("next-non-failing"),
  NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES("next-no-violations-with-dependencies"),
  NEXT_NON_FAILING_WITH_DEPENDENCIES("next-non-failing-with-dependencies");

  private final String displayName;

  private static Map<String, ApiVersionChangeOptionType> displayNames = new HashMap<>();

  static {
    for (ApiVersionChangeOptionType type : values()) {
      displayNames.put(type.displayName, type);
    }
  }

  private ApiVersionChangeOptionType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static ApiVersionChangeOptionType getByDisplayName(String displayName) {
    return displayNames.get(displayName);
  }
}
