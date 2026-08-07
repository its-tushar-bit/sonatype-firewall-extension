/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import com.sonatype.insight.brain.search.global.IqLocalSearchService;

/** Shared row cap and internal page size for the streaming list exports. */
public final class CsvExportLimits
{
  /**
   * Hard ceiling on data rows written to one export. At the cap the response is TRUNCATED: the rows
   * written are the first {@value #MAX_ROWS} in the requested sort order, and a final comment line is
   * appended so a truncated file is self-describing rather than silently short.
   * <p>
   * Chosen over "unbounded" deliberately: an unbounded export on a 40k-app estate holds a reader
   * open for the whole walk and can run for many minutes, and over "smaller" because a real export
   * of a large org must not be useless. 100k rows is roughly a 20-40MB file, which spreadsheet tools
   * still open.
   */
  public static final int MAX_ROWS = 100_000;

  /**
   * Rows fetched per internal index page while streaming. The public list endpoint caps a single
   * page at {@link IqLocalSearchService#MAX_PAGE_SIZE}; the export reuses that same ceiling and
   * simply walks more pages, so the export never asks the index for a page the list endpoint could
   * not serve.
   */
  public static final int PAGE_SIZE = IqLocalSearchService.MAX_PAGE_SIZE;

  /**
   * Trailing notice appended when the export stopped at {@link #MAX_ROWS}. Prefixed with {@code #} so a
   * spreadsheet user cannot mistake it for a final data row and a scripted consumer can skip it as a
   * comment rather than choking on a record with the wrong column count.
   */
  public static final String TRUNCATION_NOTICE =
      "# Export truncated at " + MAX_ROWS + " rows; narrow the filters to export the remainder.";

  private CsvExportLimits() {
  }
}
