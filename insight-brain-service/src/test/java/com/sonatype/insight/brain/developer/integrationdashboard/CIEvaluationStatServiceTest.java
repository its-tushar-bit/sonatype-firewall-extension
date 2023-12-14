/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.developer.integrationdashboard.api.CIEvaluationStatDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Lists;
import com.google.inject.Binder;

import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.core.util.StringUtils.uuid;
import static org.mockito.Mockito.when;

public class CIEvaluationStatServiceTest
    extends AbstractComponentTest
{
  private static final long ONE_DAY_IN_MS = 86_400_000L;

  private static final long ONE_WEEK_IN_MS = 604_800_000L;

  @Mock
  private DateTimeService dateTimeService;

  @Inject
  private CIEvaluationStatService ciEvaluationStatService;

  @Override
  public void configure(Binder binder) {
    binder.bind(DateTimeService.class).toInstance(dateTimeService);
    super.configure(binder);
  }

  @Test
  public void testGetDataForAppsWithoutCITriggeredEvaluations() {
    // Set up an org with some applications - 2 with evaluations, 1 of which has CI and not CI, configurable total
    int numTotalApps = 9;
    setUpApplications(numTotalApps, true);

    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    CIEvaluationStatDTO ciEvaluationStatDTO =
        ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);

    int expectedNumAppsWithoutCI = numTotalApps - 1;
    assertThat(ciEvaluationStatDTO.numAppsWithoutCITriggeredEvals).isEqualTo(expectedNumAppsWithoutCI);
    assertThat(ciEvaluationStatDTO.numTotalApps).isEqualTo(numTotalApps);
  }

  @Test
  public void testGetDataForAppsWithoutCITriggeredEvaluations_WhenNoAppsHaveEvaluations() {
    // Set up an org with some applications, but no evaluations
    int numTotalApps = 3;
    setUpApplications(numTotalApps, false);

    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    CIEvaluationStatDTO ciEvaluationStatDTO =
        ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);

    assertThat(ciEvaluationStatDTO.numAppsWithoutCITriggeredEvals).isEqualTo(numTotalApps);
    assertThat(ciEvaluationStatDTO.numTotalApps).isEqualTo(numTotalApps);
  }

  @Test
  public void testGetPercentageOfAppsWithCITriggeredEvaluations_WhenNoAppsExist() {
    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    CIEvaluationStatDTO ciEvaluationStatDTO =
        ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);

    assertThat(ciEvaluationStatDTO.numAppsWithoutCITriggeredEvals).isZero();
    assertThat(ciEvaluationStatDTO.numTotalApps).isZero();
  }

  @Test
  public void testGetCiCdUsageStatsOverTime_ShouldReturnCorrectValuesGivenAnIncrementSizeAndNumber() {
    // === Given ===
    // Clock frozen at 2023-11-03T15:51:56.287Z
    final long nowMs = 1699027090422L;
    final Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault());

    final Date fiveWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-35));

    final Date fourWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-28));
    createApplicationsWithCiCdEvaluationsAtMomentInTime(3, fourWeeksAgo);
    tempEntity.newApplicationCountHistoryEntry(fourWeeksAgo, 64);

    final Date threeWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-21));
    createApplicationsWithCiCdEvaluationsAtMomentInTime(5, threeWeeksAgo);
    tempEntity.newApplicationCountHistoryEntry(threeWeeksAgo, 72);

    final Date twoWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-14));
    createApplicationsWithCiCdEvaluationsAtMomentInTime(12, twoWeeksAgo);
    tempEntity.newApplicationCountHistoryEntry(twoWeeksAgo, 102);

    final Date oneWeekAgo = getDateFromOffset(fixedClock, Duration.ofDays(-7));
    createApplicationsWithCiCdEvaluationsAtMomentInTime(2, oneWeekAgo);
    tempEntity.newApplicationCountHistoryEntry(oneWeekAgo, 100);

    final Date current = new Date(nowMs);
    createApplicationsWithCiCdEvaluationsAtMomentInTime(1, current);
    tempEntity.newApplicationCountHistoryEntry(current, 123);

    // === Given ===
    when(dateTimeService.getCurrentTimeMs()).thenReturn(nowMs);

    // === Then ===
    final List<ApiIntegrationsCiCdStatIncrementDto> fiveWeeksByWeeklyIncrements =
        ciEvaluationStatService.getCiCdUsageStatsOverTime(ONE_WEEK_IN_MS, 6);

    assertThat(fiveWeeksByWeeklyIncrements).isEqualTo(
        Lists.newArrayList(
            new ApiIntegrationsCiCdStatIncrementDto(fiveWeeksAgo.getTime(), 0, 0),
            new ApiIntegrationsCiCdStatIncrementDto(fourWeeksAgo.getTime(), 64, 3),
            new ApiIntegrationsCiCdStatIncrementDto(threeWeeksAgo.getTime(), 72, 8),
            new ApiIntegrationsCiCdStatIncrementDto(twoWeeksAgo.getTime(), 102, 20),
            new ApiIntegrationsCiCdStatIncrementDto(oneWeekAgo.getTime(), 100, 22),
            new ApiIntegrationsCiCdStatIncrementDto(nowMs, 123, 23)));

    final List<ApiIntegrationsCiCdStatIncrementDto> twoWeeksByDailyIncrements =
        ciEvaluationStatService.getCiCdUsageStatsOverTime(ONE_DAY_IN_MS, 14);

    assertThat(twoWeeksByDailyIncrements).isEqualTo(
        Lists.newArrayList(
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -13).getTime(),
                102,
                20
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -12).getTime(),
                102,
                20
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -11).getTime(),
                102, 20
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -10).getTime(),
                102,
                20
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -9).getTime(),
                102,
                20
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -8).getTime(),
                102,
                20
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -7).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -6).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -5).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -4).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -3).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -2).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -1).getTime(),
                100,
                22
            ),
            new ApiIntegrationsCiCdStatIncrementDto(nowMs, 123, 23)));
  }

  private void setUpApplications(final int maxApplications, final boolean includeEvaluations) {
    Organization organization = tempEntity.newOrganization();

    if (includeEvaluations) {
      Application application = tempEntity.newApplication(organization.getId());
      Application application2 = tempEntity.newApplication(organization.getId());

      // Add 1 policy evaluation with a scan trigger type not CI, 1 with a scan trigger type of CI, and a 2nd for the
      // same app (i.e. app2 should not count as an app without CI just because it has an eval of type not CI)
      Calendar now = Calendar.getInstance();
      tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "testScanId1",
          false, false, false, now.getTime(), "testCommitHash1", ScanTriggerType.IDE);
      now.add(Calendar.MINUTE, 10);

      tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_BUILD, "testScanId2",
          false, false, false, now.getTime(), "testCommitHash2", ScanTriggerType.CONTINUOUS_INTEGRATION);
      now.add(Calendar.MINUTE, 10);

      tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_BUILD, "testScanId3",
          false, false, false, now.getTime(), "testCommitHash3", ScanTriggerType.IDE);
    }

    int effectiveMax = includeEvaluations ? maxApplications - 2 : maxApplications;
    tempEntity.createApplications(effectiveMax, organization);
  }

  private void createApplicationsWithCiCdEvaluationsAtMomentInTime(final int numberOfApplications, final Date date) {
    final List<Application> applications = createApplications(numberOfApplications);
    applications.forEach(app -> {
      tempEntity.newPolicyEvaluation(
          app.getId(),
          Stage.ID_BUILD,
          uuid(),
          false,
          false,
          false,
          date,
          uuid(),
          ScanTriggerType.CONTINUOUS_INTEGRATION
      );
    });
  }

  private List<Application> createApplications(final int numberOfApplications) {
    return tempEntity.createApplications(numberOfApplications, null);
  }

  private Date getDateFromOffset(final Clock baseClock, final int numberOfDays) {
    return getDateFromOffset(baseClock, Duration.ofDays(numberOfDays));
  }

  private Date getDateFromOffset(final Clock baseClock, final Duration offset) {
    return Date.from(Clock.offset(baseClock, offset).instant());
  }
}
