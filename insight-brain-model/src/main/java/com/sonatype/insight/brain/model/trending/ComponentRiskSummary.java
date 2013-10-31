/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.trending;

/**
 * Total numbers of policy violations in a particular component.
 * 
 * @since 1.7
 */
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

  /**
   * Returns component groupId
   * 
   * @since 1.7
   */
  public String getGroupId() {
    return groupId;
  }


  /**
   * Returns component artifactId
   * 
   * @since 1.7
   */
  public String getArtifactId() {
    return artifactId;
  }


  /**
   * Returns component version
   * 
   * @since 1.7
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns total number of critical policy violations in the component.
   * 
   * @since 1.7
   */
  public int getCritical() {
    return critical;
  }

  /**
   * Returns total number of severe policy violations in the component.
   * 
   * @since 1.7
   */
  public int getSevere() {
    return severe;
  }

  /**
   * Returns total number of moderate policy violations in the component.
   * 
   * @since 1.7
   */
  public int getModerate() {
    return moderate;
  }

  /**
   * Returns total number of other policy violations in the component.
   * 
   * @since 1.7
   */
  public int getNone() {
    return none;
  }

  public int getRisk() {
    return (critical * 100) + (severe * 20) + (moderate * 5);
  }
}
