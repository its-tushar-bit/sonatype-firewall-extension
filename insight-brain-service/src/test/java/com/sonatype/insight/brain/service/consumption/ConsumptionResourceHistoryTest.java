/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionHistoryEntryDTO;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BDD tests for the consumption history endpoint response shape.
 * Maps to: api-history.feature
 */
public class ConsumptionResourceHistoryTest
    extends AbstractConsumptionResourceTest
{
  // Mirrors the placeholder return value of ConsumptionResource.getSubscriptionDay()
  // (CLM-39593: read from license effective date). Update both sites together.
  private static final int RESOURCE_SUBSCRIPTION_DAY = 1;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ConsumptionResource.RESOURCE_PATH);
  }

  // BDD: History returns JSON array
  @Test
  public void getHistory_returnsJsonArray() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  // BDD: History entries contain required fields
  @Test
  public void getHistory_entriesContainRequiredFields() throws Exception {
    User adminUser = createSystemAdminUser();
    seedOneConsumptionEvent();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);

    List<ConsumptionHistoryEntryDTO> history = response.getBodyList(ConsumptionHistoryEntryDTO.class);
    assertThat(history).isNotNull();
    assertThat(history).hasSize(12);

    String currentMonth = BillingWindowUtil
        .calculateWindowStart(LocalDate.now(ZoneOffset.UTC), RESOURCE_SUBSCRIPTION_DAY)
        .toString();
    long entriesWithSeededValue = history.stream().filter(e -> e.getConsumed() == 1L).count();
    long zeroEntries = history.stream().filter(e -> e.getConsumed() == 0L).count();
    assertThat(entriesWithSeededValue).isEqualTo(1L);
    assertThat(zeroEntries).isEqualTo(11L);
    assertThat(history.stream()
        .filter(e -> e.getConsumed() == 1L)
        .findFirst()
        .map(ConsumptionHistoryEntryDTO::getMonth))
            .contains(currentMonth);
    assertThat(history).allMatch(e -> e.getMonth() != null);
  }

  private void seedOneConsumptionEvent() {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId(Organization.ROOT_ORGANIZATION_ID);
    event.setAppId("test-app-id");
    event.setEventTimestamp(Instant.now());
    event.setComponentCount(1);
    event.setActivityType(ActivityType.APP_SCAN);
    event.setSource("UI");
    event.setTier("APP_BASED");
    event.setBillingMonth(BillingWindowUtil
        .calculateWindowStart(LocalDate.now(ZoneOffset.UTC), RESOURCE_SUBSCRIPTION_DAY));
    tempEntity.insertConsumptionEvents(Collections.singletonList(event));
  }

  // BDD: History returns exactly 12 months (padded with zero entries)
  @Test
  public void getHistory_returnsExactly12Months_paddedWithZeros() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);

    List<ConsumptionHistoryEntryDTO> history = response.getBodyList(ConsumptionHistoryEntryDTO.class);
    assertThat(history).isNotNull();
    assertThat(history).hasSize(12);
  }

  // BDD: History with no events returns 12 zeroed entries (padded)
  @Test
  public void getHistory_noEvents_returns12ZeroedEntries() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);

    List<ConsumptionHistoryEntryDTO> history = response.getBodyList(ConsumptionHistoryEntryDTO.class);
    assertThat(history).isNotNull();
    assertThat(history).hasSize(12);
    assertThat(history).allMatch(entry -> entry.getConsumed() == 0L);
    assertThat(history).allMatch(entry -> entry.getMonth() != null && entry.getWindowEnd() != null);
  }

  // BDD: Usage viewer can access history
  @Test
  public void getHistory_asUsageViewer_returns200() throws Exception {
    User usageViewerUser = createUsageViewerUser();

    HttpResponse response = restRequest().auth(usageViewerUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

}
