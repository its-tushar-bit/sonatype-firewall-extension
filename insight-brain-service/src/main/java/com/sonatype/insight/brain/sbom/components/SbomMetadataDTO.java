/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Date;

public class SbomMetadataDTO
{
  public String specification;

  public String specVersion;

  public String fileFormat;

  public String metadataJson;

  public String scanId;

  public Date createdAt;

  public SbomMetadataDTO() {
  }

  public SbomMetadataDTO(
      final String specification,
      final String specVersion,
      final String fileFormat,
      final String metadataJson,
      final String scanId,
      final Date createdAt)
  {
    this.specification = specification;
    this.specVersion = specVersion;
    this.fileFormat = fileFormat;
    this.metadataJson = metadataJson;
    this.scanId = scanId;
    this.createdAt = createdAt;
  }
}
