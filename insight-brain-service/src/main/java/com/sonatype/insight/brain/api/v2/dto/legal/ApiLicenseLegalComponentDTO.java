/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;

/**
 * @since 1.101
 */
public class ApiLicenseLegalComponentDTO
{
  public String packageUrl;

  public String hash;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String displayName;

  public ApiLicenseLegalDataDTO licenseLegalData;

  public List<ApiLicenseLegalStageScanDTO> stageScans;

  public ApiLicenseLegalComponentDTO() {
    // for jackson
  }

  public ApiLicenseLegalComponentDTO(
      ApiReportComponentDTOV2 component,
      ApiLicenseLegalDataDTO licenseLegalData,
      List<ApiLicenseLegalStageScanDTO> stageScans)
  {
    this.packageUrl = component.packageUrl;
    this.hash = component.hash;
    this.componentIdentifier = component.componentIdentifier;
    this.displayName = component.displayName;
    this.licenseLegalData = licenseLegalData;
    this.stageScans = stageScans;
  }

  public ApiLicenseLegalComponentDTO(
      ApiComponentDTOV2 component,
      ApiLicenseLegalDataDTO licenseLegalData,
      List<ApiLicenseLegalStageScanDTO> stageScans)
  {
    this.packageUrl = component.packageUrl;
    this.hash = component.hash;
    this.componentIdentifier = component.componentIdentifier;
    this.displayName = component.displayName;
    this.licenseLegalData = licenseLegalData;
    this.stageScans = stageScans;
  }
}
