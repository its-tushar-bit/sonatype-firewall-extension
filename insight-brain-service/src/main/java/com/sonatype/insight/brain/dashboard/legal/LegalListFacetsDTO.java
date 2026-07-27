/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Sidebar facet counts for Nexus One Legal findings.
 * <p>
 * {@code stages} is keyed by stage id. {@code organizations} and {@code applications} are keyed by
 * owner internal id. {@code licenseThreatGroups} is keyed by license threat group display name.
 * Only dimensions with a positive count are included; maps are omitted when empty.
 */
public class LegalListFacetsDTO
{
  public long totalFindings;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> stages;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> organizations;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> applications;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> licenseThreatGroups;

  /** Friendly organization display names keyed by the same internal ids as {@link #organizations}. */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> organizationNames;

  /** Friendly application display names keyed by the same internal ids as {@link #applications}. */
  @JsonInclude(Include.NON_NULL)
  public Map<String, String> applicationNames;
}
