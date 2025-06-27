/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.search.index.FieldIdentifier;

/**
 * @since 1.88
 */
public class GroupingByDTO
{
  public FieldIdentifier groupIdentifier;

  public String groupBy;

  public String additionalInfo;

  public List<SearchResultItemDTO> searchResultItemDTOS = new ArrayList<>();
}
