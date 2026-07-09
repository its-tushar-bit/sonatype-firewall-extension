/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

/**
 * Request body for the Nexus One Applications list API.
 * <p>
 * {@code page} is 0-based. Null optional fields are handled in {@link ApplicationsListService}.
 */
public class ApplicationsListRequestDTO
{
  /** Optional case-insensitive substring match across name, public ID, and organization. */
  public String search;

  /** 0-based page index. */
  public Integer page;

  /** Page size (validated in service). */
  public Integer pageSize;

  /** Organization owner ids (internal). Unioned with {@link #applicationIds} when both set. */
  public Set<String> organizationIds;

  /** Application internal ids. */
  public Set<String> applicationIds;

  /** Licensed stage ids (e.g. develop, build). */
  public Set<String> stageIds;

  /** Application category / tag ids. */
  public Set<String> tagIds;

  public PolicyThreatCategoryFilter policyThreatCategories;

  public PolicyThreatLevelFilter policyThreatLevelRange;

  public PolicyViolationStateFilter policyViolationStates;

  /**
   * Reserved for latest-evaluation default sort. Non-null values return 400 until supported.
   */
  public String orderBy;

  /** When true, response includes {@link ApplicationsListFacetsDTO}. */
  public Boolean includeFacets;
}
