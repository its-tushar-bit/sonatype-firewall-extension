/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Sidebar facet counts for Martha V1 Applications (CLM-42228 Phase B).
 * <p>
 * Maps are keyed by owner id (organization / application internal id / stage id).
 * Bucketed counts land in a follow-up; until then maps are omitted from JSON.
 */
public class ApplicationsListFacetsDTO
{
  public long totalApplications;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> organizations;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> applications;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> stages;
}
