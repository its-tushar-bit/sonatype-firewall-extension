/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentSearchPageResultDTO
{
  private final int pageNumber;

  private final int pageSize;

  private final long totalCount;

  private final ComponentSearchAggregatesDTO aggregates;

  private final List<ApplicationComponentMatchDTO> results;

  @JsonCreator
  public ComponentSearchPageResultDTO(
      @JsonProperty("pageNumber") int pageNumber,
      @JsonProperty("pageSize") int pageSize,
      @JsonProperty("totalCount") long totalCount,
      @JsonProperty("aggregates") ComponentSearchAggregatesDTO aggregates,
      @JsonProperty("results") List<ApplicationComponentMatchDTO> results)
  {
    this.pageNumber = pageNumber;
    this.pageSize = pageSize;
    this.totalCount = totalCount;
    this.aggregates = aggregates;
    this.results = results;
  }

  public int getPageNumber() {
    return pageNumber;
  }

  public int getPageSize() {
    return pageSize;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public ComponentSearchAggregatesDTO getAggregates() {
    return aggregates;
  }

  public List<ApplicationComponentMatchDTO> getResults() {
    return results;
  }
}
