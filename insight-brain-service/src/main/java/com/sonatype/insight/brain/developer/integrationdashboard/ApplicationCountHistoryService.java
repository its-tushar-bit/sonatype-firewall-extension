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

import com.sonatype.insight.brain.dataaccess.ApplicationCountHistoryDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.organization.ApplicationSourceControlService;

@Named
public class ApplicationCountHistoryService
{
  private final ApplicationCountHistoryDAO applicationCountHistoryDAO;

  private final ApplicationSourceControlService applicationSourceControlService;

  private final ApplicationDAO applicationDAO;

  private final DateTimeService dateTimeService;

  @Inject
  public ApplicationCountHistoryService(
      final ApplicationCountHistoryDAO applicationCountHistoryDAO,
      final ApplicationSourceControlService applicationSourceControlService,
      final ApplicationDAO applicationDao,
      final DateTimeService dateTimeService
  )
  {
    this.applicationCountHistoryDAO = applicationCountHistoryDAO;
    this.applicationSourceControlService = applicationSourceControlService;
    this.applicationDAO = applicationDao;
    this.dateTimeService = dateTimeService;
  }

  public void recordApplicationCount() {
    final List<Application> allApplications = applicationDAO.getAll();
    final int currentAppCount = allApplications.size();

    final int currentNumberOfAppsWithScmFeedbackEnabled = (int) allApplications.stream()
        .filter(application -> applicationSourceControlService.isAutomatedSourceControlFeedbackEnabledForApp(
            application.getId()))
        .count();

    final ApplicationCountHistory countHistory = new ApplicationCountHistory(
        dateTimeService.getCurrentDate(),
        currentAppCount,
        currentNumberOfAppsWithScmFeedbackEnabled
    );

    applicationCountHistoryDAO.insert(countHistory);
  }

  public ApplicationCountHistory getApplicationHistoryCount(Date date) {
    final ApplicationCountHistory requestedHistoryCount = applicationCountHistoryDAO.getApplicationHistoryCount(date);

    if (requestedHistoryCount != null) {
      return requestedHistoryCount;
    }

    return applicationCountHistoryDAO.getInitialApplicationHistoryCount();
  }
}
