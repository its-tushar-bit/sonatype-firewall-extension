/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Limit the number of bytes that can be read from the stream.
 *
 * @since 1.27
 */
class LimitedFileInputStream
    extends FileInputStream
{
  private static final Logger log = LoggerFactory.getLogger(LimitedFileInputStream.class);

  private final long readLimit;

  private volatile long readCount;

  private final boolean isToBeTruncated;

  LimitedFileInputStream(final File file, final long readLimit) throws IOException {
    super(file);
    this.readLimit = readLimit;

    final long fromByte = Math.max(0, file.length() - readLimit);
    isToBeTruncated = fromByte > 0;
    final long skipResult = skip(fromByte);
    if (skipResult != fromByte) {
      log.warn("Attempt to skip {} truncated bytes returned {} for file: {}", fromByte, skipResult,
          file.getAbsolutePath());
    }
  }

  boolean isToBeTruncated() {
    return isToBeTruncated;
  }

  boolean isReadLimitMet() {
    return readLimit <= readCount;
  }

  @Override
  public int read() throws IOException {
    if (isReadLimitMet()) {
      return -1;
    }
    readCount++;
    return super.read();
  }

  @Override
  public int read(final byte[] b) throws IOException {
    if (isReadLimitMet()) {
      return -1;
    }
    final int bytesRead = super.read(b);
    if (bytesRead != -1) {
      readCount += bytesRead;
    }
    return bytesRead;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (isReadLimitMet()) {
      return -1;
    }
    final int bytesRead = super.read(b, off, len);
    if (bytesRead != -1) {
      readCount += bytesRead;
    }
    return bytesRead;
  }
}
