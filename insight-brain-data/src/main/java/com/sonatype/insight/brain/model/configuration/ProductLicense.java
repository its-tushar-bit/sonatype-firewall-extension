/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.96
 */
@Entity
@Table(name = "product_license")
public class ProductLicense
    implements HasStringId
{
  @Id
  @Column(name = "product_license_id")
  private String id;

  @Column(name = "license_key")
  private String licenseKey;

  @Column(name = "license_details")
  private String licenseDetails;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getLicenseKey() {
    return licenseKey;
  }

  public void setLicenseKey(String licenseKey) {
    this.licenseKey = licenseKey;
  }

  public String getLicenseDetails() {
    return licenseDetails;
  }

  public void setLicenseDetails(String licenseDetails) {
    this.licenseDetails = licenseDetails;
  }
}
