/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response from a bulk legacy-violation mutation.")
public class ApiLegacyViolationChangeResponseDTO
{
  @Schema(description = "Number of policy violations whose legacy status changed in this call.")
  public final int changedPolicyViolationCount;

  @JsonCreator
  public ApiLegacyViolationChangeResponseDTO(
      @JsonProperty("changedPolicyViolationCount") int changedPolicyViolationCount)
  {
    this.changedPolicyViolationCount = changedPolicyViolationCount;
  }
}
