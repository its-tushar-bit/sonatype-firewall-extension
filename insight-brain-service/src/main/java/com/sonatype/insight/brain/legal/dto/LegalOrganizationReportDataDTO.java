/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal.dto;

import java.util.Collection;
import java.util.Set;

/**
 * @since 1.101
 */
public class LegalOrganizationReportDataDTO
{
  public Collection<LegalApplicationDataDTO> organizationData;

  public Set<LegalLicenseMetadataDTO> licenseMetadata;

  public LegalOrganizationReportDataDTO(
      Collection<LegalApplicationDataDTO> organizationData,
      Set<LegalLicenseMetadataDTO> licenseMetadata)
  {
    this.organizationData = organizationData;
    this.licenseMetadata = licenseMetadata;
  }
}
