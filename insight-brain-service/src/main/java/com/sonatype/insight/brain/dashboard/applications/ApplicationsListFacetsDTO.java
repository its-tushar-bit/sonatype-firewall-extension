/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Sidebar facet counts for Martha V1 Applications (CLM-42228 Phase B).
 * <p>
 * Maps are keyed by owner id (organization / application internal id / stage id).
 * Bucketed counts land in a follow-up; until then maps are omitted from JSON.
 */
public class ApplicationsListFacetsDTO
{
  /**
   * RBAC-scoped total from the list query. Per-org/application facet counts are derived from at most
   * {@link ApplicationsListFacetsBuilder#MAX_FACET_DISCOVERY_HITS} discovered APPLICATION hits.
   */
  public long totalApplications;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> organizations;

  /**
   * Display names for {@link #organizations} keys (internal organization ids).
   * Omitted keys may fall back to the raw id on the client.
   */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> organizationNames;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> applications;

  /**
   * Display names for {@link #applications} keys (internal application ids).
   * Omitted keys may fall back to the raw id on the client.
   */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> applicationNames;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> stages;

  /**
   * Policy type ({@code security}/{@code license}/{@code quality}/{@code other}) to the number of
   * distinct applications holding at least one violation of that type (CLM-43211).
   */
  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> policyTypes;

  /**
   * Violation state ({@code OPEN}/{@code WAIVED}/{@code LEGACY_VIOLATION}) to the number of distinct
   * applications holding at least one violation in that state.
   * <p>
   * These buckets do not partition the estate: one application can hold violations in several states
   * and is counted under each, so the values do not sum to {@link #totalApplications}.
   */
  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> violationStates;
}
