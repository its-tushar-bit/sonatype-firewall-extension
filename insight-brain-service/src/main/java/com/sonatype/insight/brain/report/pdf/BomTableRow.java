/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.util.Comparator;

public class BomTableRow
    implements Comparable<BomTableRow>
{
  private static final Comparator<BomTableRow> COMPARATOR =
      Comparator.comparing(row -> row.componentName, Comparator.nullsLast(String::compareToIgnoreCase));

  public String componentName;

  @Override
  public int compareTo(BomTableRow that) {
    return COMPARATOR.compare(this, that);
  }
}
