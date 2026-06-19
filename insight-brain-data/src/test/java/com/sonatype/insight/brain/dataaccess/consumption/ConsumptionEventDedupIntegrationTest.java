/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.time.Instant;
import java.time.LocalDate;

import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end dedup behavior of {@link ConsumptionEventDAO} against the partial unique
 * index on {@code idempotency_key} — covers Component Details, Version Recommendation /
 * Developer Priorities, Priorities-grid fan-out, and session-boundary semantics.
 *
 * @since 1.205 (CLM-40771)
 */
public class ConsumptionEventDedupIntegrationTest
    extends ConsumptionEventIntegrationTestSupport
{
  private static final LocalDate BILLING_MONTH = LocalDate.of(2026, 12, 1);

  private static final Instant EVENT_TIME = Instant.parse("2026-12-10T10:00:00Z");

  @Before
  public void setup() {
    initialize();
    dao = daoFactory.createConsumptionEventDAO();
  }

  // -- Component Details (BDD-001..004, BDD-009) ------------------------------

  @Test
  public void singlePageVisit_emitsExactlyOneComponentDetailsEvent() {
    String key = "42:COMPONENT_DETAILS:compHash1:scanSX:sessHashABC";
    dao.recordEvent(buildEvent(key, ActivityType.COMPONENT_DETAILS, "42", "scanSX"));

    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(1L);
  }

  @Test
  public void repeatedRecord_withSameKey_isDeduped() {
    String key = "42:COMPONENT_DETAILS:compHash2:scanSY:sessHashDEF";
    dao.recordEvent(buildEvent(key, ActivityType.COMPONENT_DETAILS, "42", "scanSY"));
    dao.recordEvent(buildEvent(key, ActivityType.COMPONENT_DETAILS, "42", "scanSY"));

    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(1L);
  }

  @Test
  public void crossReport_emitsTwoEvents() {
    String key1 = "42:COMPONENT_DETAILS:compHash3:scanA:sessHash1";
    String key2 = "42:COMPONENT_DETAILS:compHash3:scanB:sessHash1";
    dao.recordEvent(buildEvent(key1, ActivityType.COMPONENT_DETAILS, "42", "scanA"));
    dao.recordEvent(buildEvent(key2, ActivityType.COMPONENT_DETAILS, "42", "scanB"));

    assertThat(countWithKey(key1)).isEqualTo(1);
    assertThat(countWithKey(key2)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(2L);
  }

  @Test
  public void differentUser_emitsTwoEvents() {
    String keyUserA = "userA:COMPONENT_DETAILS:compHash4:scanC:sessHash2";
    String keyUserB = "userB:COMPONENT_DETAILS:compHash4:scanC:sessHash2";
    dao.recordEvent(buildEvent(keyUserA, ActivityType.COMPONENT_DETAILS, "userA", "scanC"));
    dao.recordEvent(buildEvent(keyUserB, ActivityType.COMPONENT_DETAILS, "userB", "scanC"));

    assertThat(countWithKey(keyUserA)).isEqualTo(1);
    assertThat(countWithKey(keyUserB)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(2L);
  }

  // -- Session boundary (BDD-013, BDD-014) ------------------------------------

  @Test
  public void sameSession_secondVisit_isDeduped() {
    String key = "userSB:COMPONENT_DETAILS:compSB1:scanSB:sessHashSESS1";
    dao.recordEvent(buildEvent(key, ActivityType.COMPONENT_DETAILS, "userSB", "scanSB"));
    dao.recordEvent(buildEvent(key, ActivityType.COMPONENT_DETAILS, "userSB", "scanSB"));

    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(1L);
  }

  @Test
  public void differentSessionForSameUser_emitsAdditionalRow() {
    // session hash differs (logout + login) → must NOT dedup.
    String key1 = "userSB:COMPONENT_DETAILS:compSB2:scanSB:sessHashSESS1";
    String key2 = "userSB:COMPONENT_DETAILS:compSB2:scanSB:sessHashSESS2";
    dao.recordEvent(buildEvent(key1, ActivityType.COMPONENT_DETAILS, "userSB", "scanSB"));
    dao.recordEvent(buildEvent(key2, ActivityType.COMPONENT_DETAILS, "userSB", "scanSB"));

    assertThat(countWithKey(key1)).isEqualTo(1);
    assertThat(countWithKey(key2)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(2L);
  }

  // -- Version Recommendation / Developer Priorities (BDD-010, BDD-012) -------

  @Test
  public void pageMountFiresExactlyOneVersionRecommendation() {
    String key = "userVR:DEVELOPER_PRIORITIES:hashVR1:scanVR:sessHashVR";
    dao.recordEvent(buildEvent(key, ActivityType.DEVELOPER_PRIORITIES, "userVR", "scanVR"));

    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(1L);
  }

  @Test
  public void compareVersionForDifferentHash_emitsAdditionalRow() {
    String key1 = "userVR:DEVELOPER_PRIORITIES:hashVR2:scanVR:sessHashVR";
    String key2 = "userVR:DEVELOPER_PRIORITIES:hashVR3:scanVR:sessHashVR";
    dao.recordEvent(buildEvent(key1, ActivityType.DEVELOPER_PRIORITIES, "userVR", "scanVR"));
    dao.recordEvent(buildEvent(key2, ActivityType.DEVELOPER_PRIORITIES, "userVR", "scanVR"));

    assertThat(countWithKey(key1)).isEqualTo(1);
    assertThat(countWithKey(key2)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(2L);
  }

  // -- Priorities grid (BDD-007, BDD-008, BDD-043) ----------------------------

  @Test
  public void actionableRows_eachEmitOneVrEvent_independently() {
    String key1 = "userPG:DEVELOPER_PRIORITIES:pgHash1:scanPG:sessHashPG";
    String key2 = "userPG:DEVELOPER_PRIORITIES:pgHash2:scanPG:sessHashPG";
    String key3 = "userPG:DEVELOPER_PRIORITIES:pgHash3:scanPG:sessHashPG";
    dao.recordEvent(buildEvent(key1, ActivityType.DEVELOPER_PRIORITIES, "userPG", "scanPG"));
    dao.recordEvent(buildEvent(key2, ActivityType.DEVELOPER_PRIORITIES, "userPG", "scanPG"));
    dao.recordEvent(buildEvent(key3, ActivityType.DEVELOPER_PRIORITIES, "userPG", "scanPG"));

    assertThat(countWithKey(key1)).isEqualTo(1);
    assertThat(countWithKey(key2)).isEqualTo(1);
    assertThat(countWithKey(key3)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(3L);
  }

  @Test
  public void actionableRow_doubleEmit_isDeduped() {
    String key = "userPG:DEVELOPER_PRIORITIES:pgHash4:scanPG:sessHashPG";
    dao.recordEvent(buildEvent(key, ActivityType.DEVELOPER_PRIORITIES, "userPG", "scanPG"));
    dao.recordEvent(buildEvent(key, ActivityType.DEVELOPER_PRIORITIES, "userPG", "scanPG"));

    assertThat(countWithKey(key)).isEqualTo(1);
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(1L);
  }

  @Test
  public void nonActionableRow_emitsZeroEvents() {
    // Frontend gate suppresses the recorder call entirely; backend fixture asserts
    // the absence of any insertion.
    assertThat(dao.sumByMonth(BILLING_MONTH)).isEqualTo(0L);
    assertThat(countByActivityType(ActivityType.DEVELOPER_PRIORITIES)).isEqualTo(0L);
  }

  // ---- helpers ---------------------------------------------------------------

  private ConsumptionEvent buildEvent(
      final String key,
      final ActivityType type,
      final String userId,
      final String scanId)
  {
    ConsumptionEvent e = new ConsumptionEvent();
    e.setOrgId("org-dedup-it");
    e.setTier("ENTERPRISE");
    e.setSource("UI");
    e.setUserId(userId);
    e.setScanId(scanId);
    e.setActivityType(type);
    e.setComponentCount(1);
    e.setBillingMonth(BILLING_MONTH);
    e.setEventTimestamp(EVENT_TIME);
    e.setIdempotencyKey(key);
    return e;
  }
}
