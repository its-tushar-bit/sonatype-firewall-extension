/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * The license of a component (identified by GAV) can be overridden at application or organization (i.e. owner) level.
 * If it is overridden at both application and organization levels, the application one wins.
 * 
 * @since 1.6
 */
@Entity
@Table(name = "license_override")
public class LicenseOverride
    implements HasStringId
{
  @Id
  @Column(name = "license_override_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "group_id")
  private String groupId;

  @Column(name = "artifact_id")
  private String artifactId;

  @Column(name = "version")
  private String version;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private LicenseOverrideStatus status;

  @Column(name = "license_id")
  private String licenseId;

  @Column(name = "comment")
  private String comment;

  public LicenseOverride() {
  }

  public LicenseOverride(String ownerId, String groupId, String artifactId, String version,
      LicenseOverrideStatus status, String licenseId, String comment)
  {
    this.ownerId = ownerId;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.status = status;
    this.licenseId = licenseId;
    this.comment = comment;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
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

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public LicenseOverrideStatus getStatus() {
    return status;
  }

  public void setStatus(LicenseOverrideStatus status) {
    this.status = status;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }
}
