/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

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
      return eq(multiLicenseId, that.multiLicenseId) && eq(licenseId, that.licenseId);
    }

    private static <T> boolean eq(T obj1, T obj2) {
      return (obj1 == null) ? obj2 == null : obj1.equals(obj2);
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + hash(multiLicenseId);
      result = 31 * result + hash(licenseId);
      return result;
    }

    private static int hash(Object obj) {
      return (obj == null) ? 0 : obj.hashCode();
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
