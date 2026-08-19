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
public enum ApiComponentOverrideOptionType
{
  LICENSE_OVERRIDE("license-override"),
  SECURITY_OVERRIDE("security-override");

  private final String displayName;

  private static Map<String, ApiComponentOverrideOptionType> displayNames = new HashMap<>();
  static {
    for (ApiComponentOverrideOptionType type : values()) {
      displayNames.put(type.displayName, type);
    }
  }

  private ApiComponentOverrideOptionType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static ApiComponentOverrideOptionType getByDisplayName(String displayName) {
    return displayNames.get(displayName);
  }
}
