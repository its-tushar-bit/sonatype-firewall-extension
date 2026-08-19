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

  /**
   * Catalog-only: CISA KEV / known exploitation ({@code exploitationKnown} on HDS). My Scan Data
   * rejects non-null values (estate index has no KEV field yet).
   */
  public Boolean knownExploited;

  /**
   * Catalog-only: malware flag ({@code hasMalware} on HDS). My Scan Data rejects non-null values.
   */
  public Boolean malware;

  /** Inclusive EPSS lower bound in {@code [0.0, 1.0]}. Catalog-only; My Scan rejects non-null. */
  public Float minEpssScore;

  /** Inclusive EPSS upper bound in {@code [0.0, 1.0]}. Catalog-only; My Scan rejects non-null. */
  public Float maxEpssScore;

  /** Reserved — not supported on either tab yet. */
  public Boolean patchAvailable;

  /** Catalog-only: CWE ids (OR). My Scan Data rejects non-empty values. */
  public Set<String> cwes;

  /**
   * Catalog-only relative publish window: {@code 30d}|{@code 90d}|{@code 1y}|{@code 2y}. My Scan
   * Data rejects non-blank values.
   */
  public String publishedWindow;

  /** Reserved — not supported on either tab yet. */
  public Set<String> policyCompliance;
}
