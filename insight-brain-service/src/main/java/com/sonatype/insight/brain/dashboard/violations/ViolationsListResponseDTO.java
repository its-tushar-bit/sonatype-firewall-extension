/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated Nexus One Violations list response with violation card rows.
 */
public class ViolationsListResponseDTO
{
  public static final String SOURCE_INDEX = "index";

  /** Violation card rows mapped directly from the search index. */
  public List<ViolationRowDTO> violations = new ArrayList<>();

  public ViolationsListFacetsDTO facets;

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;

  public String source = SOURCE_INDEX;
}
