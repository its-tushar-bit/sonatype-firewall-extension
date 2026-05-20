/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.consumption.ActivityType;

import jakarta.annotation.Nullable;

/**
 * Maps HDS request paths to consumption activity types. Paths not in this allow-list
 * are not counted for consumption. This is an explicit allow-list — new HDS endpoints
 * are not counted by default.
 *
 * @since 1.204
 */
public final class HdsPathActivityMapper
{
  private static final Map<String, ActivityType> PATH_MAPPINGS;

  static {
    LinkedHashMap<String, ActivityType> m = new LinkedHashMap<>();
    m.put("rest/component/details/evaluation", ActivityType.COMPONENT_DETAILS);
    m.put("rest/component/details/integration", ActivityType.COMPONENT_DETAILS);
    m.put("/componentDetails", ActivityType.COMPONENT_DETAILS);
    // m.put("rest/component/versions", ActivityType.API);
    m.put("rest/component/version-scoring", ActivityType.VERSION_RECOMMENDATION);
    m.put("rest/component/dependencies", ActivityType.VERSION_RECOMMENDATION);
    m.put("rest/component/signatures/vulnerability", ActivityType.REACHABILITY);
    m.put("rest/vulnerability/affected", ActivityType.DEVELOPER_PRIORITIES);
    PATH_MAPPINGS = Collections.unmodifiableMap(m);
  }

  static Map<String, ActivityType> getPathMappings() {
    return PATH_MAPPINGS;
  }

  private HdsPathActivityMapper() {
  }

  /**
   * Resolve the activity type for an HDS request path.
   *
   * @return the activity type, or null if the path is not counted
   */
  @Nullable
  public static ActivityType resolve(String requestPath) {
    if (requestPath == null) {
      return null;
    }
    for (Map.Entry<String, ActivityType> entry : PATH_MAPPINGS.entrySet()) {
      if (requestPath.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

}
