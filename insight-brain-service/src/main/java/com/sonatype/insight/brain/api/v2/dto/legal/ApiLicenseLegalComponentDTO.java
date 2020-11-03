/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;

/**
 * @since 1.101
 */
public class ApiLicenseLegalComponentDTO
{
  public String packageUrl;

  public String hash;

  public ComponentIdentifier componentIdentifier;

  public String displayName;

  public ApiLicenseLegalDataDTO licenseLegalData;

  public ApiLicenseLegalComponentDTO() {
    // for jackson
  }

  public ApiLicenseLegalComponentDTO(ApiReportComponentDTOV2 component, ApiLicenseLegalDataDTO licenseLegalData) {
    this.packageUrl = component.packageUrl;
    this.hash = component.hash;
    this.componentIdentifier = component.componentIdentifier.toComponentIdentifier();
    this.displayName = component.displayName;
    this.licenseLegalData = licenseLegalData;
  }
}
