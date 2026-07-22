/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated Nexus One Vulnerabilities list response with estate-distinct vulnerability rows.
 */
public class VulnerabilitiesListResponseDTO
{
  public static final String SOURCE_INDEX = "index";

  public static final String SOURCE_CATALOG = "catalog";

  /** Vulnerability card rows (one per distinct {@code vulnerabilityId} in scope). */
  public List<VulnerabilityRowDTO> vulnerabilities = new ArrayList<>();

  public VulnerabilitiesListFacetsDTO facets;

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;

  public String source = SOURCE_INDEX;
}
