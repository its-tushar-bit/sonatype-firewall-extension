/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility that distills a single filename (for display purposes in case a component lacks a component identifier) from
 * all the recorded occurrences of a component.
 */
public class ComponentDisplayFilename
{
  private final Map<String, Integer> filenameFrequencies = new HashMap<>();

  private String mostFrequentFilename;

  private int mostFrequentFilenameFrequency;

  public Optional<String> getFilename() {
    return Optional.ofNullable(mostFrequentFilename);
  }

  public ComponentDisplayFilename addPathnames(Collection<String> pathnames) {
    if (pathnames != null) {
      for (String pathname : pathnames) {
        int separator = FilenameUtils.indexOfLastSeparator(pathname);
        if (separator >= 0 && separator == pathname.length() - 1) {
          pathname = pathname.substring(0, pathname.length() - 1);
          separator = FilenameUtils.indexOfLastSeparator(pathname);
        }
        String filename = pathname.substring(separator + 1);
        Integer filenameFrequency = filenameFrequencies.get(filename);
        filenameFrequency = filenameFrequency == null ? 1 : filenameFrequency + 1;
        filenameFrequencies.put(filename, filenameFrequency);
        if (filenameFrequency > mostFrequentFilenameFrequency) {
          mostFrequentFilename = filename;
          mostFrequentFilenameFrequency = filenameFrequency;
        }
        else if (filenameFrequency == mostFrequentFilenameFrequency && filename.compareTo(mostFrequentFilename) < 0) {
          mostFrequentFilename = filename;
        }
      }
    }
    return this;
  }
}
