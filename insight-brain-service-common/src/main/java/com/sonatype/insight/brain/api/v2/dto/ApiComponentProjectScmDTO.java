/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.100
 */
@JsonInclude(Include.NON_NULL)
public class ApiComponentProjectScmDTO
{
  private String scmUrl;

  private ApiComponentProjectScmMetadataDTO scmMetadata;

  private ApiComponentProjectScmDetailsDTO scmDetails;

  public String getScmUrl() {
    return scmUrl;
  }

  public void setScmUrl(final String scmUrl) {
    this.scmUrl = scmUrl;
  }

  public ApiComponentProjectScmMetadataDTO getScmMetadata() {
    if (scmMetadata.forks != null || scmMetadata.stars != null) {
      return scmMetadata;
    }

    return null;
  }

  public void setScmMetadata(final ApiComponentProjectScmMetadataDTO scmMetadata) {
    this.scmMetadata = scmMetadata;
  }

  public ApiComponentProjectScmDetailsDTO getScmDetails() {
    if (scmDetails.commitsPerMonth != null || scmDetails.uniqueDevsPerMonth != null) {
      return scmDetails;
    }

    return null;
  }

  public void setScmDetails(final ApiComponentProjectScmDetailsDTO scmDetails) {
    this.scmDetails = scmDetails;
  }
}
