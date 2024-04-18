/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

public final class ReportEntry
{
  public final String name;

  public final long time;

  public final byte[] buf;

  public ReportEntry(final String name, final long time, final byte[] buf) {
    this.name = name;
    this.time = time;
    this.buf = buf;
  }
}
