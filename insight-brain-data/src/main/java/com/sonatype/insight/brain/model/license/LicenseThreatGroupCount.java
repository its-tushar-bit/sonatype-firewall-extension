/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

/**
 * Per-License-Threat-Group count of components whose license-obligation review is
 * {@code UNREVIEWED} for the given owner or application scope.
 *
 * @since 1.204
 */
public class LicenseThreatGroupCount
{
  private String licenseThreatGroupId;

  private String licenseThreatGroupName;

  private int threatLevel;

  private long unreviewedComponentCount;

  public LicenseThreatGroupCount() {
  }

  public LicenseThreatGroupCount(
      String licenseThreatGroupId,
      String licenseThreatGroupName,
      int threatLevel,
      long unreviewedComponentCount)
  {
    this.licenseThreatGroupId = licenseThreatGroupId;
    this.licenseThreatGroupName = licenseThreatGroupName;
    this.threatLevel = threatLevel;
    this.unreviewedComponentCount = unreviewedComponentCount;
  }

  public String getLicenseThreatGroupId() {
    return licenseThreatGroupId;
  }

  public void setLicenseThreatGroupId(String licenseThreatGroupId) {
    this.licenseThreatGroupId = licenseThreatGroupId;
  }

  public String getLicenseThreatGroupName() {
    return licenseThreatGroupName;
  }

  public void setLicenseThreatGroupName(String licenseThreatGroupName) {
    this.licenseThreatGroupName = licenseThreatGroupName;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public long getUnreviewedComponentCount() {
    return unreviewedComponentCount;
  }

  public void setUnreviewedComponentCount(long unreviewedComponentCount) {
    this.unreviewedComponentCount = unreviewedComponentCount;
  }
}
