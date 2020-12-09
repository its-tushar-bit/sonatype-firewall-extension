/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;

/**
 * @since 1.101
 */
public class ApiLicenseLegalDataDTO
{
  public List<String> declaredLicenses;

  public List<String> observedLicenses;

  public List<String> effectiveLicenses;

  public List<ApiLicenseThreatDTOV2> effectiveLicenseThreats;

  public List<String> copyrights;

  public List<ApiLicenseLegalFileDTO> licenseFiles;

  public List<ApiLicenseLegalFileDTO> noticeFiles;

  public ApiLicenseLegalDataDTO() {
    // for jackson
  }

  public ApiLicenseLegalDataDTO(
      List<String> declaredLicenses,
      List<String> observedLicenses,
      List<String> effectiveLicenses,
      List<ApiLicenseThreatDTOV2> effectiveLicenseThreats,
      List<String> copyrights,
      List<ApiLicenseLegalFileDTO> licenseFiles,
      List<ApiLicenseLegalFileDTO> noticeFiles)
  {
    this.declaredLicenses = declaredLicenses;
    this.observedLicenses = observedLicenses;
    this.effectiveLicenses = effectiveLicenses;
    this.effectiveLicenseThreats = effectiveLicenseThreats;
    this.copyrights = copyrights;
    this.licenseFiles = licenseFiles;
    this.noticeFiles = noticeFiles;
  }
}
