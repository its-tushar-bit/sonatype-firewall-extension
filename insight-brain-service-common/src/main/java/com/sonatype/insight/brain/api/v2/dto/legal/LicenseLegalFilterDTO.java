/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Set;

public class LicenseLegalFilterDTO
{
  public LicenseLegalFilterDTO() {
    // for Jackson
  }

  public LicenseLegalFilterDTO(Set<String> organizationIds,
                               Set<String> applicationIds,
                               Set<String> tagIds,
                               Set<String> stageTypeIds,
                               Set<String> licenseIds,
                               LicenseLegalResultsOrder order,
                               int page,
                               int pageSize,
                               String componentName)
  {
    this.organizationIds = organizationIds;
    this.applicationIds = applicationIds;
    this.tagIds = tagIds;
    this.stageTypeIds = stageTypeIds;
    this.licenseIds = licenseIds;
    this.order = order;
    this.page = page;
    this.pageSize = pageSize;
    this.componentName = componentName;
  }

  public LicenseLegalFilterDTO(Set<String> organizationIds,
                               Set<String> applicationIds,
                               Set<String> tagIds,
                               Set<String> stageTypeIds,
                               Set<String> licenseIds,
                               Set<LicenseLegalReviewStatus> reviewStatus,
                               LicenseLegalResultsOrder order,
                               int page,
                               int pageSize,
                               String componentName)
  {
    this.organizationIds = organizationIds;
    this.applicationIds = applicationIds;
    this.tagIds = tagIds;
    this.stageTypeIds = stageTypeIds;
    this.licenseIds = licenseIds;
    this.reviewStatus = reviewStatus;
    this.order = order;
    this.page = page;
    this.pageSize = pageSize;
    this.componentName = componentName;
  }

  public Set<String> applicationIds;

  public Set<String> organizationIds;

  public Set<String> stageTypeIds;

  public Set<String> tagIds;

  public Set<String> licenseIds;

  public Set<LicenseLegalReviewStatus> reviewStatus;

  public LicenseLegalResultsOrder order;

  public int page;

  public int pageSize;

  public String componentName;
}
