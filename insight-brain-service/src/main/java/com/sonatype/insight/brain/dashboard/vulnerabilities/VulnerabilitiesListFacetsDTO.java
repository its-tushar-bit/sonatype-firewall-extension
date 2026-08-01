/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Facet counts for the Nexus One Vulnerabilities list.
 */
public class VulnerabilitiesListFacetsDTO
{
  /** Distinct estate vulnerability count matching the current query (same as response {@code total}). */
  public long totalVulnerabilities;

  /**
   * CVSS band → distinct vulnerability count ({@code critical}|{@code high}|{@code medium}|{@code low}|{@code none}).
   */
  public Map<String, Long> severities = new LinkedHashMap<>();

  /** Component format / ecosystem → distinct vulnerability count. */
  public Map<String, Long> ecosystems = new LinkedHashMap<>();

  /**
   * Scope facets (CLM-43211): organization / application / stage id → distinct vulnerability count.
   * <p>
   * Unlike severity and ecosystem, these vary across the uncollapsed hits for one vulnerability — a
   * CVE present in twenty applications counts once in each — so they are aggregated over
   * SECURITY_VULNERABILITY docs rather than bucketed from the collapsed page. Absent when the index
   * backend cannot serve the aggregation.
   */
  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> organizations;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> applications;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> stages;

  /** Display names for facet ids, so the rail never falls back to rendering raw internal ids. */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> organizationNames;

  @JsonInclude(Include.NON_NULL)
  public Map<String, String> applicationNames;

  @JsonInclude(Include.NON_NULL)
  public Map<String, String> stageNames;
}
