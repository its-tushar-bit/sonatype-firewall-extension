/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.sonatype.insight.brain.model.Color;

public enum NxColor
{
  purple,

  pink,

  blue,

  red,

  turquoise,

  orange,

  yellow,

  kiwi,

  sky,

  indigo,

  black;

  public static NxColor getNxColorFromColor(Color color) {
    switch (color) {
      case light_purple:
        return NxColor.purple;
      case light_red:
        return NxColor.pink;
      case dark_blue:
        return NxColor.blue;
      case dark_red:
        return NxColor.red;
      case dark_green:
        return NxColor.turquoise;
      case orange:
        return NxColor.orange;
      case yellow:
        return NxColor.yellow;
      case light_green:
        return NxColor.kiwi;
      case light_blue:
        return NxColor.sky;
      case dark_purple:
        return NxColor.indigo;
      default:
        return NxColor.black;
    }
  }

  public String toNxClass() {
    return "nx-selectable-color--" + this.name();
  }
}
