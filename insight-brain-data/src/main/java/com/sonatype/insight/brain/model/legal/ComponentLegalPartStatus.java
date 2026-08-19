/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ComponentLegalPartStatus
{
  ENABLED,
  DISABLED;

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  public static ComponentLegalPartStatus fromString(String name) {
    if (name == null) {
      return null;
    }

    return valueOf(name.toUpperCase(Locale.ENGLISH));
  }
}
