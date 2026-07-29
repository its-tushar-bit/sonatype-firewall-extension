/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A single Nexus One violation card row, mapped from a {@code POLICY_VIOLATION} search index hit
 * with a page-scoped SQL enrich for {@link #firstOccurredTime} ({@code PolicyViolation.openTime}).
 * <p>
 * Fields the prototype {@code RealViolation} carries but that are not yet available without reindex
 * — {@code applicationCategory} and the CVE reference — remain intentionally absent.
 */
@JsonInclude(Include.NON_NULL)
public class ViolationRowDTO
{
  /** Native policy violation id — the drill-in / detail link id. */
  public String policyViolationId;

  /** Policy threat level 0–10. */
  public Integer threatLevel;

  /** Derived severity band ({@code low}/{@code moderate}/{@code severe}/{@code critical}). */
  public String severity;

  /** Policy threat category ({@code security}/{@code license}/{@code quality}/{@code other}). */
  public String threatCategory;

  public String policyId;

  public String policyName;

  public String organizationId;

  public String organizationName;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String componentName;

  /** Component version from {@link #componentIdentifier} coordinates when present. */
  public String componentVersion;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  /** Latest policy evaluation stage (display name). */
  public String stage;

  /** Violation state ({@code OPEN} / {@code WAIVED}). */
  public String state;

  /** True when the violation was waived by an auto-waiver rather than a manual waiver. */
  public boolean waivedWithAutoWaiver;

  public String constraintName;

  /**
   * Epoch millis when the violation was first opened ({@code PolicyViolation.openTime}), enriched
   * per page from SQL. Omitted when unknown so the FE can skip the "first seen" line.
   */
  public Long firstOccurredTime;
}
