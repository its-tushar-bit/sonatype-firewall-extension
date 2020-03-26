/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.util.Comparator;

import org.codehaus.plexus.util.StringUtils;

public class LicensesTableRow
    implements Comparable<LicensesTableRow>
{
  private static final Comparator<LicensesTableRow> COMPARATOR =
      Comparator.comparing(LicensesTableRow::getLicenses, String::compareToIgnoreCase)
          .thenComparing(row -> row.componentName, Comparator.nullsLast(String::compareToIgnoreCase));

  public String declaredLicenses;

  public String observedLicenses;

  public String componentName;

  // Visible for testing
  String getLicenses() {
    if (StringUtils.isEmpty(declaredLicenses)) {
      return StringUtils.isEmpty(observedLicenses) ? "" : observedLicenses;
    }
    if (StringUtils.isEmpty(observedLicenses)) {
      return declaredLicenses;
    }
    return declaredLicenses + ", " + observedLicenses;
  }

  @Override
  public int compareTo(LicensesTableRow that) {
    return COMPARATOR.compare(this, that);
  }
}
