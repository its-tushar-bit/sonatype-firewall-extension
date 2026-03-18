/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.model.license.LicenseOverride;

public class LicenseOverrideUtil
{
  public static LicenseOverride toInternalLicenseOverride(ApiLicenseOverrideDTO apiLicenseOverrideDTO) {
    if (apiLicenseOverrideDTO == null) {
      return null;
    }

    return new LicenseOverride(
        apiLicenseOverrideDTO.ownerId,
        apiLicenseOverrideDTO.componentIdentifier != null
            ? apiLicenseOverrideDTO.componentIdentifier.toComponentIdentifier()
            : null,
        apiLicenseOverrideDTO.status,
        apiLicenseOverrideDTO.licenseIds,
        apiLicenseOverrideDTO.comment);
  }

  public static ApiLicenseOverrideDTO toApiLicenseOverrideDTO(LicenseOverride licenseOverride) {
    if (licenseOverride == null) {
      return null;
    }

    return new ApiLicenseOverrideDTO(
        licenseOverride.getId(),
        licenseOverride.getOwnerId(),
        licenseOverride.getComment(),
        licenseOverride.getLicenseIds(),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(licenseOverride.getComponentIdentifier()),
        licenseOverride.getStatus());
  }
}
