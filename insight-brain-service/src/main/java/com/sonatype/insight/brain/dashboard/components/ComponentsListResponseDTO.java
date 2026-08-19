/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dashboard.ComponentRiskDTO;

/**
 * Paginated Nexus One Components list response with portfolio component rows.
 */
public class ComponentsListResponseDTO
{
  public static final String SOURCE_INDEX = "index";

  /** Component-centric rows — {@link ComponentRiskDTO} parity with Classic dashboard. */
  public List<ComponentRiskDTO> components = new ArrayList<>();

  public ComponentsListFacetsDTO facets;

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;

  public String source = SOURCE_INDEX;
}
