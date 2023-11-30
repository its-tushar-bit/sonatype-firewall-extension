/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationCountHistoryDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.organization.ApplicationSourceControlService;

import org.apache.commons.collections.CollectionUtils;

@Named
public class ApplicationCountHistoryService
{
  private final ApplicationCountHistoryDAO applicationCountHistoryDAO;

  private final ApplicationSourceControlService applicationSourceControlService;

  private final ApplicationDAO applicationDAO;

  private final DateTimeService dateTimeService;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public ApplicationCountHistoryService(
      final ApplicationCountHistoryDAO applicationCountHistoryDAO,
      final ApplicationSourceControlService applicationSourceControlService,
      final ApplicationDAO applicationDao,
      final DateTimeService dateTimeService,
      final PolicyViolationDAO policyViolationDAO)
  {
    this.applicationCountHistoryDAO = applicationCountHistoryDAO;
    this.applicationSourceControlService = applicationSourceControlService;
    this.applicationDAO = applicationDao;
    this.dateTimeService = dateTimeService;
    this.policyViolationDAO = policyViolationDAO;
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

    final long meanTimeToRemediateMillis = getMeanTimeToRemediate(policyViolationDAO.getWaivedFixed());

    final ApplicationCountHistory countHistory = new ApplicationCountHistory(
        dateTimeService.getCurrentDate(),
        currentAppCount,
        currentNumberOfAppsWithScmFeedbackEnabled,
        policyActionFailuresByAppCount,
        waiversCount,
        meanTimeToRemediateMillis);

    applicationCountHistoryDAO.insert(countHistory);
  }

  public ApplicationCountHistory getApplicationHistoryCount(Date date) {
    final ApplicationCountHistory requestedCountHistory = applicationCountHistoryDAO.getApplicationCountHistory(date);

    if (requestedCountHistory != null) {
      return requestedCountHistory;
    }

    return applicationCountHistoryDAO.getInitialApplicationCountHistory();
  }

  private long getMeanTimeToRemediate(final List<PolicyViolation> byFixOrWaiveTimes) {
    if (CollectionUtils.isEmpty(byFixOrWaiveTimes)) {
      return 0L;
    }

    final double average = byFixOrWaiveTimes.stream()
        .map(policyViolation -> policyViolation.getFixOrWaiveTime().getTime() - policyViolation.getOpenTime().getTime())
        .mapToLong(Long::longValue)
        .average()
        .orElse(0.0);

    return Math.round(average);
  }
}
