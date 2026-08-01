/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.Set;

/**
 * Request body for the Nexus One Vulnerabilities list API (Martha V1 / CLM-42216).
 * <p>
 * {@code page} is 0-based. Null optional fields are handled in {@link VulnerabilitiesListService}.
 */
public class VulnerabilitiesListRequestDTO
{
  /** {@code myScanData} (default) or {@code catalog}. */
  public String tab;

  /**
   * Optional case-insensitive substring match across vulnerability id, description, and component
   * name.
   */
  public String search;

  /** 0-based page index. */
  public Integer page;

  /** Page size (validated in service; default 25). */
  public Integer pageSize;

  /**
   * Sort order. Defaults to {@code -cvssScore}. Only {@code cvssScore} / {@code -cvssScore} are
   * accepted in V1.
   */
  public String orderBy;

  /** When true (default), response includes {@link VulnerabilitiesListFacetsDTO}. */
  public Boolean includeFacets;

  /**
   * CVSS severity bands: {@code critical}, {@code high}, {@code medium}, {@code low}, {@code none}.
   * OR semantics across selected bands.
   */
  public Set<String> severities;

  /** Inclusive CVSS lower bound in {@code [0.0, 10.0]}. Null means no lower clamp. */
  public Float minCvssScore;

  /** Inclusive CVSS upper bound in {@code [0.0, 10.0]}. Null means no upper clamp. */
  public Float maxCvssScore;

  /** Component format / ecosystem ids (e.g. {@code maven}, {@code npm}). OR semantics. */
  public Set<String> ecosystems;

  /**
   * Organization ids, expanded to child organizations. OR semantics (CLM-43211).
   * <p>
   * These three scope-filters read fields already carried by every uncollapsed
   * SECURITY_VULNERABILITY doc, so they need no reindex: a vulnerability matches when any of its
   * (application, stage, component) hits matches.
   */
  public Set<String> organizationIds;

  /** Internal application ids. OR semantics (CLM-43211). */
  public Set<String> applicationIds;

  /** Stage type ids (e.g. {@code build}, {@code release}). OR semantics (CLM-43211). */
  public Set<String> stageIds;

  // --- Reserved until index/enrichment lands; non-null/non-empty values → 400 ---

  public Boolean knownExploited;

  public Boolean malware;

  public Boolean patchAvailable;

  public Set<String> cwes;

  public String publishedWindow;

  public Set<String> policyCompliance;
}
