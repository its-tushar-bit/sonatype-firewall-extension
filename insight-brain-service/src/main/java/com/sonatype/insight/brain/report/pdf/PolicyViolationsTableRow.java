/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.util.Comparator;

public class PolicyViolationsTableRow
    implements Comparable<PolicyViolationsTableRow>
{
  private static final Comparator<PolicyViolationsTableRow> COMPARATOR = Comparator
      .comparingInt((PolicyViolationsTableRow row) -> row.threatLevel)
      .reversed()
      .thenComparing(row -> row.policyName, String::compareToIgnoreCase)
      .thenComparing(row -> row.componentName, Comparator.nullsLast(String::compareToIgnoreCase));

  public Integer threatLevel;

  public String policyName;

  public String policyType;

  public boolean waived;

  public String componentName;

  @Override
  public int compareTo(PolicyViolationsTableRow that) {
    return COMPARATOR.compare(this, that);
  }
}
