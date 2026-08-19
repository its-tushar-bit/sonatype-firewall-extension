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
 *        From 1.178 Pulled up from the same-named package of the insight-brain-service module,
 *        because now we need this type at the ORM level (DevelopmentPrioritizationComponentInfo).
 */
public enum ApiVersionChangeOptionType
{
  NEXT_NO_VIOLATIONS("next-no-violations"),
  NEXT_NON_FAILING("next-non-failing"),
  NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES("next-no-violations-with-dependencies"),
  NEXT_NON_FAILING_WITH_DEPENDENCIES("next-non-failing-with-dependencies"),
  // InnerSource specific remediation types
  INNER_SOURCE_LATEST_NON_BREAKING("inner-source-latest-non-breaking"),
  INNER_SOURCE_LATEST("inner-source-latest"),
  // For backward compatibility,
  // Do not use the following types in ApiVersionChangeOptionDTO,
  // They should only be used in ApiSuggestedVersionChangeOptionDTO
  RECOMMENDED_NON_BREAKING("recommended-non-breaking"),
  RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES("recommended-non-breaking-with-dependencies");

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

  public String getNameForTelemetry() {
    return displayName.replace('-', '_');
  }
}
