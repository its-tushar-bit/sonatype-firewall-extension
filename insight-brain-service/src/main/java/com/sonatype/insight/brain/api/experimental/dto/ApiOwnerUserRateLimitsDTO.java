/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.dto;

import java.util.List;

public class ApiOwnerUserRateLimitsDTO
{
  public String ownerType;

  public String ownerId;

  public String ownerPublicId;

  public String ownerName;

  public List<ApiUserRateLimitsDTO> userRateLimits;
}
