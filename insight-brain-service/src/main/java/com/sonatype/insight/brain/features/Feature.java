/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Feature
{
  String name();

  @JsonValue
  public default String getId() {
    return name().toLowerCase(Locale.ENGLISH).replace('_', '-');
  }
}
