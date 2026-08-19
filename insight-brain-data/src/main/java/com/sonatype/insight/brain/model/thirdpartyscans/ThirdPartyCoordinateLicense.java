/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "coordinate_license")
public class ThirdPartyCoordinateLicense
    implements HasStringId
{
  public ThirdPartyCoordinateLicense() {
    // noop
  }

  public ThirdPartyCoordinateLicense(
      String fileCoordinateId,
      String licenseId,
      String name,
      String url)
  {

    this.fileCoordinateId = fileCoordinateId;
    this.licenseId = licenseId;
    this.name = name;
    this.url = url;
  }

  @Id
  @Column(name = "coordinate_license_id")
  private String id;

  @Column(name = "file_coordinate_id")
  private String fileCoordinateId;

  @Column(name = "license_id")
  private String licenseId;

  @Column(name = "name")
  private String name;

  @Column(name = "url")
  private String url;

  @Column(name = "identification_sources")
  private String identificationSources;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getFileCoordinateId() {
    return fileCoordinateId;
  }

  public void setFileCoordinateId(String fileCoordinateId) {
    this.fileCoordinateId = fileCoordinateId;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getIdentificationSources() {
    return identificationSources;
  }

  public void setIdentificationSources(String identificationSources) {
    this.identificationSources = identificationSources;
  }

  public void addIdentificationSource(String identificationSource) {
    if (this.identificationSources == null) {
      setIdentificationSources(identificationSource);
    }
    else if (!this.identificationSources.contains(identificationSource)) {
      setIdentificationSources(getIdentificationSources() + "," + identificationSource);
    }
  }
}
