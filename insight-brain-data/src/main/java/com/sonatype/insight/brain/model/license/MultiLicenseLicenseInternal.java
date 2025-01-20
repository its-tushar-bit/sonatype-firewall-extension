/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@IdClass(MultiLicenseLicenseInternal.ID.class)
@Table(name = "multi_license_license")
// TODO This class does NOT really have a string id. It implements HasStringId only to make it easier to create a DAO
// for it.
public class MultiLicenseLicenseInternal
    implements HasStringId
{
  @Id
  @Column(name = "multi_license_id")
  private String multiLicenseId;

  @Id
  @Column(name = "license_id")
  private String licenseId;

  public MultiLicenseLicenseInternal() {
  }

  public MultiLicenseLicenseInternal(String multiLicenseId, String licenseId) {
    this.multiLicenseId = multiLicenseId;
    this.licenseId = licenseId;
  }

  public static class ID
      implements Serializable
  {
    private static final long serialVersionUID = 4817701526044595237L;

    private String multiLicenseId;

    private String licenseId;

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      ID that = (ID) obj;
      return Objects.equals(multiLicenseId, that.multiLicenseId) && Objects.equals(licenseId, that.licenseId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(multiLicenseId, licenseId);
    }
  }

  public String getMultiLicenseId() {
    return multiLicenseId;
  }

  public void setMultiLicenseId(String multiLicenseId) {
    this.multiLicenseId = multiLicenseId;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }

  @Override
  public String getId() {
    return multiLicenseId + licenseId;
  }

  @Override
  public void setId(String id) {
  }
}
