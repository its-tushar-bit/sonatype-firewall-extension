/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentSearchAggregatesDTO
{
  private final long totalAffectedApplications;

  private final long affectedComponents;

  private final long violatingComponents;

  private final long activeWaivers;

  @JsonCreator
  public ComponentSearchAggregatesDTO(
      @JsonProperty("totalAffectedApplications") long totalAffectedApplications,
      @JsonProperty("affectedComponents") long affectedComponents,
      @JsonProperty("violatingComponents") long violatingComponents,
      @JsonProperty("activeWaivers") long activeWaivers)
  {
    this.totalAffectedApplications = totalAffectedApplications;
    this.affectedComponents = affectedComponents;
    this.violatingComponents = violatingComponents;
    this.activeWaivers = activeWaivers;
  }

  public long getTotalAffectedApplications() {
    return totalAffectedApplications;
  }

  public long getAffectedComponents() {
    return affectedComponents;
  }

  public long getViolatingComponents() {
    return violatingComponents;
  }

  public long getActiveWaivers() {
    return activeWaivers;
  }
}
