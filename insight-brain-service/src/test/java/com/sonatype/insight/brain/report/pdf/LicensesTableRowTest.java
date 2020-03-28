/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicensesTableRowTest
{
  @Test
  public void testGetLicenses() {
    assertThat(createLicensesTableRow(null, null).getLicenses()).isEqualTo("");
    assertThat(createLicensesTableRow("", null).getLicenses()).isEqualTo("");
    assertThat(createLicensesTableRow(null, "").getLicenses()).isEqualTo("");
    assertThat(createLicensesTableRow("", "").getLicenses()).isEqualTo("");
    assertThat(createLicensesTableRow(null, "o1, o2, o3").getLicenses()).isEqualTo("o1, o2, o3");
    assertThat(createLicensesTableRow("d1, d2, d3", null).getLicenses()).isEqualTo("d1, d2, d3");
    assertThat(createLicensesTableRow("d1, d2, d3", "o1, o2, o3").getLicenses()).isEqualTo("d1, d2, d3, o1, o2, o3");
  }

  private LicensesTableRow createLicensesTableRow(String declaredLicenses, String observedLicenses) {
    LicensesTableRow licensesTableRow = new LicensesTableRow();
    licensesTableRow.declaredLicenses = declaredLicenses;
    licensesTableRow.observedLicenses = observedLicenses;
    return licensesTableRow;
  }
}
