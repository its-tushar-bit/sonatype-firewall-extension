/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

public enum FirewallSortableField
{
  QUARANTINE_TIME("quarantineTime"),
  UNQUARANTINE_TIME("unquarantineTime");

  private final String column;

  FirewallSortableField(final String column) {
    this.column = column;
  }

  public String getColumn() {
    return column;
  }
}
