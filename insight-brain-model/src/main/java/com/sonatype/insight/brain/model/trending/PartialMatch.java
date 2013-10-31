/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * Partially matched component information.
 * 
 * @since 1.7
 */
public class PartialMatch
{
  private String groupId;
  private String artifactId;
  private String version;
  private int count;

  public PartialMatch() {
  }

  public PartialMatch(String groupId, String artifactId, String version, int count) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.count = count;
  }

  /**
   * Returns partially matched component groupId.
   * 
   * @since 1.7
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Returns partially matched component artifactId.
   * 
   * @since 1.7
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Returns partially matched component version.
   * 
   * @since 1.7
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns number of applications that use this partially matched component.
   * 
   * @since 1.7
   */
  public int getCount() {
    return count;
  }
}
