/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

public class RemediationVersionDTO
{
  private final String version;

  private final Integer breakingChangesCount;

  public RemediationVersionDTO(final String version, final Integer breakingChangesCount) {
    this.version = version;
    this.breakingChangesCount = breakingChangesCount;
  }

  public RemediationVersionDTO(final String version) {
    this(version, null);
  }

  public String getVersion() {
    return version;
  }

  public Integer getBreakingChangesCount() {
    return breakingChangesCount;
  }
}
