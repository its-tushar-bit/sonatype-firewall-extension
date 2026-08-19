/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

/**
 * Representation of an individual, persisted file that makes up an application report (such as bom.json).
 * Contrast with ReportEntry, which contains the _contents_ of such a file in memory.
 */
public interface ReportEntity
    extends BaseReportEntity
{
  /**
   * The name, aka relative file path within the report, of this entity
   */
  String getName();
}
