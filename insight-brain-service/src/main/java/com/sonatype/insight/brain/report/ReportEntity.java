/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for report implementations to provide different ways to access the report data
 */
public interface ReportEntity
{
  boolean exists();

  void deleteIfExists() throws IOException;

  boolean canCreate();

  OutputStream getOutputStream() throws IOException;

  String getLocation();

  long length();

  InputStream getInputStream() throws IOException;
}
