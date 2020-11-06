/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import com.sonatype.insight.license.dto.model.LicenseObligationDTO;

public class ApiLicenseLegalObligationDTO
{
  public LicenseObligationDTO licenseObligationDTO;

  public int licenseObligationStatus;

  public ApiLicenseLegalObligationDTO(
      LicenseObligationDTO licenseObligationDTO,
      int licenseObligationStatus)
  {
    this.licenseObligationDTO = licenseObligationDTO;
    this.licenseObligationStatus = licenseObligationStatus;
  }
}
