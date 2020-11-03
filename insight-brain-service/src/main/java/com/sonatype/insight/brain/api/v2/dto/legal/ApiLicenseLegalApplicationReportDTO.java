/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.List;
import java.util.Set;

/**
 * @since 1.101
 */
public class ApiLicenseLegalApplicationReportDTO
{
  public List<ApiLicenseLegalComponentDTO> components;

  public Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata;

  public ApiLicenseLegalApplicationReportDTO() {
    // for jackson
  }

  public ApiLicenseLegalApplicationReportDTO(
      List<ApiLicenseLegalComponentDTO> components,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata)
  {
    this.components = components;
    this.licenseLegalMetadata = licenseLegalMetadata;
  }
}
