/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.List;

/**
 * Request body for Nexus One component where-used APIs (CLM-43959 / CLM-44667).
 * <p>
 * {@code page} is 0-based. Optional {@code nameSearch} / {@code includeIds} / {@code organizationId}
 * are hash-scoped filters for Path search and URL resolve — not a full-estate scan.
 */
public class ComponentUsageRequestDTO
{
  /** Exact component hash (required). */
  public String componentHash;

  /** 0-based page index. */
  public Integer page;

  /** Page size. */
  public Integer pageSize;

  /**
   * Optional case/whitespace-insensitive substring match on application or organization name
   * (normalized via {@code NameHelper}).
   */
  public String nameSearch;

  /**
   * Optional ids to force-include in the page (e.g. URL-selected application/organization), even when
   * they do not match {@code nameSearch}. Still requires hash + RBAC (+ org filter when set).
   */
  public List<String> includeIds;

  /**
   * Optional organization id filter for the applications endpoint. Ignored for organizations.
   */
  public String organizationId;
}
