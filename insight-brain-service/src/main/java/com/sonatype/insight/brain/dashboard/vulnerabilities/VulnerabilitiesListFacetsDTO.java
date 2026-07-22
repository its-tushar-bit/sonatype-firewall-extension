/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
