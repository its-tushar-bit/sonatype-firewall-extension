/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.audit;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;

/**
 * DTO class for records in the license override audit logs.
 * 
 * @since 1.6
 */
public class LicenseOverrideAudit
{
  private String groupId;
  private String artifactId;
  private String version;
  private String status;
  // The overriddenLicenses is declared as List only to be backwards compatible with the existing license override
  // audit logs.
  private List<String> overriddenLicenses;
  private String comment;

  public LicenseOverrideAudit() {
  }

  public LicenseOverrideAudit(LicenseOverride licenseOverride) {
    groupId = licenseOverride.getGroupId();
    artifactId = licenseOverride.getArtifactId();
    version = licenseOverride.getVersion();
    status = licenseOverride.getStatus().getName();
    if (licenseOverride.getLicenseId() != null) {
      overriddenLicenses = new ArrayList<String>();
      License license = new LicenseDAO().getByIdNotNull(licenseOverride.getLicenseId());
      overriddenLicenses.add(license.getShortDisplayName());
    }
    comment = licenseOverride.getComment();
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getOverriddenLicenses() {
    return overriddenLicenses;
  }

  public void setOverriddenLicenses(List<String> overriddenLicenses) {
    this.overriddenLicenses = overriddenLicenses;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}