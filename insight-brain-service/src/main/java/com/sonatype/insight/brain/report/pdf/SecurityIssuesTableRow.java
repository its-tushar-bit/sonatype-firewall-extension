/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.util.Comparator;

public class SecurityIssuesTableRow
    implements Comparable<SecurityIssuesTableRow>
{
  private static final Comparator<SecurityIssuesTableRow> COMPARATOR =
      Comparator.comparing((SecurityIssuesTableRow row) -> row.severity, Comparator.nullsFirst(Float::compareTo))
          .reversed()
          .thenComparing(row -> row.reference, Comparator.nullsLast(String::compareToIgnoreCase))
          .thenComparing(row -> row.componentName, Comparator.nullsLast(String::compareToIgnoreCase))
          .thenComparing(row -> row.analysisState, Comparator.nullsLast(String::compareToIgnoreCase));

  public String reference;

  public Float severity;

  public String componentName;

  public String analysisState;

  @Override
  public int compareTo(SecurityIssuesTableRow that) {
    return COMPARATOR.compare(this, that);
  }
}
