/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

/**
 * Request body for the Nexus One Components list API.
 * <p>
 * {@code page} is 0-based. Null optional fields are handled in {@link ComponentsListService}.
 */
public class ComponentsListRequestDTO
{
  /** Optional case-insensitive substring match across component name, hash, and coordinates. */
  public String search;

  /** 0-based page index. */
  public Integer page;

  /** Page size (validated in service). */
  public Integer pageSize;

  /** Organization owner ids (internal). Unioned with {@link #applicationIds} when both set. */
  public Set<String> organizationIds;

  /** Application internal ids. */
  public Set<String> applicationIds;

  /**
   * Optional distinct component hashes. Used internally when stage/threat filters rewrite the query
   * via violation-scope discovery; clients may also send an explicit hash filter.
   */
  public Set<String> componentHashes;

  /** Licensed stage ids (e.g. develop, build). */
  public Set<String> stageIds;

  /** Application category / tag ids. */
  public Set<String> tagIds;

  public PolicyThreatCategoryFilter policyThreatCategories;

  public PolicyThreatLevelFilter policyThreatLevelRange;

  /** One min/max pair per selected threat bucket; combined with OR in violation-scoped queries. */
  public List<PolicyThreatLevelFilter> policyThreatLevelRanges;

  public PolicyViolationStateFilter policyViolationStates;

  /**
   * Classic {@link com.sonatype.insight.brain.dashboard.ComponentRiskOrderByEnum} token,
   * optionally prefixed with {@code -} for descending. Default {@code -TOTAL_RISK}.
   */
  public String orderBy;

  /** When true, response includes {@link ComponentsListFacetsDTO}. */
  public Boolean includeFacets;

  /** Shallow copy of all request fields (one home when fields are added). */
  public ComponentsListRequestDTO copy() {
    ComponentsListRequestDTO copy = new ComponentsListRequestDTO();
    copy.search = search;
    copy.page = page;
    copy.pageSize = pageSize;
    copy.organizationIds = organizationIds;
    copy.applicationIds = applicationIds;
    copy.componentHashes = componentHashes;
    copy.stageIds = stageIds;
    copy.tagIds = tagIds;
    copy.policyThreatCategories = policyThreatCategories;
    copy.policyThreatLevelRange = policyThreatLevelRange;
    copy.policyThreatLevelRanges = policyThreatLevelRanges;
    copy.policyViolationStates = policyViolationStates;
    copy.orderBy = orderBy;
    copy.includeFacets = includeFacets;
    return copy;
  }

  /** Copy with {@link #componentHashes} replaced. */
  public ComponentsListRequestDTO withComponentHashes(final Set<String> hashes) {
    ComponentsListRequestDTO copy = copy();
    copy.componentHashes = hashes;
    return copy;
  }
}
