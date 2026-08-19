/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Set;

import com.sonatype.insight.brain.model.component.InnerSourceData;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiDependencyDataDTO
{
  @JsonInclude(Include.NON_NULL)
  public Boolean directDependency;

  @JsonInclude(Include.NON_NULL)
  public Boolean innerSource;

  @JsonInclude(Include.NON_NULL)
  public Set<String> parentComponentPurls;

  @JsonInclude(Include.NON_NULL)
  public Set<InnerSourceData> innerSourceData;

  /**
   * Classifies a component's dependency type from its report-derived dependency data. Single source of truth shared by
   * the Development Priorities page and the SLO violation feed so the two never drift. Reads only this type's fields
   * and
   * treats {@code null} data as {@link PrioritizedComponent#DEPENDENCY_TYPE_UNKNOWN}.
   */
  public static String dependencyType(final ApiDependencyDataDTO dependencyData) {
    if (dependencyData == null) {
      return PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN;
    }

    if (dependencyData.innerSource != null && dependencyData.innerSource) {
      if (dependencyData.directDependency != null && dependencyData.directDependency) {
        return PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE_DIRECT;
      }
      else {
        return PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE_TRANSITIVE;
      }
    }
    else if (dependencyData.directDependency != null && dependencyData.directDependency) {
      return PrioritizedComponent.DEPENDENCY_TYPE_DIRECT;
    }
    else {
      return PrioritizedComponent.DEPENDENCY_TYPE_TRANSITIVE;
    }
  }
}
