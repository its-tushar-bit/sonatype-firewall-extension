/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated Nexus One Legal list response with legal finding card rows.
 */
public class LegalListResponseDTO
{
  public static final String SOURCE_INDEX = "index";

  /** Legal finding card rows mapped directly from the search index. */
  public List<LegalRowDTO> findings = new ArrayList<>();

  public LegalListFacetsDTO facets;

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;

  public String source = SOURCE_INDEX;
}
