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

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationCountHistoryDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiUsageIncrementDto;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationSourceControlService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

@Named
public class ApplicationCountHistoryService
{
  private final ApplicationCountHistoryDAO applicationCountHistoryDAO;

  private final ApplicationSourceControlService applicationSourceControlService;

  private final ApplicationDAO applicationDAO;

  private final DateTimeService dateTimeService;

  private final PolicyViolationDAO policyViolationDAO;

  private final CIEvaluationStatService ciEvaluationStatService;

  @Inject
  public ApplicationCountHistoryService(
      final ApplicationCountHistoryDAO applicationCountHistoryDAO,
      final ApplicationSourceControlService applicationSourceControlService,
      final ApplicationDAO applicationDAO,
      final DateTimeService dateTimeService,
      final PolicyViolationDAO policyViolationDAO,
      final CIEvaluationStatService ciEvaluationStatService
  )
  {
    this.applicationCountHistoryDAO = applicationCountHistoryDAO;
    this.applicationSourceControlService = applicationSourceControlService;
    this.applicationDAO = applicationDAO;
    this.dateTimeService = dateTimeService;
    this.policyViolationDAO = policyViolationDAO;
    this.ciEvaluationStatService = ciEvaluationStatService;
  }

  public void recordApplicationCount() {
    final List<Application> allApplications = applicationDAO.getAll();
    final int currentAppCount = allApplications.size();

    final int currentNumberOfAppsWithScmFeedbackEnabled = (int) allApplications.stream()
        .filter(application -> applicationSourceControlService.isAutomatedSourceControlFeedbackEnabledForApp(
            application.getId()))
        .count();

    final int policyActionFailuresByAppCount =
        policyViolationDAO.getCountApplicationsWithPolicyActionFailures(Stage.ID_BUILD);

    final int waiversCount = policyViolationDAO.getCountActiveWaivers();

    // we treat 3 months as 12 weeks/84 days. This maintains consistency with other metrics on the developer dashboard
    final long meanTimeToRemediateMillis = policyViolationDAO.getMeanTimeToRemediate(84);

    final ApplicationCountHistory countHistory = new ApplicationCountHistory(
        dateTimeService.getCurrentDate(),
        currentAppCount,
        currentNumberOfAppsWithScmFeedbackEnabled,
        policyActionFailuresByAppCount,
        waiversCount,
        meanTimeToRemediateMillis);

    applicationCountHistoryDAO.insert(countHistory);
  }

  public List<ApiUsageIncrementDto> getUsageOverTime(
      final long incrementSizeMillis,
      final int numberOfIncrements
  )
  {
    final List<ApiUsageIncrementDto> usageOverTime = new ArrayList<>();

    final long now = dateTimeService.getCurrentTimeMs();

    long currentUpperBound = now - (incrementSizeMillis * numberOfIncrements) + incrementSizeMillis;

    for (int i = 0; i < numberOfIncrements; i++) {
      final Date timeOfIncrement = new Date(currentUpperBound);

      final int totalNumberOfAppsUsingCiCDAtTime =
          ciEvaluationStatService.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(timeOfIncrement);

      usageOverTime.add(
          ApiUsageIncrementDto.fromApplicationHistoryCount(
              currentUpperBound,
              totalNumberOfAppsUsingCiCDAtTime,
              getApplicationHistoryCount(timeOfIncrement)
          ));

      currentUpperBound += incrementSizeMillis;
    }

    return usageOverTime;
  }

  public ApplicationCountHistory getApplicationHistoryCount(Date date) {
    final ApplicationCountHistory requestedCountHistory = applicationCountHistoryDAO.getApplicationCountHistory(date);

    if (requestedCountHistory != null) {
      return requestedCountHistory;
    }

    return applicationCountHistoryDAO.getInitialApplicationCountHistory();
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
    // The @Authorize annotation provides the implementation for this function
  }
}
