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

  /** Violation state (Open / Waived / Legacy). All three are indexed via waiver-status clauses. */
  public PolicyViolationStateFilter policyViolationStates;

  /**
   * Application category / tag ids. Resolved to category names (O(ids) via Tag DAO) and applied as
   * an index TERMS clause on multi-valued {@code applicationCategoryName} already denormalized onto
   * {@code POLICY_VIOLATION} docs. Unknown ids resolve to a no-match clause; docs without the field
   * (pre-denorm estates) do not match.
   */
  public Set<String> applicationCategoryIds;

  /**
   * Age window in days ("first seen" cutoff). Not range-queryable on {@code POLICY_VIOLATION} docs
   * ({@code openTime} is SQL page-enrich only) — rejected by
   * {@link ViolationsListRequestValidator}.
   */
  public Integer ageInDays;

  /**
   * Waiver-type filter (CLM-42261): {@code true} narrows to auto-waived violations, {@code false} to
   * manually-waived only, {@code null} applies no waiver-type narrowing. Both non-null values imply the
   * WAIVED state, so combining a non-null value with an OPEN {@code policyViolationStates} filter yields
   * an intentionally empty result set.
   */
  public Boolean waivedWithAutoWaiver;

  /**
   * Sort order. Defaults to {@code -policyThreatLevel} (highest threat first). Only threat-level
   * ordering is supported; other values return 400.
   */
  public String orderBy;

  /** When true (default), response includes {@link ViolationsListFacetsDTO}. */
  public Boolean includeFacets;

  /**
   * Optional case-insensitive substring match against organization names for the Organizations
   * facet map. When non-blank, {@link ViolationsListFacetsDTO#organizations} (and matching
   * {@link ViolationsListFacetsDTO#organizationNames}) is replaced with name-matched owners that
   * have a positive violation count under the active list filters (capped at the same size as the
   * uncapped top-N facet map). Does not narrow list rows — only the Organizations facet map.
   * Blank/null keeps top-by-count organization facets. Max length matches {@code search}.
   */
  public String organizationFacetSearch;

  /**
   * Optional case-insensitive substring match against application names for the Applications
   * facet map. Same replace semantics as {@link #organizationFacetSearch} (does not narrow list
   * rows).
   */
  public String applicationFacetSearch;

  /**
   * Optional exact component hash filter for component-detail Policy Violations (CLM-43958).
   * When non-blank, list rows and facets narrow to {@code POLICY_VIOLATION} documents with that
   * {@code componentHash}. Blank/null leaves estate-wide behavior unchanged.
   */
  public String componentHash;
}
