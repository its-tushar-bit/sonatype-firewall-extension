/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

/**
 * Media type constants for CSV content.
 *
 */
public final class CsvMediaType
{
  private CsvMediaType() {
    throw new UnsupportedOperationException(getClass().getSimpleName() + " should not be instantiated.");
  }

  /**
   * A String constant representing {@value #TEXT_CSV} media type.
   */
  public static final String TEXT_CSV = "text/csv";
}
