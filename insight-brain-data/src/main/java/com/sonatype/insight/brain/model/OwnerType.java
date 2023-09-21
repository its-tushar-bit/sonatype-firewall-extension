/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @since 1.17.0
 */
public enum OwnerType
{
  APPLICATION, ORGANIZATION, REPOSITORY_CONTAINER, REPOSITORY_MANAGER, REPOSITORY, GLOBAL;

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  public static OwnerType fromString(String name) {
    if (name == null) {
      return null;
    }

    return valueOf(name.toUpperCase(Locale.ENGLISH));
  }
}
