/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

/**
 * Summary of counts for repository results details matching given filters.
 * Used to provide total count and per-filter counts for the bulk waiver page.
 *
 * @since 1.203
 */
public class RepositoryResultsCountSummary
{
  public final long totalCount;

  public final long openCount;

  public final long waivedCount;

  public final long quarantinedCount;

  public RepositoryResultsCountSummary(
      final long totalCount,
      final long openCount,
      final long waivedCount,
      final long quarantinedCount)
  {
    this.totalCount = totalCount;
    this.openCount = openCount;
    this.waivedCount = waivedCount;
    this.quarantinedCount = quarantinedCount;
  }
}
