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
 * Representation of an individual, persisted file that makes up an application report (such as bom.json). Note that
 * an instance of this class does not guarantee that the file actually exists. Current file existence can be checked
 * with the exists() method.
 *
 * Contrast with ReportEntry, which contains the _contents_ of such a file in memory.
 */
public interface BaseReportEntity
{
  /**
   * @return whether or not the file exists in the underlying storage
   */
  boolean exists() throws IOException;

  /**
   * @return the last modified time of the file
   * @throws IOException if the file does not exist or if there is an error reading the file's metadata
   */
  long getTime() throws IOException;

  /**
   * @return the length of the file in bytes
   */
  long length() throws IOException;

  /**
   * @return an OutputStream that can be used to write to the file. If the file already exists, it will be overwritten.
   * If it doesn't already exist, it will be created.
   */
  OutputStream getOutputStream() throws IOException;

  /**
   * @return an InputStream that can be used to read from the file. If the file does not exist, an IOException will be
   * thrown.
   */
  InputStream getInputStream() throws IOException;
  
  /**
   * The primary {@link ApplicationReportPersistenceService} class that handles this.
   */
  Class<? extends ApplicationReportPersistenceService> getApplicationReportPersistenceServiceClass();
}
