/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal.dto;

import java.util.List;
import java.util.Set;

/**
 * @since 1.101
 */
public class LegalReportDataDTO
{
  public List<LegalReportComponentDTO> components;

  public Set<LegalLicenseMetadataDTO> licenseMetadata;

  public LegalReportDataDTO(
      List<LegalReportComponentDTO> components,
      Set<LegalLicenseMetadataDTO> licenseMetadata)
  {
    this.components = components;
    this.licenseMetadata = licenseMetadata;
  }
}
