/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsightFileLock
{
  private static final Logger log = LoggerFactory.getLogger(InsightFileLock.class);

  private final File file;

  private volatile FileLock lock;

  private volatile RandomAccessFile lockFile;

  InsightFileLock(InsightConfig configuration) {
    this.file = new File(configuration.getSonatypeWork().getAbsolutePath(), "lock");
  }

  public void lock() {
    if (lock != null) {
      return;
    }
    try {
      lockFile = new RandomAccessFile(file, "rws");
      lock = lockFile.getChannel().tryLock(0L, 1L, false);
      if (lock != null) {
        byte[] payload = ManagementFactory.getRuntimeMXBean().getName().getBytes(StandardCharsets.UTF_8);
        lockFile.setLength(0);
        lockFile.seek(0);
        lockFile.write(payload);
      }
      else {
        throw new IllegalStateException("Work directory " + file.getParent() + " is already in use.");
      }
    }
    catch (Exception e) {
      log.error("Failed to write lock file {}", file, e);
      releaseLock();
      throw new IllegalStateException("Work directory " + file.getParent() + " is already in use.", e);
    }
    finally {
      if (lock == null) {
        releaseLockFile();
      }
    }
  }

  public void release() {
    releaseLock();
    releaseLockFile();
  }

  private void releaseLock() {
    if (lock != null) {
      try {
        lock.release();
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      lock = null;
    }
  }

  private void releaseLockFile() {
    if (lockFile != null) {
      try {
        lockFile.close();
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      lockFile = null;
    }
  }
}
