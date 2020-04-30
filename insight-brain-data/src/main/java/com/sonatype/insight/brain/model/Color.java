/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Color
{
  @Deprecated
  white,

  @Deprecated
  grey,

  @Deprecated
  black,

  @Deprecated
  green,

  yellow,

  orange,

  @Deprecated
  red,

  @Deprecated
  blue,

  light_red,

  light_green,

  light_blue,

  light_purple,

  dark_red,

  dark_green,

  dark_blue,

  dark_purple;

  public static Color fromValue(String color) {
    return Color.valueOf(color.replace('-', '_'));
  }

  @JsonValue
  public String toValue() {
    return this.name().replace('_', '-');
  }

  public Color getUpdatedColor() {
    switch (this) {
      case black:
        return Color.dark_purple;
      case blue:
        return Color.dark_blue;
      case green:
        return Color.dark_green;
      case grey:
        return Color.light_purple;
      case red:
        return Color.dark_red;
      case white:
        return Color.light_green;
      default:
        return this;
    }
  }

  public boolean isLegacy() {
    switch (this) {
      case black:
      case blue:
      case green:
      case grey:
      case red:
      case white:
        return true;
      default:
        return false;
    }
  }

  public static Color convertColorStringToEnum(String color) {
    if (color == null) {
      return null;
    }
    try {
      return Color.fromValue(color);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("Unsupported color: " + color);
    }
  }
}
