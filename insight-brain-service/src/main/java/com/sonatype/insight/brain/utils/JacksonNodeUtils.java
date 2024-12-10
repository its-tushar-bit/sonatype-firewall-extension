/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.utils;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class JacksonNodeUtils
{
  private JacksonNodeUtils() {
  }

  public static void fill(final ArrayNode node, final int[] data) {
    for (final int d : data) {
      node.add(d);
    }
  }

  public static void fill(final ArrayNode node, final List<int[]> datas) {
    for (final int[] data : datas) {
      fill(node.addArray(), data);
    }
  }
}
