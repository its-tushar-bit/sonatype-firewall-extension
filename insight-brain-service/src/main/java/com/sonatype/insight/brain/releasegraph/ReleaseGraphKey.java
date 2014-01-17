/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import com.sonatype.insight.brain.model.GAVPopularity;

public class ReleaseGraphKey
{
  private String artifactId;

  private String groupId;

  private String version;

  private ReportItemKey reportKey;

  public ReleaseGraphKey(String groupId, String artifactId, String version, ReportItemKey reportKey) {
    this.artifactId = artifactId;
    this.groupId = groupId;
    this.version = version;
    this.reportKey = reportKey;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
    result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((reportKey == null) ? 0 : reportKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ReleaseGraphKey)) {
      return false;
    }
    ReleaseGraphKey that = (ReleaseGraphKey) obj;
    return eq(reportKey, that.reportKey) && eq(artifactId, that.artifactId) && eq(groupId, that.groupId)
        && eq(version, that.version);
  }

  public ReportItemKey getReportItemKey() {
    return reportKey;
  }

  boolean isMatch(GAVPopularity gav) {
    return eq(artifactId, gav.getArtifactId()) && eq(groupId, gav.getGroupId()) && eq(version, gav.getVersion());
  }

  private static <T> boolean eq(T o1, T o2) {
    return (o1 != null) ? o1.equals(o2) : o2 == null;
  }

  public String getGAV() {
    return groupId + ':' + artifactId + ':' + version;
  }
}