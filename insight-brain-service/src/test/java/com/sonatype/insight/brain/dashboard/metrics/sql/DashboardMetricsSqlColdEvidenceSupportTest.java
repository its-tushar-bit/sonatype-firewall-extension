/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowable;

public class DashboardMetricsSqlColdEvidenceSupportTest
{
  @Test
  public void nearestRankUsesSortedOneBasedCeilingRank() {
    List<Long> unsortedNanos = List.of(20L, 1L, 19L, 5L, 18L, 6L, 17L, 7L, 16L, 8L,
        15L, 9L, 14L, 10L, 13L, 11L, 12L, 2L, 4L, 3L);

    assertThat(DashboardMetricsSqlColdEvidenceSupport.nearestRank(unsortedNanos, 50)).isEqualTo(10L);
    assertThat(DashboardMetricsSqlColdEvidenceSupport.nearestRank(unsortedNanos, 95)).isEqualTo(19L);
  }

  @Test
  public void nearestRankRejectsInvalidInput() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DashboardMetricsSqlColdEvidenceSupport.nearestRank(List.of(), 95));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DashboardMetricsSqlColdEvidenceSupport.nearestRank(List.of(1L), 0));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DashboardMetricsSqlColdEvidenceSupport.nearestRank(List.of(1L), 101));
  }

  @Test
  public void thresholdVerdictUsesInclusiveMillisecondBudget() {
    assertThat(DashboardMetricsSqlColdEvidenceSupport.thresholdVerdict(50_000_000L, 50L))
        .isEqualTo("PASS");
    assertThat(DashboardMetricsSqlColdEvidenceSupport.thresholdVerdict(50_000_001L, 50L))
        .isEqualTo("FAIL");
  }

  @Test
  public void advisoryLockIsAcquiredBeforeWorkAndReleasedAfterFailure() {
    List<String> events = new ArrayList<>();
    DashboardMetricsSqlColdEvidenceSupport.LockLifecycle lock =
        new DashboardMetricsSqlColdEvidenceSupport.LockLifecycle()
        {
          @Override
          public void acquire() {
            events.add("acquire");
          }

          @Override
          public void release() {
            events.add("release");
          }
        };

    Throwable failure = catchThrowable(() -> DashboardMetricsSqlColdEvidenceSupport.withAdvisoryLock(lock, () -> {
      events.add("work");
      throw new IllegalStateException("evidence failed");
    }));

    assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("evidence failed");
    assertThat(events).containsExactly("acquire", "work", "release");
  }

  @Test
  public void redShapeStopsBeforeLaterShapeExecution() throws Exception {
    AtomicInteger laterExecutions = new AtomicInteger();

    List<String> completed = DashboardMetricsSqlColdEvidenceSupport.runSequentiallyUntil(
        List.of(
            () -> "GREEN",
            () -> "OVER_2X",
            () -> {
              laterExecutions.incrementAndGet();
              return "GREEN";
            }),
        "OVER_2X"::equals);

    assertThat(completed).containsExactly("GREEN", "OVER_2X");
    assertThat(laterExecutions).hasValue(0);
  }
}
