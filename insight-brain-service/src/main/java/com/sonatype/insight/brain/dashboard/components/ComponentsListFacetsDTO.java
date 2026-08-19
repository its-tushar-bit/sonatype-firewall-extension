/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Sidebar facet counts for Martha V1 Components (CLM-42214).
 * <p>
 * Count maps are keyed by owner id (organization / application internal id / stage id).
 * Parallel name maps supply friendly labels for the filter rail (ids alone are opaque UUIDs).
 */
public class ComponentsListFacetsDTO
{
  /**
   * RBAC-scoped distinct {@code componentHash} total from the list query.
   */
  public long totalComponents;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> organizations;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> applications;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> stages;

  /** Internal organization id → display name for {@link #organizations} keys. */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> organizationNames;

  /** Internal application id → display name for {@link #applications} keys. */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> applicationNames;

  /**
   * Stage id → display name for {@link #stages} keys (CLM-43211).
   * <p>
   * Unlike the Applications list, a component row carries no stage risk breakdown, so the rail has
   * no page-row fallback to label a stage with — the names have to come from here.
   */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> stageNames;
}
