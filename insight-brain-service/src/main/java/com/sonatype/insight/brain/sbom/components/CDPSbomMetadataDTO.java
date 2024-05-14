/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Date;

public class CDPSbomMetadataDTO
{
  private String organizationName;

  private String applicationName;

  private Date sbomCreationTime;

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(final String organizationName) {
    this.organizationName = organizationName;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  public Date getSbomCreationTime() {
    return sbomCreationTime;
  }

  public void setSbomCreationTime(final Date sbomCreationTime) {
    this.sbomCreationTime = sbomCreationTime;
  }
}
