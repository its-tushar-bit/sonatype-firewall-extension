/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.dto;

import com.sonatype.nexus.scm.api.model.RateLimitResponse;

public class ApiRateLimitDTO
{
  public String category;

  public int remaining;

  public int limit;

  public long resetEpochTime;

  public static ApiRateLimitDTO convert(RateLimitResponse rateLimitResponse) {
    ApiRateLimitDTO dto = new ApiRateLimitDTO();
    dto.category = rateLimitResponse.getCategory();
    dto.remaining = rateLimitResponse.getRemaining();
    dto.limit = rateLimitResponse.getLimit();
    dto.resetEpochTime = rateLimitResponse.getResetEpochTime();
    return dto;
  }
}
