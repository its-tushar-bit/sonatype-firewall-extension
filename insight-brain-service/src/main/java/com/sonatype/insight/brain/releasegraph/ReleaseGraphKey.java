/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.ComponentPopularity;

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
    return Objects.equals(reportKey, that.reportKey) && Objects.equals(artifactId, that.artifactId)
        && Objects.equals(groupId, that.groupId) && Objects.equals(version, that.version);
  }

  public ReportItemKey getReportItemKey() {
    return reportKey;
  }

  boolean isMatch(ComponentPopularity component) {
    return component.getComponentIdentifier() != null &&
        component.getComponentIdentifier().isMaven() &&
        Objects.equals(groupId, component.getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID)) &&
        Objects.equals(artifactId, component.getComponentIdentifier().get(ComponentIdentifier.MAVEN_ARTIFACT_ID)) &&
        Objects.equals(version, component.getComponentIdentifier().get(ComponentIdentifier.VERSION));
  }

  public String getGAV() {
    return groupId + ':' + artifactId + ':' + version;
  }
}