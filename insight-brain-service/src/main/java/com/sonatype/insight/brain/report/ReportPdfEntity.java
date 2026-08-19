/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;

/**
 * Representation of the persisted application report PDF file
 */
public interface ReportPdfEntity
    extends BaseReportEntity
{
  /**
   * @return true if PDF file does not exist and can be created, defined as follows: if the persisted file does not
   *         exist or is empty.
   */
  default boolean canCreate() throws IOException {
    return !exists() || length() == 0;
  }

  /**
   * Deletes the file if it exists. If the file does not exist, this method does nothing.
   */
  void deleteIfExists() throws IOException;
}
