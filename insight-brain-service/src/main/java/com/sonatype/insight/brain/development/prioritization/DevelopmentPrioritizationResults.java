/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;

public class DevelopmentPrioritizationResults
{
  private final List<PrioritizedComponent> topPriorities;

  private final ApiPageResult<PrioritizedComponent> additionalPriorities;

  private final ApiPageResult<PrioritizedComponent> priorities;

  public DevelopmentPrioritizationResults(
      final List<PrioritizedComponent> topPriorities,
      final ApiPageResult<PrioritizedComponent> additionalPriorities,
      final ApiPageResult<PrioritizedComponent> priorities
  )
  {
    this.topPriorities = topPriorities;
    this.additionalPriorities = additionalPriorities;
    this.priorities = priorities;
  }

  public List<PrioritizedComponent> getTopPriorities() {
    return topPriorities;
  }

  public ApiPageResult<PrioritizedComponent> getAdditionalPriorities() {
    return additionalPriorities;
  }

  public ApiPageResult<PrioritizedComponent> getPriorities() {
    return priorities;
  }

  @Override
  public String toString() {

    return "DevelopmentPrioritizationResults{" +
        "topPriorities=" + topPriorities +
        ", additionalPriorities=" + additionalPriorities +
        ", priorities=" + priorities + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DevelopmentPrioritizationResults that = (DevelopmentPrioritizationResults) o;
    return Objects.equals(topPriorities, that.topPriorities) &&
        Objects.equals(additionalPriorities, that.additionalPriorities) &&
        Objects.equals(priorities, that.priorities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topPriorities, additionalPriorities, priorities);
  }
}
