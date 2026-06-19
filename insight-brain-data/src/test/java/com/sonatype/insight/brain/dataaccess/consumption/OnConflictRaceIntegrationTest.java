/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent inserts of the same idempotency key must produce exactly one row, with
 * no exception escaping any thread — exercises the ON CONFLICT (Postgres) /
 * savepoint-absorption (H2) dedup path. BDD-015, BDD-046.
 *
 * @since 1.205 (CLM-40771)
 */
public class OnConflictRaceIntegrationTest
    extends ConsumptionEventIntegrationTestSupport
{
  private static final LocalDate BILLING_MONTH = LocalDate.of(2026, 12, 1);

  private static final int THREAD_COUNT = 8;

  @Before
  public void setup() {
    initialize();
    dao = daoFactory.createConsumptionEventDAO();
  }

  @Test
  public void concurrentRecord_sameKey_producesExactlyOneRow_noException() throws Exception {
    String key = "raceUser:COMPONENT_DETAILS:raceComp:raceScan:raceSession";
    CountDownLatch startGate = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<?>> futures = new ArrayList<>(THREAD_COUNT);

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        try {
          startGate.await();
          dao.recordEvent(buildEvent(key));
          return null;
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }));
    }
    startGate.countDown();

    for (Future<?> f : futures) {
      f.get(10, TimeUnit.SECONDS);
    }
    executor.shutdown();

    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(1L);
  }

  // ---- helpers ---------------------------------------------------------------

  private ConsumptionEvent buildEvent(final String key) {
    ConsumptionEvent e = new ConsumptionEvent();
    e.setOrgId("org-race-it");
    e.setTier("ENTERPRISE");
    e.setSource("UI");
    e.setUserId("raceUser");
    e.setScanId("raceScan");
    e.setActivityType(ActivityType.COMPONENT_DETAILS);
    e.setComponentCount(1);
    e.setBillingMonth(BILLING_MONTH);
    e.setEventTimestamp(Instant.parse("2026-12-10T12:00:00Z"));
    e.setIdempotencyKey(key);
    return e;
  }
}
