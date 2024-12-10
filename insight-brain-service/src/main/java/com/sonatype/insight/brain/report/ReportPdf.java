/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;

public interface ReportPdf
    extends ReportEntity
{
  String REPORT_FILE_NAME = "report.pdf";

  boolean canCreate();

  long length();

  void deleteIfExists() throws IOException;
}
