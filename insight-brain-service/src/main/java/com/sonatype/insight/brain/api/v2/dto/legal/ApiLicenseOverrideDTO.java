/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;

import java.util.Collections;
import java.util.Set;

public class ApiLicenseOverrideDTO
{
  public String id;

  public String ownerId;

  public String comment;

  public Set<String> licenseIds;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public LicenseOverrideStatus status;

  public ApiLicenseOverrideDTO() {
    // for jackson
  }

  public ApiLicenseOverrideDTO(
      final String licenseOverrideId,
      final String ownerId,
      final String comment,
      final Set<String> licenseIds,
      final ApiComponentIdentifierDTOV2 componentIdentifier,
      final LicenseOverrideStatus status)
  {
    this.id = licenseOverrideId;
    this.ownerId = ownerId;
    this.comment = comment;
    this.licenseIds = licenseIds;
    this.componentIdentifier = componentIdentifier;
    this.status = status;
  }

  public ApiLicenseOverrideDTO(
      final String ownerId,
      final String comment,
      final String licenseId,
      final ApiComponentIdentifierDTOV2 componentIdentifier,
      final LicenseOverrideStatus status)
  {
    this.ownerId = ownerId;
    this.comment = comment;
    this.licenseIds = licenseId != null ? Collections.singleton(licenseId) : null;
    this.componentIdentifier = componentIdentifier;
    this.status = status;
  }

  public ApiLicenseOverrideDTO(
      final String ownerId,
      final String comment,
      final Set<String> licenseIds,
      final ApiComponentIdentifierDTOV2 componentIdentifier,
      final LicenseOverrideStatus status)
  {
    this.ownerId = ownerId;
    this.comment = comment;
    this.licenseIds = licenseIds;
    this.componentIdentifier = componentIdentifier;
    this.status = status;
  }

  @Override
  public String toString() {
    return "ApiLicenseOverrideDTO{" +
        "id='" + id + '\'' +
        ", ownerId='" + ownerId + '\'' +
        ", comment='" + comment + '\'' +
        ", licenseIds=" + licenseIds +
        ", componentIdentifier=" + componentIdentifier +
        ", status=" + status +
        '}';
  }
}
