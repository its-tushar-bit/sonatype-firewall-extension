/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "license_override_license")
public class LicenseOverrideLicenseInternal
    implements HasStringId
{
  @Id
  @Column(name = "license_override_license_id")
  private String id;

  @Column(name = "license_override_id")
  private String licenseOverrideId;

  @Column(name = "license_id")
  private String licenseId;

  public LicenseOverrideLicenseInternal() {
  }

  public String getLicenseOverrideId() {
    return licenseOverrideId;
  }

  public void setLicenseOverrideId(String licenseOverrideId) {
    this.licenseOverrideId = licenseOverrideId;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
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
}
