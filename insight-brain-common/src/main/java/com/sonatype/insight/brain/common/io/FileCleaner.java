/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.io;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Named;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes directories and files.
 *
 * Having our own utility encourages consistency across the system, making it easier to change implementation as
 * necessary.
 *
 * @since 1.9
 */
@Named
public class FileCleaner
{
  private static final Logger log = LoggerFactory.getLogger(FileCleaner.class);

  /**
   * Delete a file. If file is directory delete it with all sub-directories and containing files.
   */
  public void delete(File file) throws FileDeletionException {
    if (file != null && file.exists()) {
      long start = System.currentTimeMillis();

      try {
        FileUtils.forceDelete(file);
      }
      catch (Exception e) {
        throw new FileDeletionException(file, e);
      }

      long duration = System.currentTimeMillis() - start;
      if (duration > 500) {
        log.debug("Deleted file/dir '{}' in {} ms.", file.getAbsolutePath(), duration);
      }
    }
  }

  /**
   * Indicates that a deletion was not successful.
   *
   * Also having a specific exception makes it easy to locate in logs.
   */
  public static class FileDeletionException
      extends IOException
  {
    private static final long serialVersionUID = 6322158674244100349L;

    /**
     * @param file the file that could not be deleted
     * @param exception the exception that occurred while trying to delete
     */
    public FileDeletionException(File file, Exception exception) {
      super("File can not be deleted: " + file.getAbsolutePath(), exception);
    }
  }
}
