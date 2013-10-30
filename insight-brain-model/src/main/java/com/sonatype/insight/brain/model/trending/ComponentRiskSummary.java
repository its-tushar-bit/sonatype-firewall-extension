/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

public class ComponentRiskSummary
{
  private String groupId;
  private String artifactId;
  private String version;
  private int critical;
  private int severe;
  private int moderate;
  private int none;

  public ComponentRiskSummary() {
  }

  public ComponentRiskSummary(String groupId, String artifactId, String version, int critical, int severe,
      int moderate, int none)
  {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.critical = critical;
    this.severe = severe;
    this.moderate = moderate;
    this.none = none;
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

  public int getCritical() {
    return critical;
  }

  public int getSevere() {
    return severe;
  }

  public int getModerate() {
    return moderate;
  }

  public int getNone() {
    return none;
  }

  public int getRisk() {
    return (critical * 100) + (severe * 20) + (moderate * 5);
  }
}
