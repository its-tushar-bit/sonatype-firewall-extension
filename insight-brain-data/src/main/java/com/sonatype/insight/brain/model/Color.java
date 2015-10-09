/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Color
{
  white, grey, black, green, yellow, orange, red, blue,
  // CLM-5299 will migrate old colors above to the new below.
  light_red, light_green, light_blue, light_purple, dark_red, dark_green, dark_blue, dark_purple;

  @JsonValue
  public String toValue() {
    return this.name().replace('_', '-');
  }
}
