/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiChartVisibilityDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.apache.shiro.authz.UnauthorizedException;

class StatsChartVisibilityService
{
  ApiChartVisibilityDto getChartVisibilityForUser() {
    try {
      checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    }
    catch (final UnauthorizedException unauthorizedException) {
      return new ApiChartVisibilityDto(false);
    }

    return new ApiChartVisibilityDto(true);
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
    // implemented via @Authorize
  }
}
