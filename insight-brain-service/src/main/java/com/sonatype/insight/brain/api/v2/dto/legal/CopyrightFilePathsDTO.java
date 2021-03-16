/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.List;

/**
 * @since 1.108
 */
public class CopyrightFilePathsDTO
{
  private List<CopyrightFilePathDTO> filePaths;

  private int totalFileMatches;

  public CopyrightFilePathsDTO() {
    // For Jackson
  }

  public CopyrightFilePathsDTO(final List<CopyrightFilePathDTO> filePaths, final int totalFileMatches) {
    this.filePaths = filePaths;
    this.totalFileMatches = totalFileMatches;
  }

  public void setFilePaths(final List<CopyrightFilePathDTO> filePaths) {
    this.filePaths = filePaths;
  }

  public void setTotalFileMatches(final int totalFileMatches) {
    this.totalFileMatches = totalFileMatches;
  }

  public List<CopyrightFilePathDTO> getFilePaths() {
    return filePaths;
  }

  public int getTotalFileMatches() {
    return totalFileMatches;
  }
}
