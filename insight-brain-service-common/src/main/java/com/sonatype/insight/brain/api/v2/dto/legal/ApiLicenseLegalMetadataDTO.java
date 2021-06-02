/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.dto.model.LicenseThreatGroupDTO;

/**
 * @since 1.101
 */
public class ApiLicenseLegalMetadataDTO
{
  public String licenseId;

  public String licenseName;

  public String licenseText;

  public Set<LicenseObligationDTO> obligations = new HashSet<>();

  public LicenseThreatGroupDTO threatGroup;

  public boolean isMulti = false;

  public Set<String> singleLicenseIds = new HashSet<>();

  public ApiLicenseLegalMetadataDTO() {
    // for jackson
  }

  public ApiLicenseLegalMetadataDTO(
      String licenseId,
      String licenseName,
      String licenseText,
      Set<LicenseObligationDTO> obligations,
      LicenseThreatGroupDTO threatGroup)
  {
    this.licenseId = licenseId;
    this.licenseName = licenseName;
    this.licenseText = licenseText;
    this.obligations = obligations;
    this.threatGroup = threatGroup;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiLicenseLegalMetadataDTO that = (ApiLicenseLegalMetadataDTO) o;
    return isMulti == that.isMulti && Objects.equals(licenseId, that.licenseId) &&
        Objects.equals(licenseName, that.licenseName) &&
        Objects.equals(licenseText, that.licenseText) &&
        Objects.equals(obligations, that.obligations) &&
        Objects.equals(threatGroup, that.threatGroup) &&
        Objects.equals(singleLicenseIds, that.singleLicenseIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(licenseId, licenseName, licenseText, obligations, threatGroup, isMulti, singleLicenseIds);
  }

  @Override
  public String toString() {
    return "ApiLicenseLegalMetadataDTO{" +
        "licenseId='" + licenseId + '\'' +
        ", licenseName='" + licenseName + '\'' +
        ", obligations=" + obligations +
        ", threatGroup=" + threatGroup +
        ", isMulti=" + isMulti +
        ", singleLicenseIds=" + singleLicenseIds +
        '}';
  }
}
