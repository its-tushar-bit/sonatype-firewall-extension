/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

public enum ScmPrIneligibleReason
{
  NOT_ELIGIBLE("not_eligible"),
  NO_REMEDIATION("no_remediation"),
  ALREADY_REMEDIATED("already_remediated"),
  NOT_GOLDEN_VERSION("not_golden_version"),
  SAME_VERSION("same_version");

  private final String value;

  ScmPrIneligibleReason(final String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
