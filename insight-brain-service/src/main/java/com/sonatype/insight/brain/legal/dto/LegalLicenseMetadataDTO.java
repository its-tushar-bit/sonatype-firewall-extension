/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal.dto;

import java.util.Set;

import com.sonatype.insight.license.dto.model.LicenseObligationDTO;

/**
 * @since 1.101
 */
public class LegalLicenseMetadataDTO
{
  public String licenseId;

  public String licenseName;

  public String licenseText;

  public Set<LicenseObligationDTO> obligations;

  public LegalLicenseMetadataDTO(
      String licenseId,
      String licenseName,
      String licenseText,
      Set<LicenseObligationDTO> obligations)
  {
    this.licenseId = licenseId;
    this.licenseName = licenseName;
    this.licenseText = licenseText;
    this.obligations = obligations;
  }
}
