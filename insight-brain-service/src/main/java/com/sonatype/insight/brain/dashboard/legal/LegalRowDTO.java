/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.legal;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A single Nexus One legal finding row, mapped directly from a {@code LEGAL_VIOLATION} search
 * index hit.
 */
@JsonInclude(Include.NON_NULL)
public class LegalRowDTO
{
  /** Composite legal finding id — the drill-in / detail link id. */
  public String legalFindingId;

  /** License threat level 0–10. */
  public Integer threatLevel;

  /** Derived severity band ({@code low}/{@code moderate}/{@code severe}/{@code critical}). */
  public String severity;

  public String licenseId;

  public String licenseName;

  public String licenseThreatGroupName;

  public String organizationId;

  public String organizationName;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String componentName;

  /** Component version from {@link #componentIdentifier} coordinates when present. */
  public String componentVersion;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String componentHash;

  /** Latest policy evaluation stage (display name). */
  public String stage;

  public String reportId;
}
