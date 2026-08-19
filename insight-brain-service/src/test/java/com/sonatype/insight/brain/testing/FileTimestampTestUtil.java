/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Test utility for advancing past a filesystem's last-modified-time granularity, so that a file written after this
 * call is guaranteed a strictly greater modification timestamp than one written before it.
 */
public final class FileTimestampTestUtil
{
  // Upper bound preserving the original worst-case guarantee: some filesystems (e.g. FAT) have a 2s resolution on
  // file-modification timestamps.
  private static final long MAX_WAIT_MILLIS = 2_000;

  private static final long POLL_MILLIS = 5;

  private FileTimestampTestUtil() {
  }

  /**
   * Blocks until a probe file created in {@code directory} is observed to gain a newer modification timestamp, which
   * guarantees that a subsequent write in the same filesystem receives a strictly greater timestamp than any file
   * written before this call. Because the probe is created after the earlier write, once its timestamp is seen to
   * advance the filesystem clock has crossed into a strictly later tick.
   *
   * <p>
   * Returns within a few milliseconds on modern high-resolution filesystems and never blocks longer than
   * {@value #MAX_WAIT_MILLIS} ms, which covers coarse (e.g. 2s FAT) resolutions.
   * </p>
   *
   * @throws IllegalStateException if the timestamp does not advance within {@value #MAX_WAIT_MILLIS} ms (the
   *           filesystem resolution exceeds the supported maximum), so the missing guarantee surfaces as an explicit
   *           failure rather than a silent no-op
   */
  public static void waitForNewFileTime(final Path directory) throws InterruptedException {
    try {
      Files.createDirectories(directory);
      Path probe = Files.createTempFile(directory, "mtime-probe", ".tmp");
      try {
        FileTime start = Files.getLastModifiedTime(probe);
        long deadlineNanos = System.nanoTime() + MAX_WAIT_MILLIS * 1_000_000L;
        while (System.nanoTime() < deadlineNanos) {
          Thread.sleep(POLL_MILLIS);
          Files.writeString(probe, "x");
          if (Files.getLastModifiedTime(probe).compareTo(start) > 0) {
            return;
          }
        }
        // The probe is rewritten every POLL_MILLIS, so for any filesystem whose modification-time resolution is at
        // most MAX_WAIT_MILLIS the timestamp must advance within the deadline. Reaching here means the resolution is
        // coarser than the cap (or the filesystem does not update mtime on write); fail loudly rather than silently
        // returning without the strictly-greater-timestamp guarantee this method promises.
        throw new IllegalStateException(
            "File modification time did not advance within " + MAX_WAIT_MILLIS + " ms for directory " + directory
                + "; its filesystem timestamp resolution may exceed the supported maximum");
      }
      finally {
        Files.deleteIfExists(probe);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
