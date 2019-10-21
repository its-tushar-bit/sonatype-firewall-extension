/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import org.apache.http.HttpEntity;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class TelemetrySenderTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(TelemetrySender.class);

  @Inject
  private TelemetrySender telemetrySender;

  @Inject
  private VersionService versionService;

  @Inject
  private TelemetryId telemetryId;

  @Inject
  private HierarchyMetricsTelemetryCollector telemetryCollector;

  private HdsClient mockHdsClient = mock(HdsClient.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testSend_Empty() {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(mockHdsClient)
        .post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));

    telemetrySender.send(Collections.emptyList());

    assertThat(invocation[0]).isNull();
  }

  @Test
  public void testSend() throws Exception {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];

    doAnswer(x -> invocation[0] = x).when(mockHdsClient)
        .post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));

    TelemetryData telemetryDataSend = telemetryCollector.collectData();

    Date expectedMinCreateTime = new Date();
    telemetrySender.send(telemetryDataSend);
    assertThat(TelemetrySender.RESOURCE_PATH).isEqualTo(invocation[0].getArguments()[0]);
    Date expectedMaxCreateTime = new Date();

    HttpEntity httpEntity = (HttpEntity) invocation[0].getArguments()[1];
    ByteArrayDataSource multipartDataSource = new ByteArrayDataSource(httpEntity.getContent(), "multipart/form-data");
    MimeMultipart multipart = new MimeMultipart(multipartDataSource);
    BodyPart bodyPart = multipart.getBodyPart(0);
    String filename = bodyPart.getFileName();
    assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);

    try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
      byte[] buffer = new byte[1024];

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
      zipInputStream.read(buffer);
      TelemetryHeader telemetryHeaderReceived = JsonUtils.parse(buffer, TelemetryHeader.class);
      assertThat(telemetryHeaderReceived.getCreateTime()).isAfterOrEqualTo(expectedMinCreateTime)
          .isBeforeOrEqualTo(expectedMaxCreateTime);
      assertThat(telemetryHeaderReceived.getTelemetryId()).isEqualTo(telemetryId.getId());
      assertThat(telemetryHeaderReceived.getProduct())
          .isEqualTo(TelemetrySender.PRODUCT_PREFIX + "/" + versionService.getVersion());
      assertThat(telemetryHeaderReceived.getFormat()).isEqualTo(TelemetrySender.FILE_FORMAT);

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      zipInputStream.read(buffer);
      List<TelemetryData> telemetryDataReceived =
          new ObjectMapper().readValue(buffer, new TypeReference<List<TelemetryData>>() { });
      assertThat(telemetryDataReceived).hasSize(1);
      TelemetryData telemetryData = telemetryDataReceived.get(0);
      assertThat(telemetryData.getAttributes())
          .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
          .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "0");
      assertThat(telemetryData.getTimestamp()).isEqualTo(telemetryDataSend.getTimestamp());
    }
  }

  @Test
  public void testSend_ExceptionsAreHandled() throws Exception {
    RuntimeException exception = new RuntimeException();
    doThrow(exception).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));

    telemetrySender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));

    assertThat(logOutput).atDebugLevel().contains("Failed to send telemetry.", exception);
  }

  @Test
  public void testSend_ClientUserAgent() throws Exception {
    String clientUserAgent = "test_client_user_agent";
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class),
        eq(clientUserAgent));

    telemetrySender.send(new TelemetryData(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION), clientUserAgent);

    // If invocation[0] is not null, then the mock was called with the right client user agent value.
    assertThat(invocation[0]).isNotNull();
  }
}
