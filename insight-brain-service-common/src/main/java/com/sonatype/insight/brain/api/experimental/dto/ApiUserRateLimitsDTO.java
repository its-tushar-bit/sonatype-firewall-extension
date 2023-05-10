/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.dto;

import java.util.List;
import java.util.Set;

public class ApiUserRateLimitsDTO
{
  public String user;

  public Set<String> definingOwnerIds;

  public Set<String> associatedApplicationIds;

  public List<ApiRateLimitDTO> rateLimits;
}
