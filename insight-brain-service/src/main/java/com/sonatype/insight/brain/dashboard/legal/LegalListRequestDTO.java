/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

/**
 * Request body for the Nexus One Legal list API.
 * <p>
 * {@code page} is 0-based. Null optional fields are handled in {@link LegalListService}.
 */
public class LegalListRequestDTO
{
  /**
   * Optional case-insensitive substring match across component name, application name,
   * application public id, organization name, license name, and license threat group name.
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

  /** License threat group display names. */
  public Set<String> licenseThreatGroupNames;

  /** Inclusive license threat-level range (0–10), indexed on {@code componentLicenseThreatLevel}. */
  public PolicyThreatLevelFilter licenseThreatLevelRange;

  /**
   * Sort order. Defaults to {@code -licenseThreatLevel} (highest threat first). Only license
   * threat-level ordering is supported; other values return 400.
   */
  public String orderBy;

  /** When true (default), response includes {@link LegalListFacetsDTO}. */
  public Boolean includeFacets;
}
