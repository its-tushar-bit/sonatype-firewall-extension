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

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsScmFeedbackStatIncrementDto;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ScmStatServiceTest
    extends AbstractComponentTest
{
  private static final long ONE_DAY_IN_MS = 86_400_000L;

  private static final long ONE_WEEK_IN_MS = 604_800_000L;

  @Mock
  private DateTimeService dateTimeService;

  @Inject
  private ScmStatService scmStatService;

  @Override
  public void configure(Binder binder) {
    binder.bind(DateTimeService.class).toInstance(dateTimeService);
    super.configure(binder);
  }

  @Test
  public void testGetScmFeedbackUsageStatsOverTime_shouldReturnCorrectValuesGivenIncrementSizeAndNumber() {
    // === Given ===
    // Clock frozen at 2023-11-03T15:51:56.287Z
    final long nowMs = 1699027090422L;
    final Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault());

    final Date fiveWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-35));

    final Date fourWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-28));
    tempEntity.newApplicationCountHistoryEntry(fourWeeksAgo, 42, 20, 0, 0, 0);

    final Date threeWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-21));
    tempEntity.newApplicationCountHistoryEntry(threeWeeksAgo, 62, 63, 0, 0, 0);

    final Date twoWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-14));
    tempEntity.newApplicationCountHistoryEntry(twoWeeksAgo, 111, 65, 0, 0, 0);

    final Date oneWeekAgo = getDateFromOffset(fixedClock, Duration.ofDays(-7));
    tempEntity.newApplicationCountHistoryEntry(oneWeekAgo, 134, 61, 0, 0, 0);

    final Date current = new Date(nowMs);
    tempEntity.newApplicationCountHistoryEntry(current, 429, 84, 0, 0, 0);

    // === When ===
    when(dateTimeService.getCurrentTimeMs()).thenReturn(nowMs);

    // === Then ===
    final List<ApiIntegrationsScmFeedbackStatIncrementDto> fiveWeeksByWeeklyIncrements =
        scmStatService.getScmFeedbackUsageStatsOverTime(ONE_WEEK_IN_MS, 6);

    assertThat(fiveWeeksByWeeklyIncrements).isEqualTo(
        Lists.newArrayList(
            new ApiIntegrationsScmFeedbackStatIncrementDto(fiveWeeksAgo.getTime(), 0, 0),
            new ApiIntegrationsScmFeedbackStatIncrementDto(fourWeeksAgo.getTime(), 42, 20),
            new ApiIntegrationsScmFeedbackStatIncrementDto(threeWeeksAgo.getTime(), 62, 63),
            new ApiIntegrationsScmFeedbackStatIncrementDto(twoWeeksAgo.getTime(), 111, 65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(oneWeekAgo.getTime(), 134, 61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(nowMs, 429, 84)));

    // === Then -- With Different Increment and Size ===
    final List<ApiIntegrationsScmFeedbackStatIncrementDto> twoWeeksByDailyIncrements =
        scmStatService.getScmFeedbackUsageStatsOverTime(ONE_DAY_IN_MS, 14);

    assertThat(twoWeeksByDailyIncrements).isEqualTo(
        Lists.newArrayList(
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -13).getTime(),
                111,
                65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -12).getTime(),
                111,
                65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -11).getTime(),
                111,
                65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -10).getTime(),
                111,
                65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -9).getTime(),
                111,
                65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -8).getTime(),
                111,
                65),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -7).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -6).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -5).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -4).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -3).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -2).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(
                getDateFromOffset(fixedClock, -1).getTime(),
                134,
                61),
            new ApiIntegrationsScmFeedbackStatIncrementDto(nowMs,
                429,
                84)));
  }

  private Date getDateFromOffset(final Clock baseClock, final int numberOfDays) {
    return getDateFromOffset(baseClock, Duration.ofDays(numberOfDays));
  }

  private Date getDateFromOffset(final Clock baseClock, final Duration offset) {
    return Date.from(Clock.offset(baseClock, offset).instant());
  }
}
