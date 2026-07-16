/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Sidebar facet counts for Martha V1 Violations.
 * <p>
 * {@code states} and {@code threatCategories} are keyed by a stable enum-style name
 * ({@code OPEN}/{@code WAIVED}, {@code security}/{@code license}/…). {@code waiverTypes} is keyed by
 * {@code AUTO}/{@code MANUAL} (CLM-42261). {@code stages} is keyed by stage id, and
 * {@code organizations}/{@code applications} are keyed by owner internal id.
 * Only dimensions with a positive count are included; maps are omitted when empty.
 * <p>
 * Counts reflect the active list query scope (narrowing filters reduce facet counts as well as rows;
 * see CLM-42258). Organization and application counts are discovery-capped (see {@link ViolationsListFacetsBuilder})
 * because violation volume is far higher than application volume; full aggregate facets land under
 * the scale-test story (CLM-42262).
 */
public class ViolationsListFacetsDTO
{
  public long totalViolations;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> states;

  /** Waiver-type counts (CLM-42261), keyed {@code AUTO} / {@code MANUAL}. */
  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> waiverTypes;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> threatCategories;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> stages;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> organizations;

  @JsonInclude(Include.NON_NULL)
  public Map<String, Long> applications;
}
