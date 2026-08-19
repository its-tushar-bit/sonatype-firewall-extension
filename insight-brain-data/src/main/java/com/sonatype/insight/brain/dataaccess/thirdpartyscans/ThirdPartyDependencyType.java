/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum ThirdPartyDependencyType
{
  DIRECT("D", "direct"),
  TRANSITIVE("T", "transitive"),
  UNSPECIFIED(null, "unspecified");

  private String value;

  private String displayName;

  private ThirdPartyDependencyType(String value, String displayName) {
    this.value = value;
    this.displayName = displayName;
  }

  public String getValue() {
    return value;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static ThirdPartyDependencyType fromValue(String value) {
    return Arrays.stream(values())
        .filter(type -> StringUtils.equalsAnyIgnoreCase(type.getValue(), value))
        .findFirst()
        .orElse(null);
  }
}
