/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.audit;


/**
 * DTO class for records in the BOM audit logs.
 * 
 * @since 1.6
 */
public class BomAudit
{
  private String groupId;
  private String artifactId;
  private String version;
  private boolean modified;

  public BomAudit() {
  }

  public BomAudit(String groupId, String artifactId, String version, boolean modified) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.modified = modified;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isModified() {
    return modified;
  }

  public void setModified(boolean modified) {
    this.modified = modified;
  }
}