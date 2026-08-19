/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Legacy-violation status for an application or organization.")
public class ApiLegacyViolationStatusDTO
{
  @Schema(description = "Whether legacy violations are enabled. null means inherit from the parent organization.")
  public Boolean enabled;

  @Schema(
      description = "Whether legacy violations are enabled in the parent organization. null when no parent set this value.")
  @JsonInclude(Include.NON_NULL)
  @JsonProperty(access = Access.READ_ONLY)
  public Boolean enabledInParent;

  @Schema(description = "The name of the organization the legacy status is inherited from, or null if not inherited.")
  @JsonInclude(Include.NON_NULL)
  @JsonProperty(access = Access.READ_ONLY)
  public String inheritedFromOrganizationName;

  @Schema(
      description = "Whether children (orgs and apps) are allowed to override the legacy status. Organization-only."
          + " Primitive boolean: an absent or null value in the request body is treated as false.")
  public boolean allowOverride;

  @Schema(description = "Whether the legacy status can be changed for this owner.")
  @JsonProperty(access = Access.READ_ONLY)
  public boolean allowChange;
}
