/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.88
 */
public class SearchResultDTO
{
  public String searchQuery;

  public int page;

  public int pageSize;

  public long totalNumberOfHits;

  public boolean isExactTotalNumberOfHits;

  public List<GroupingByDTO> groupingByDTOS = new ArrayList<>();

  public int countSearchResults() {
    int resultRecordCount = 0;
    for (GroupingByDTO groupingByDTO : groupingByDTOS) {
      resultRecordCount += groupingByDTO.searchResultItemDTOS.size();
    }
    return resultRecordCount;
  }

  @JsonInclude(Include.NON_NULL)
  public List<String> searchAfter;
}
