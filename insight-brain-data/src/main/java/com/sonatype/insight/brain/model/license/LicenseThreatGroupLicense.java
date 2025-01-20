/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Association between license threat groups and licenses.
 */
@Entity
@Table(name = "license_threat_group_license")
public class LicenseThreatGroupLicense
    implements HasStringId
{
  @Id
  @Column(name = "license_threat_group_license_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "license_threat_group_id")
  private String licenseThreatGroupId;

  @Column(name = "license_id")
  private String licenseId;

  public LicenseThreatGroupLicense() {
  }

  public LicenseThreatGroupLicense(String ownerId, String licenseThreatGroupId, String licenseId) {
    this.ownerId = ownerId;
    this.licenseThreatGroupId = licenseThreatGroupId;
    this.licenseId = licenseId;
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

  public String getLicenseThreatGroupId() {
    return licenseThreatGroupId;
  }

  public void setLicenseThreatGroupId(String licenseThreatGroupId) {
    this.licenseThreatGroupId = licenseThreatGroupId;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }
}
