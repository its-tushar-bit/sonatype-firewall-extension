/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsScmFeedbackStatIncrementDto;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

@Named
public class ScmStatService
{
  private final ApplicationCountHistoryService applicationCountHistoryService;

  private final DateTimeService dateTimeService;

  @Inject
  ScmStatService(
      final ApplicationCountHistoryService applicationCountHistoryService,
      final DateTimeService dateTimeService)
  {
    this.applicationCountHistoryService = applicationCountHistoryService;
    this.dateTimeService = dateTimeService;
  }

  public List<ApiIntegrationsScmFeedbackStatIncrementDto> getScmFeedbackUsageStatsOverTime(
      final long incrementSizeMillis,
      final int numberOfIncrements)
  {
    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    final List<ApiIntegrationsScmFeedbackStatIncrementDto> results = new ArrayList<>();

    final long now = dateTimeService.getCurrentTimeMs();

    long currentUpperBound = now - (incrementSizeMillis * numberOfIncrements) + incrementSizeMillis;

    for (int i = 0; i < numberOfIncrements; i++) {
      final Date timeOfIncrement = new Date(currentUpperBound);

      final ApplicationCountHistory applicationCountHistory =
          applicationCountHistoryService.getApplicationHistoryCount(timeOfIncrement);

      results.add(new ApiIntegrationsScmFeedbackStatIncrementDto(
          currentUpperBound,
          applicationCountHistory.getApplicationCount(),
          applicationCountHistory.getScmFeedbackEnabledCount()));

      currentUpperBound += incrementSizeMillis;
    }

    return results;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
    // The @Authorize annotation provides the implementation for this function
  }
}
