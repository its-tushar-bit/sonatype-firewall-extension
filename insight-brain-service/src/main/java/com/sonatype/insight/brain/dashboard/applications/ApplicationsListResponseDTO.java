/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;

/**
 * Paginated Nexus One Applications list response with evaluation card rows.
 */
public class ApplicationsListResponseDTO
{
  public static final String SOURCE_INDEX = "index";

  /** Evaluation card rows — {@link ApplicationRiskScoreDTO} parity with Classic dashboard. */
  public List<ApplicationRiskScoreDTO> applications = new ArrayList<>();

  public ApplicationsListFacetsDTO facets;

  public long total;

  public int page;

  public int pageSize;

  public boolean hasNextPage;

  public String source = SOURCE_INDEX;
}
