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
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.developer.integrationdashboard.CIEvaluationStatService.CICD_TRIGGERED_EVALUATION_CUT_OFF_MS;
import static org.assertj.core.api.Assertions.assertThat;
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
                20),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -12).getTime(),
                102,
                20),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -11).getTime(),
                102, 20),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -10).getTime(),
                102,
                20),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -9).getTime(),
                102,
                20),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -8).getTime(),
                102,
                20),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -7).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -6).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -5).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -4).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -3).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -2).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(
                getDateFromOffset(fixedClock, -1).getTime(),
                100,
                22),
            new ApiIntegrationsCiCdStatIncrementDto(nowMs, 123, 23)));
  }

  @Test
  public void testGetBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth_shouldCorrectlyApplyBounds() {
    // === Given ===
    final Date now = new Date();
    final long oneWeekMS = 604800000L;
    final Organization organization = tempEntity.newOrganization();

    // app 1- evaluated 3 weeks ago
    final Application application1 = tempEntity.newApplication(organization.getId());
    final Date evalTime1 = new Date(now.getTime() - 3 * oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application1.getId(),
        Stage.ID_BUILD,
        "scan-build-1",
        false,
        false,
        false,
        evalTime1,
        "hash-1",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 2 - evaluated 3 weeks ago
    final Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(
        application2.getId(),
        Stage.ID_BUILD,
        "scan-build-2",
        false,
        false,
        false,
        evalTime1,
        "hash-2",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 3 - evaluated 4 weeks ago
    final Application application3 = tempEntity.newApplication(organization.getId());
    final Date evalTime3 = new Date(now.getTime() - 4 * oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application3.getId(),
        Stage.ID_BUILD,
        "scan-build-3",
        false,
        false,
        false,
        evalTime3,
        "hash-3",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 4 - evaluated 4 weeks and over ~3 months ago
    final Application application4 = tempEntity.newApplication(organization.getId());
    final Date evalTime4 = new Date(now.getTime() - 4 * oneWeekMS - CICD_TRIGGERED_EVALUATION_CUT_OFF_MS - 2);
    tempEntity.newPolicyEvaluation(
        application4.getId(),
        Stage.ID_BUILD,
        "scan-build-4",
        false,
        false,
        false,
        evalTime4,
        "hash-4",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 5 - evaluated 2 weeks ago
    final Application application5 = tempEntity.newApplication(organization.getId());
    final Date evalTime5 = new Date(now.getTime() - 2 * oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application5.getId(),
        Stage.ID_BUILD,
        "scan-build-5",
        false,
        false,
        false,
        evalTime5,
        "hash-5",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 6 - evaluated 1 weeks ago
    final Application application6 = tempEntity.newApplication(organization.getId());
    final Date evalTime6 = new Date(now.getTime() - oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application6.getId(),
        Stage.ID_BUILD,
        "scan-build-6",
        false,
        false,
        false,
        evalTime6,
        "hash-6",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // === Then ===
    // should count all but app4 which is outside the 3 month cut off
    int result = ciEvaluationStatService.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(now);
    assertThat(result).isEqualTo(5);

    // should now also exclude app 6
    result = ciEvaluationStatService
        .getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(new Date(evalTime6.getTime() - 1));
    assertThat(result).isEqualTo(4);

    // should now also exclude app 6 and 5
    result = ciEvaluationStatService
        .getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(new Date(evalTime5.getTime() - 1));
    assertThat(result).isEqualTo(3);

    // should now also exclude app 6 and 5
    result = ciEvaluationStatService
        .getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(new Date(evalTime5.getTime() - 1));
    assertThat(result).isEqualTo(3);

    // should now exclude apps 6, 5, 1, and 2 as well as 4 (still outside the cutoff). 3 will still be included
    result = ciEvaluationStatService
        .getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(new Date(evalTime1.getTime() - 1));
    assertThat(result).isEqualTo(1);

    // should now exclude all apps
    result = ciEvaluationStatService
        .getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(new Date(evalTime3.getTime() - 1));
    assertThat(result).isEqualTo(0);

    // will now exclude all but app 4, no longer past the cut off window
    result = ciEvaluationStatService
        .getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(new Date(evalTime3.getTime() - 2));
    assertThat(result).isEqualTo(1);
  }

  private void createApplicationsWithCiCdEvaluationsAtMomentInTime(final int numberOfApplications, final Date date) {
    final List<Application> applications = createApplications(numberOfApplications);
    applications.forEach(app -> {
      tempEntity.newPolicyEvaluation(
          app.getId(),
          Stage.ID_BUILD,
          TemporaryEntity.uuid(),
          false,
          false,
          false,
          date,
          TemporaryEntity.uuid(),
          ScanTriggerType.CONTINUOUS_INTEGRATION);
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
