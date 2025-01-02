/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileReportPdf
    extends AbstractFileReportEntity
    implements ReportPdf
{
  public FileReportPdf(final File file) {
    super(file);
  }

  @Override
  public boolean canCreate() {
    return !file.isFile() || file.length() == 0;
  }

  @Override
  public long length() {
    return file.length();
  }

  @Override
  public void deleteIfExists() throws IOException {
    Files.deleteIfExists(file.toPath());
  }
}
