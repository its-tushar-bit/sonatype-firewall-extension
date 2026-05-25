/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

/**
 * @since 1.20.0
 */
public class RisksFilterDTO
{
  public Set<String> applicationIds;

  public Set<String> organizationIds;

  public Set<String> policyWaiverReasonIds;

  public Set<String> stageIds;

  public Set<String> tagIds;

  public PolicyThreatCategoryFilter policyThreatCategories;

  public PolicyThreatLevelFilter policyThreatLevelRange;

  public PolicyViolationStateFilter policyViolationStates;

  public String orderBy;

  /**
   * The maximum age of risks that pass the filter, in days. When null, no age-based filtering is applied. Note that it
   * is not null by default however, so a null value must be set explicitly if desired.
   *
   * @since 1.27.0
   */
  public Integer maxDaysOld = DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD;

  /**
   * @since 1.147
   */
  public ExpirationDate expirationDate = ExpirationDate.ALL;

  /**
   * @since 1.149
   */
  public Set<String> repositoryIds = Collections.emptySet();

  public int maxResults = 1000;

  // default pageSize, can be overridden
  public int pageSize = 100;

  // return first page if page is not provided
  public int page = 0;

  // Firewall-only text filters. Null means no filter applied.
  public String componentName;

  public String repositoryPublicId;
}
