/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom;

public enum SbomDependencyType
{
  DIRECT("D"),
  TRANSITIVE("T"),
  UNKNOWN("unknown");

  private final String code;

  SbomDependencyType(String code) {
    this.code = code;
  }

  public static SbomDependencyType fromCode(String code) {
    if (code == null) {
      return UNKNOWN;
    }
    for (SbomDependencyType type : SbomDependencyType.values()) {
      if (type.code.equals(code)) {
        return type;
      }
    }
    return UNKNOWN;
  }
}
