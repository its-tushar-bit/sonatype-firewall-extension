/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

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

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public int getCount() {
    return count;
  }
}
