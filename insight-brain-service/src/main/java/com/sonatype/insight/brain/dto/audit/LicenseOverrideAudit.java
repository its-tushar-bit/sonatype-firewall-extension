/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.audit;

import java.util.ArrayList;
import java.util.Collections;
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
    extends Auditable
{
  private String status;

  // The overriddenLicenses is declared as List only to be backwards compatible with the existing license override
  // audit logs.
  private List<String> overriddenLicenses;

  private String comment;

  public LicenseOverrideAudit() {
  }

  public LicenseOverrideAudit(LicenseOverride licenseOverride, LicenseDAO licenseDAO) {
    setComponentIdentifier(licenseOverride.getComponentIdentifier());
    status = licenseOverride.getStatus().getName();
    if (!licenseOverride.getLicenseIds().isEmpty()) {
      overriddenLicenses = new ArrayList<>();

      for (String licenseId : licenseOverride.getLicenseIds()) {
        License license = licenseDAO.getByIdNotNull(licenseId);
        overriddenLicenses.add(license.getShortDisplayName());
      }

      Collections.sort(overriddenLicenses);
    }
    comment = licenseOverride.getComment();
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
