/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiLicenseDTOV2
    extends ApiLicenseDTO
{
  public List<ApiLicenseThreatDTOV2> licenseThreatGroups = new ArrayList<>();

  public ApiLicenseDTOV2() {
    // for jackson
  }

  public ApiLicenseDTOV2(String licenseId, String licenseName, List<ApiLicenseThreatDTOV2> licenseThreatGroups) {
    super(licenseId, licenseName);
    this.licenseThreatGroups = licenseThreatGroups;
  }
}
