/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.ExpirationDate;
import com.sonatype.insight.brain.dashboard.RisksFilterDTO;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

public class RisksFilterDTOBuilder
{
  private final RisksFilterDTO risksFilterDTO;

  public RisksFilterDTOBuilder() {
    this.risksFilterDTO = new RisksFilterDTO();
  }

  public RisksFilterDTO build() {
    return this.risksFilterDTO;
  }

  public RisksFilterDTOBuilder withApplicationIds(final Set<String> applicationIds) {
    this.risksFilterDTO.applicationIds = applicationIds;
    return this;
  }

  public RisksFilterDTOBuilder withOrganizationIds(final Set<String> organizationIds) {
    this.risksFilterDTO.organizationIds = organizationIds;
    return this;
  }

  public RisksFilterDTOBuilder withTagIds(final Set<String> tagIds) {
    this.risksFilterDTO.tagIds = tagIds;
    return this;
  }

  public RisksFilterDTOBuilder withPolicyThreatCategories(final PolicyThreatCategoryFilter policyThreatCategories) {
    this.risksFilterDTO.policyThreatCategories = policyThreatCategories;
    return this;
  }

  public RisksFilterDTOBuilder withPolicyThreatLevelRange(final PolicyThreatLevelFilter policyThreatLevelRange) {
    this.risksFilterDTO.policyThreatLevelRange = policyThreatLevelRange;
    return this;
  }

  public RisksFilterDTOBuilder withOrderBy(final String orderBy) {
    this.risksFilterDTO.orderBy = orderBy;
    return this;
  }

  public RisksFilterDTOBuilder withExpirationDate(final ExpirationDate expirationDate) {
    this.risksFilterDTO.expirationDate = expirationDate;
    return this;
  }

  public RisksFilterDTOBuilder withPageSize(final int pageSize) {
    this.risksFilterDTO.pageSize = pageSize;
    return this;
  }

  public RisksFilterDTOBuilder withRepositoryIds(final Set<String> repositoryIds) {
    this.risksFilterDTO.repositoryIds = repositoryIds;
    return this;
  }

  public RisksFilterDTOBuilder withPage(final int page) {
    this.risksFilterDTO.page = page;
    return this;
  }
}
