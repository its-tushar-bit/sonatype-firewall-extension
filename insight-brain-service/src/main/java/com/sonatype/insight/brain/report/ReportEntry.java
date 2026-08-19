/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

/**
 * The contents of an individual, persisted file that makes up an application report (such as bom.json).
 * Contrast with ReportEntity, which represents the file where it is persisted
 */
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
