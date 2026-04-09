/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.173
 */
public class RepositoryResultsDetailsResponseDto
{
  public List<RepositoryResultsDetailsDto> repositoryResultsDetails = new ArrayList<>();

  public boolean hasNextPage;

  /**
   * Total count of results for the bulk waiver page before user-applied filters.
   * Only populated when isBulkWaiverPage = true in the request.
   *
   * @since 1.203
   */
  public Long totalCount;

  /**
   * Total count of results matching the applied filters before pagination.
   * Only populated when isBulkWaiverPage = true in the request.
   *
   * @since 1.203
   */
  public Long filterCount;
}
