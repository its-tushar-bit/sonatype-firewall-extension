/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.util.Comparator;

public class LicensesTableRow
    implements Comparable<LicensesTableRow>
{
  private static final Comparator<LicensesTableRow> COMPARATOR =
      Comparator
          .comparingInt((LicensesTableRow row) -> row.threatLevel)
          .reversed()
          .thenComparing(row -> row.effectiveLicenses, String::compareToIgnoreCase)
          .thenComparing(row -> row.declaredLicenses, Comparator.nullsLast(String::compareToIgnoreCase))
          .thenComparing(row -> row.observedLicenses, Comparator.nullsLast(String::compareToIgnoreCase))
          .thenComparing(row -> row.componentName, Comparator.nullsLast(String::compareToIgnoreCase));

  public boolean overridden;

  public Integer threatLevel;

  public String effectiveLicenses;

  public String declaredLicenses;

  public String observedLicenses;

  public String componentName;

  @Override
  public int compareTo(LicensesTableRow that) {
    return COMPARATOR.compare(this, that);
  }
}
