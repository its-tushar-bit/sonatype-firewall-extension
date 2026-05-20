/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConsumptionEventsTest
{
  @After
  public void tearDown() {
    ConsumptionContext.clear();
  }

  @Test
  public void builderFromContext_copiesOrgIdSourceTierFromContext() {
    ConsumptionContext.set("tenant-42", "APP_BASED", "API");

    ConsumptionEvent event = ConsumptionEvents.builderFromContext(ConsumptionContext.get())
        .appId("app-1")
        .activityType(ActivityType.APP_SCAN)
        .componentCount(1)
        .build();

    assertThat(event.getOrgId()).isEqualTo("tenant-42");
    assertThat(event.getSource()).isEqualTo("API");
    assertThat(event.getTier()).isEqualTo("APP_BASED");
  }

  @Test
  public void builderFromContext_setsEventTimestampToCurrentInstant() {
    ConsumptionContext.set("org", "APP_BASED", "UI");
    Instant before = Instant.now();

    ConsumptionEvent event = ConsumptionEvents.builderFromContext(ConsumptionContext.get())
        .appId("app-1")
        .activityType(ActivityType.APP_SCAN)
        .componentCount(1)
        .build();

    Instant after = Instant.now();
    assertThat(event.getEventTimestamp())
        .isAfterOrEqualTo(before)
        .isBeforeOrEqualTo(after);
  }

  @Test
  public void builderFromContext_setsBillingMonthToCurrentBillingWindowStart() {
    ConsumptionContext.set("org", "APP_BASED", "UI");
    LocalDate expected = BillingWindowUtil.calculateWindowStart(LocalDate.now(ZoneOffset.UTC), 1);

    ConsumptionEvent event = ConsumptionEvents.builderFromContext(ConsumptionContext.get())
        .appId("app-1")
        .activityType(ActivityType.APP_SCAN)
        .componentCount(1)
        .build();

    assertThat(event.getBillingMonth()).isEqualTo(expected);
  }

  @Test
  public void builderFromContext_setsPerEventFieldsViaBuilder() {
    ConsumptionContext.set("org", "APP_BASED", "UI");

    ConsumptionEvent event = ConsumptionEvents.builderFromContext(ConsumptionContext.get())
        .appId("app-set-by-caller")
        .scanId("scan-set-by-caller")
        .userId("user-set-by-caller")
        .activityType(ActivityType.APP_SCAN)
        .componentCount(7)
        .build();

    assertThat(event.getAppId()).isEqualTo("app-set-by-caller");
    assertThat(event.getScanId()).isEqualTo("scan-set-by-caller");
    assertThat(event.getUserId()).isEqualTo("user-set-by-caller");
    assertThat(event.getActivityType()).isEqualTo(ActivityType.APP_SCAN);
    assertThat(event.getComponentCount()).isEqualTo(7);
  }

  @Test
  public void builderFromContext_allowsCallerToOverrideSource() {
    ConsumptionContext.set("org", "APP_BASED", "BACKGROUND_JOB");

    ConsumptionEvent event = ConsumptionEvents.builderFromContext(ConsumptionContext.get())
        .appId("app-1")
        .activityType(ActivityType.VERSION_RECOMMENDATION)
        .componentCount(1)
        .source("UI")
        .build();

    assertThat(event.getSource()).isEqualTo("UI");
  }

  @Test(expected = NullPointerException.class)
  public void builderFromContext_nullContext_throwsNpe() {
    ConsumptionEvents.builderFromContext(null);
  }

  @Test
  public void builderFromContext_nullOrgId_throwsNpeWithContext() {
    ConsumptionContext.set(null, "APP_BASED", "UI");

    assertThatThrownBy(() -> ConsumptionEvents.builderFromContext(ConsumptionContext.get()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("orgId");
  }

  @Test
  public void builderFromContext_nullTier_throwsNpeWithContext() {
    ConsumptionContext.set("tenant-1", null, "UI");

    assertThatThrownBy(() -> ConsumptionEvents.builderFromContext(ConsumptionContext.get()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tier");
  }

  @Test
  public void builderFromContext_nullSource_throwsNpeWithContext() {
    ConsumptionContext.set("tenant-1", "APP_BASED", null);

    assertThatThrownBy(() -> ConsumptionEvents.builderFromContext(ConsumptionContext.get()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("source");
  }
}
