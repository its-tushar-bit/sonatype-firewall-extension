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
import com.sonatype.insight.brain.developer.integrationdashboard.api.CIEvaluationStatDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationCountHistoryDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.162
 */
@Named
class CIEvaluationStatService
{
  private static final Logger log = LoggerFactory.getLogger(CIEvaluationStatService.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  private final ApplicationCountHistoryDAO applicationCountHistoryDAO;

  private final DateTimeService dateTimeService;

  @Inject
  public CIEvaluationStatService(
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationDAO applicationDAO,
      final ApplicationCountHistoryDAO applicationCountHistoryDAO,
      final DateTimeService dateTimeService
  )
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationDAO = applicationDAO;
    this.applicationCountHistoryDAO = applicationCountHistoryDAO;
    this.dateTimeService = dateTimeService;
  }

  CIEvaluationStatDTO getDataForAppsWithoutCITriggeredEvaluations(final long sinceUtcTimestamp) {
    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    Date sinceUtcDate = new Date(sinceUtcTimestamp);
    log.debug("Getting data for non CI/CD plugin-integrated applications from evaluations on or after {}",
        sinceUtcDate);

    int numAppsWithCI = policyEvaluationDAO.getCountOfApplicationsWithCITriggeredEvaluations(sinceUtcDate);
    int numTotalApps = (int) applicationDAO.getCount();
    int numAppsWithoutCI = numTotalApps - numAppsWithCI;

    return new CIEvaluationStatDTO(numAppsWithoutCI, numTotalApps);
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
          policyEvaluationDAO.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(timeOfIncrement);

      results.add(new ApiIntegrationsCiCdStatIncrementDto(
          currentUpperBound,
          totalNumberOfAppsAtTime,
          totalNumberOfAppsUsingCiCDAtTime));

      currentUpperBound += incrementSizeMillis;
    }

    return results;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
  }
}
