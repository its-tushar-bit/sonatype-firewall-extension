/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

import java.util.ArrayList;
import java.util.List;

public class SearchResultDTO
{
  public String searchQuery;

  public int page;

  public int pageSize;

  public int totalNumberOfHits;

  public boolean isExactTotalNumberOfHits;

  public List<GroupingByDTO> groupingByDTOS = new ArrayList<>();
}
