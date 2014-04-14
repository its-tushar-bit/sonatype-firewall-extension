/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.io;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;

/**
 * Removes directories and files. 
 * 
 * Having our own utility encourages consistency across the system, making it easier to change implementation as necessary.
 * 
 * @since 1.9
 */
@Named
public class FileCleaner
{
  /**
   * Delete a file. If file is directory delete it with all sub-directories and containing files.
   */
  public void delete(File file) throws FileDeletionException {
    // FileUtils.forceDelete(file) will try to delete the file even if it doesn't exist and that will also cause a call
    // to System.gc() and a 10 millisec sleep, which can cause performance problems.
    if (file != null && file.exists()) {
      try {
        FileUtils.forceDelete(file);
      }
      catch (IOException e) {
        throw new FileDeletionException(file, e);
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
