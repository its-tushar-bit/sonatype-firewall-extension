/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

public class ReleaseStatusDTO
{
  private Long releaseReadyCount;

  private Long partiallyReadyCount;

  private Long needsAttentionCount;

  public ReleaseStatusDTO() {
    // for Jackson
  }

  public ReleaseStatusDTO(
      long releaseReadyCount,
      long partiallyReadyCount,
      long needsAttentionCount)
  {
    this.releaseReadyCount = releaseReadyCount;
    this.partiallyReadyCount = partiallyReadyCount;
    this.needsAttentionCount = needsAttentionCount;
  }

  public Long getReleaseReadyCount() {
    return releaseReadyCount;
  }

  public Long getPartiallyReadyCount() {
    return partiallyReadyCount;
  }

  public Long getNeedsAttentionCount() {
    return needsAttentionCount;
  }

  public void setReleaseReadyCount(final Long releaseReadyCount) {
    this.releaseReadyCount = releaseReadyCount;
  }

  public void setPartiallyReadyCount(final Long partiallyReadyCount) {
    this.partiallyReadyCount = partiallyReadyCount;
  }

  public void setNeedsAttentionCount(final Long needsAttentionCount) {
    this.needsAttentionCount = needsAttentionCount;
  }
}
