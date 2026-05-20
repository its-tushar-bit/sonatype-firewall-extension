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

public class ConsumptionResourceExportTest
    extends AbstractConsumptionResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ConsumptionResource.RESOURCE_PATH);
  }

  @Test
  public void exportCsv_returnsCsvContentType() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).contains("text/csv");
  }

  @Test
  public void exportCsv_hasAttachmentContentDispositionWithTimestampedFilename() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    String contentDisposition = response.getHeader("Content-Disposition");
    assertThat(contentDisposition)
        .isNotNull()
        .contains("attachment")
        .containsPattern(ConsumptionResource.EXPORT_FILE_PREFIX + "-\\d{8}-\\d{6}\\.csv");
  }

  @Test
  public void exportCsv_firstLineIsCanonicalHeader() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);

    String body = response.getBodyText();
    assertThat(body).isNotNull();

    String headerLine = body.split("\r\n", 2)[0];
    assertThat(headerLine).isEqualTo(ConsumptionHistoryEntryDTO.getCsvHeader());
  }

  @Test
  public void exportCsv_usesCrlfLineSeparators() throws Exception {
    seedOneConsumptionEvent();

    User adminUser = createSystemAdminUser();
    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    String body = response.getBodyText();
    assertThat(body).startsWith(ConsumptionHistoryEntryDTO.getCsvHeader() + "\r\n");
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
    event.setBillingMonth(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
    tempEntity.insertConsumptionEvents(Collections.singletonList(event));
  }

  @Test
  public void exportCsv_asUsageViewer_returns200() throws Exception {
    User usageViewerUser = createUsageViewerUser();

    HttpResponse response = restRequest().auth(usageViewerUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  public void exportCsv_anonymous_returnsUnauthorized() throws Exception {
    HttpResponse response = restRequest().anon()
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_UNAUTHORIZED, response);
  }

}
