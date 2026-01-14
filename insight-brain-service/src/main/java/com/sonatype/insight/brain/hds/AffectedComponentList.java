/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for a list of components affected by a CVE vulnerability.
 * Used for proper JSON deserialization from HDS API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AffectedComponentList
{
  private final List<AffectedComponentDTO> components;

  private final String nextCursor;

  private final Boolean hasMore;

  public AffectedComponentList() {
    this.components = new ArrayList<>();
    this.nextCursor = null;
    this.hasMore = null;
  }

  @JsonCreator
  public AffectedComponentList(
      @JsonProperty("components") List<AffectedComponentDTO> components,
      @JsonProperty("nextCursor") String nextCursor,
      @JsonProperty("hasMore") Boolean hasMore)
  {
    this.components = components != null ? components : new ArrayList<>();
    this.nextCursor = nextCursor;
    this.hasMore = hasMore;
  }

  public List<AffectedComponentDTO> getComponents() {
    return components;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public Boolean getHasMore() {
    return hasMore;
  }
}
