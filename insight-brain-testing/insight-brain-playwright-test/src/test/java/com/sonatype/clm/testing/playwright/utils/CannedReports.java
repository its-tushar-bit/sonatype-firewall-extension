/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

/**
 * Stable references into the canned scan reports. Centralising the hashes here makes a future
 * BOM regeneration a single-file update — callers fail loudly at the assertion site instead
 * of drifting silently across the suite.
 */
public final class CannedReports
{
  /** Hash of {@code org.apache.tiles:tiles-core:2.2.2}, first entry in large-report's bom.json. */
  public static final String LARGE_REPORT_TILES_CORE_HASH = "01db730fbe26c148e3d0";

  private CannedReports() {
  }
}
