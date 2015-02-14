/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;

/**
 * Aggregates the results of a search.
 *
 * @deprecated since 1.13.0, use {@link ApiSearchResultsDTOV2}
 */
@Deprecated
public class ApiSearchResultsDTO
{
  public ApiSearchCriteriaDTO criteria = new ApiSearchCriteriaDTO();

  public List<ApiSearchResultDTO> results = new ArrayList<>();
}
