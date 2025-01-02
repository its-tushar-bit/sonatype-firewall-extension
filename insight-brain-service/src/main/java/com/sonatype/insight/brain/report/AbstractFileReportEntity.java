/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AbstractFileReportEntity
    implements ReportEntity
{
  protected final File file;

  public AbstractFileReportEntity(final File file) {
    this.file = file;
  }

  @Override
  public boolean exists() {
    return file.exists();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return new FileOutputStream(file);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }

  public File getFile() {
    return file;
  }
}
