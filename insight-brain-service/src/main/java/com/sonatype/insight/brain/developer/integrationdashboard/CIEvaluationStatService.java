/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.dataaccess.ApplicationCountHistoryDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

/**
 * @since 1.162
 */
@Named
public class CIEvaluationStatService
{
  public static final Long CICD_TRIGGERED_EVALUATION_CUT_OFF_MS = 7257600000L;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationCountHistoryDAO applicationCountHistoryDAO;

  private final DateTimeService dateTimeService;

  @Inject
  public CIEvaluationStatService(
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationCountHistoryDAO applicationCountHistoryDAO,
      final DateTimeService dateTimeService
  )
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationCountHistoryDAO = applicationCountHistoryDAO;
    this.dateTimeService = dateTimeService;
  }

  List<ApiIntegrationsCiCdStatIncrementDto> getCiCdUsageStatsOverTime(
      final long incrementSizeMillis,
      final int numberOfIncrements
  )
  {
    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    final List<ApiIntegrationsCiCdStatIncrementDto> results = new ArrayList<>();

    final long now = dateTimeService.getCurrentTimeMs();

    long currentUpperBound = now - (incrementSizeMillis * numberOfIncrements) + incrementSizeMillis;

    for (int i = 0; i < numberOfIncrements; i++) {
      final Date timeOfIncrement = new Date(currentUpperBound);

      final int totalNumberOfAppsAtTime =
          applicationCountHistoryDAO.getApplicationCountAtOrDefault(timeOfIncrement);

      final int totalNumberOfAppsUsingCiCDAtTime =
          getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(timeOfIncrement);

      results.add(new ApiIntegrationsCiCdStatIncrementDto(
          currentUpperBound,
          totalNumberOfAppsAtTime,
          totalNumberOfAppsUsingCiCDAtTime));

      currentUpperBound += incrementSizeMillis;
    }

    return results;
  }

  public int getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(final Date upperBound) {
    // 84 days is meant to approximate 3 months, this is a falloff period. If an app is truly integrated and active
    // it should have evaluations done regularly, after 84 days if there have been no new evaluations it should no
    // longer count as integrated
    final Date lowerBound = new Date(upperBound.getTime() -  CICD_TRIGGERED_EVALUATION_CUT_OFF_MS);

    return policyEvaluationDAO.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(lowerBound, upperBound);
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
  }
}
