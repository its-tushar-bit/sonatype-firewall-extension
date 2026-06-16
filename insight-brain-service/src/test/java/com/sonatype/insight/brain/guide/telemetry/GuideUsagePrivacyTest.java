/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.After;
import org.junit.Test;

public class GuideUsagePrivacyTest
{
  private GuideUsageTelemetryCollector newCollector() {
    TelemetryId telemetryId = mock(TelemetryId.class);
    when(telemetryId.getId()).thenReturn("t");
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUsernameOrSystem()).thenReturn("secret-user");
    return new GuideUsageTelemetryCollector(telemetryId, currentUser, mock(TelemetrySender.class), () -> 1L);
  }

  @After
  public void tearDown() {
    GuideChannelContext.clear();
  }

  @Test
  public void userIdIsHashedAndSearchCarriesNoIdentifier() {
    GuideUsageTelemetryCollector collector = newCollector();
    GuideChannelContext.set(GuideChannel.API);
    // a "search" invocation whose only arg is the page size; no identifier/query must be captured
    collector.record(GuideOperationType.GLOBAL_SEARCH, new Object[]{Integer.valueOf(25)});

    TelemetryData td = collector.collectAllData().get(0);
    assertThat((String) td.getAttributes().get("user_id")).isNotEqualTo("secret-user").hasSize(64);
    assertThat(td.getAttributes()).doesNotContainKey("query");
    assertThat(td.getAttributes()).doesNotContainKey("purl");
    assertThat(td.getAttributes()).doesNotContainKey("vulnerability_id");
    assertThat(td.getAttributes().values()).doesNotContain("secret-user");
  }

  @Test
  public void freeTextSearchQueryNeverLeaksFromRealGuideRequestDtos() {
    // Strong guard exercising the actual Guide search DTOs that ship with this feature: each carries
    // a free-text {@code query} field, and the extractor must never capture it (searches stay count-only).
    // Asserting against the real types prevents regressions when a future request type is added.
    String secret = "super-secret-internal-package-name";
    GuideComponentSearchRequest componentSearch = new GuideComponentSearchRequest(secret, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    GuideVulnerabilitySearchRequest vulnSearch = new GuideVulnerabilitySearchRequest(secret, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    GuideGlobalSearchRequest globalSearch =
        new GuideGlobalSearchRequest(secret, null, null, null, null, null, null, null);

    GuideUsageTelemetryCollector collector = newCollector();
    GuideChannelContext.set(GuideChannel.API);
    collector.record(GuideOperationType.GLOBAL_SEARCH, new Object[]{componentSearch});
    collector.record(GuideOperationType.VULNERABILITY_LOOKUP, new Object[]{vulnSearch});
    collector.record(GuideOperationType.GLOBAL_SEARCH, new Object[]{globalSearch});

    for (TelemetryData td : collector.collectAllData()) {
      assertThat(td.getAttributes()).doesNotContainKey("query");
      assertThat(td.getAttributes()).doesNotContainKey("purl");
      assertThat(td.getAttributes()).doesNotContainKey("vulnerability_id");
      assertThat(td.getAttributes().values()).doesNotContain(secret);
    }
  }
}
