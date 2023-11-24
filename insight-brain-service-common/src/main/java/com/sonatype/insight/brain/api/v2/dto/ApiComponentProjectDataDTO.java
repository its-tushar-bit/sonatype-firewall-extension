/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.100
 */
@JsonInclude(Include.NON_NULL)
public class ApiComponentProjectDataDTO
{
  @JsonSerialize(using = ISODateSerializer.class)
  private Date firstReleaseDate;

  @JsonSerialize(using = ISODateSerializer.class)
  private Date lastReleaseDate;

  private ApiComponentProjectMetadataDTO projectMetadata;

  private ApiComponentProjectScmDTO sourceControlManagement;

  public Date getFirstReleaseDate() {
    return firstReleaseDate;
  }

  public void setFirstReleaseDate(final Date firstReleaseDate) {
    this.firstReleaseDate = firstReleaseDate;
  }

  public Date getLastReleaseDate() {
    return lastReleaseDate;
  }

  public void setLastReleaseDate(final Date lastReleaseDate) {
    this.lastReleaseDate = lastReleaseDate;
  }

  public ApiComponentProjectMetadataDTO getProjectMetadata() {
    if (projectMetadata.organization != null || projectMetadata.description != null) {
      return projectMetadata;
    }

    return null;
  }

  public void setProjectMetadata(final ApiComponentProjectMetadataDTO projectMetadata) {
    this.projectMetadata = projectMetadata;
  }

  public ApiComponentProjectScmDTO getSourceControlManagement() {
    if (sourceControlManagement.getScmUrl() != null) {
      return sourceControlManagement;
    }

    return null;
  }

  public void setSourceControlManagement(final ApiComponentProjectScmDTO sourceControlManagement) {
    this.sourceControlManagement = sourceControlManagement;
  }
}
