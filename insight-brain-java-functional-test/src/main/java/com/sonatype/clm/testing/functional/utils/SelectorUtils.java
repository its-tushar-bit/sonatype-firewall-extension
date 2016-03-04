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

  // pseudo-classes are assumed to apply to the previous class
  public static String createSelector(String... selectors) {
    StringBuilder sb = new StringBuilder();

    for (String selector : selectors) {
      if (!(selector.startsWith(":") || selector.startsWith("[")) && sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(selector);
    }
    return sb.toString();
  }
}
