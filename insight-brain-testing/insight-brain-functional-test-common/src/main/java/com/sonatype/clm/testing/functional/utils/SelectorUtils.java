/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

public class SelectorUtils
{
  public static String nthChild(int num) {
    return ":nth-child(" + num + ")";
  }

  public static String nthOfType(int num) {
    return ":nth-of-type(" + num + ")";
  }

  // pseudo-classes are assumed to apply to the previous class
  public static String createSelector(String... selectors) {
    if (selectors.length == 1) {
      return selectors[0];
    }

    StringBuilder sb = new StringBuilder();
    for (String selector : selectors) {
      if (sb.length() > 0
          && !(selector.startsWith(":") || selector.startsWith("[") || sb.charAt(sb.length() - 1) == '.'))
      {
        sb.append(' ');
      }
      sb.append(selector);
    }
    return sb.toString();
  }
}
