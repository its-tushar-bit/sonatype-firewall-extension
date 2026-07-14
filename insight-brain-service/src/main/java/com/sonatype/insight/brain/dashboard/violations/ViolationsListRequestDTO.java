/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;

/**
 * Request body for the Nexus One Violations list API (Martha V1).
 * <p>
 * {@code page} is 0-based. Null optional fields are handled in {@link ViolationsListService}.
 * Filters that cannot yet be resolved from the search index are rejected with a 400 by
 * {@link ViolationsListRequestValidator} rather than being silently ignored.
 */
public class ViolationsListRequestDTO
{
  /**
   * Optional case-insensitive substring match across component name, application name,
   * application public id, organization name, and policy name.
   */
  public String search;

  /** 0-based page index. */
  public Integer page;

  /** Page size (validated in service). */
  public Integer pageSize;

  /** Organization owner ids (internal). Unioned with {@link #applicationIds} when both set. */
  public Set<String> organizationIds;

  /** Application internal ids. */
  public Set<String> applicationIds;

  /** Licensed stage ids (e.g. {@code build}, {@code release}). */
  public Set<String> stageIds;

  /** Policy threat categories (Security / License / Quality / Other). */
  public PolicyThreatCategoryFilter policyThreatCategories;

  /** Inclusive threat-level range (0–10). */
  public PolicyThreatLevelFilter policyThreatLevelRange;

  /** Violation state (Open / Waived). Legacy is not yet indexed and is rejected. */
  public PolicyViolationStateFilter policyViolationStates;

  /**
   * Application category / tag ids. Not indexed on violation documents yet — rejected until the
   * filter sidebar work (CLM-42258) lands.
   */
  public Set<String> applicationCategoryIds;

  /**
   * Age window in days ("first seen" cutoff). Violation timestamps are not indexed yet — rejected
   * until the filter sidebar work (CLM-42258) lands.
   */
  public Integer ageInDays;

  /**
   * Auto-waiver filter. Handled by the dedicated auto-waiver story (CLM-42261) — rejected here so
   * clients get a clear 400 rather than a silently ignored filter.
   */
  public Boolean waivedWithAutoWaiver;

  /**
   * Sort order. Defaults to {@code -policyThreatLevel} (highest threat first). Only threat-level
   * ordering is supported; other values return 400.
   */
  public String orderBy;

  /** When true (default), response includes {@link ViolationsListFacetsDTO}. */
  public Boolean includeFacets;
}
