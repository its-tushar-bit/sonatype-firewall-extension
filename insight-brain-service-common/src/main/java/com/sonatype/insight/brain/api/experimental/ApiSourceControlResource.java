/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import com.sonatype.insight.brain.api.experimental.dto.ApiOwnerUserRateLimitsDTO;
import com.sonatype.insight.brain.model.OwnerType;

public interface ApiSourceControlResource
{
  ApiOwnerUserRateLimitsDTO getRateLimits(OwnerType ownerType, String ownerId);
}
