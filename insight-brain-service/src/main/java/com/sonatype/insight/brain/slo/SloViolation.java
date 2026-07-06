/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class SloViolation
{
  public String violationId;

  public String applicationPublicId;

  public String applicationInternalId;

  public String stage;

  public String policyId;

  public String policyName;

  public int threatLevel;

  public String threatCategory;

  public String vulnerabilityRefId; // CVE

  public Object cvssScore; // numeric or null (from constraint facts)

  public String cvssVector; // attack vector parsed from CVSS vector string

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String componentHash;

  public String dependencyType;

  public String reachabilityStatus; // "reachable" / "non-reachable" / "unknown"

  public String recommendedRemediationVersion;

  public Date openTime;

  public Date fixTime;

  public Date waiveTime;

  public Date legacyViolationTime;

  // Primitive so the flag is always serialized (true/false), letting consumers distinguish a legacy
  // (grandfathered) violation from a regular one. Legacy status does not alter SLO semantics.
  public boolean legacy;

  public SloWaiver waiver;
}
