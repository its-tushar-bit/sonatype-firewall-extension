/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since version.next
 */
@JsonInclude(Include.NON_NULL)
public class ApiSuccessMetricsRetentionPolicyDTO
{
  public boolean inheritPolicy;

  public boolean enablePurging;

  public ApiAgeDTO maxAge;

  public ApiSuccessMetricsRetentionPolicyDTO() {
  }

  public ApiSuccessMetricsRetentionPolicyDTO(boolean inheritPolicy, boolean enablePurging, ApiAgeDTO maxAge) {
    this.inheritPolicy = inheritPolicy;
    this.enablePurging = enablePurging;
    this.maxAge = maxAge;
  }
}
