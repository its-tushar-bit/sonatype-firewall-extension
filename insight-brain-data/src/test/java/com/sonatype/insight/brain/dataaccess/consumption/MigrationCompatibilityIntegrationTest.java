/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies legacy NULL-key rows coexist with new keyed rows — the partial unique index
 * on idempotency_key MUST exclude NULLs so multiple NULL-key inserts are accepted.
 * BDD-018.
 *
 * @since 1.205 (CLM-40771)
 */
public class MigrationCompatibilityIntegrationTest
    extends ConsumptionEventIntegrationTestSupport
{
  private static final LocalDate BILLING_MONTH = LocalDate.of(2026, 12, 1);

  @Before
  public void setup() {
    initialize();
    dao = daoFactory.createConsumptionEventDAO();
  }

  @Test
  public void legacyNullKeyRows_coexist_withNewKeyedRows() {
    dao.recordEvent(buildEventNoKey(50));
    dao.recordEvent(buildEventNoKey(30));
    String key = "userMig:COMPONENT_DETAILS:migComp:migScan:migSession";
    dao.recordEvent(buildEventWithKey(key, 10));

    assertThat(countNullKeyRows()).isEqualTo(2);
    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(90L);
  }

  @Test
  public void multipleNullKeyRows_doNotViolateUniqueConstraint() {
    List<ConsumptionEvent> legacyBatch = Arrays.asList(
        buildEventNoKey(1),
        buildEventNoKey(2),
        buildEventNoKey(3));

    for (ConsumptionEvent e : legacyBatch) {
      dao.recordEvent(e);
    }

    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(6L);
    assertThat(countNullKeyRows()).isEqualTo(3);
  }

  // ---- helpers ---------------------------------------------------------------

  // idempotencyKey left null — simulates a pre-CLM-40771 legacy row
  private ConsumptionEvent buildEventNoKey(final int count) {
    ConsumptionEvent e = new ConsumptionEvent();
    e.setOrgId("org-mig-it");
    e.setTier("ENTERPRISE");
    e.setSource("API");
    e.setUserId("userMig");
    e.setScanId("migScan");
    e.setActivityType(ActivityType.COMPONENT_DETAILS);
    e.setComponentCount(count);
    e.setBillingMonth(BILLING_MONTH);
    e.setEventTimestamp(Instant.parse("2026-12-10T09:00:00Z"));
    return e;
  }

  private ConsumptionEvent buildEventWithKey(final String key, final int count) {
    ConsumptionEvent e = buildEventNoKey(count);
    e.setIdempotencyKey(key);
    return e;
  }
}
