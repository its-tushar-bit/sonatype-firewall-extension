/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

/**
 * @since 1.108
 */
public class CopyrightFilePathDTO
    implements Comparable<CopyrightFilePathDTO>
{
  private String filePath;

  private int copyrightMatches;

  public CopyrightFilePathDTO() {
    // For Jackson
  }

  public CopyrightFilePathDTO(final String filePath, final int copyrightMatches) {
    this.filePath = filePath;
    this.copyrightMatches = copyrightMatches;
  }

  public String getFilePath() {
    return filePath;
  }

  public int getCopyrightMatches() {
    return copyrightMatches;
  }

  public void setFilePath(final String filePath) {
    this.filePath = filePath;
  }

  public void setCopyrightMatches(final int copyrightMatches) {
    this.copyrightMatches = copyrightMatches;
  }

  @Override
  public int compareTo(final CopyrightFilePathDTO other) {
    return filePath.compareTo(other.filePath);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final CopyrightFilePathDTO that = (CopyrightFilePathDTO) o;
    return copyrightMatches == that.copyrightMatches && filePath.equals(that.filePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filePath, copyrightMatches);
  }

  @Override
  public String toString() {
    return "CopyrightFilePathDTO{" +
        "filePath='" + filePath + '\'' +
        ", copyrightMatches=" + copyrightMatches +
        '}';
  }
}
