/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Inject;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import org.apache.http.HttpEntity;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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

  private final HdsClient mockHdsClient = mock(HdsClient.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testSend_Empty() throws Exception {
    telemetrySender.start();

    telemetrySender.send(Collections.emptyList());

    Thread.sleep(1000);
    verifyNoInteractions(mockHdsClient);
  }

  @Test
  public void testSend() throws Exception {
    telemetrySender.start();

    TelemetryData telemetryDataSend = new TelemetryData(TelemetryPurpose.DATABASE);
    telemetryDataSend.put("test-key", "test-value");

    Date expectedMinCreateTime = new Date();
    telemetrySender.send(telemetryDataSend);
    Date expectedMaxCreateTime = new Date();

    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(mockHdsClient, timeout(10000)).post(eq(TelemetrySender.RESOURCE_PATH), entityCaptor.capture(), eq(null));
    HttpEntity httpEntity = entityCaptor.getValue();
    ByteArrayDataSource multipartDataSource = new ByteArrayDataSource(httpEntity.getContent(), "multipart/form-data");
    MimeMultipart multipart = new MimeMultipart(multipartDataSource);
    BodyPart bodyPart = multipart.getBodyPart(0);
    String filename = bodyPart.getFileName();
    assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);

    try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
      ObjectMapper json = new ObjectMapper().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
      TelemetryHeader telemetryHeaderReceived = json.readValue(zipInputStream, TelemetryHeader.class);
      assertThat(telemetryHeaderReceived.getCreateTime()).isAfterOrEqualTo(expectedMinCreateTime)
          .isBeforeOrEqualTo(expectedMaxCreateTime);
      assertThat(telemetryHeaderReceived.getTelemetryId()).isEqualTo(telemetryId.getId());
      assertThat(telemetryHeaderReceived.getProduct())
          .isEqualTo(TelemetrySender.PRODUCT_PREFIX + "/" + versionService.getVersion());
      assertThat(telemetryHeaderReceived.getBuildNumber())
          .isEqualTo(versionService.getBuild());
      assertThat(telemetryHeaderReceived.getFormat()).isEqualTo(TelemetrySender.FILE_FORMAT);
      assertThat(telemetryHeaderReceived.getClusterId()).isEqualTo(telemetryHeaderReceived.getTelemetryId());

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      TelemetryData[] telemetryDataReceived = json.readValue(zipInputStream, TelemetryData[].class);
      assertThat(telemetryDataReceived).hasSize(1);
      TelemetryData telemetryData = telemetryDataReceived[0];
      assertThat(telemetryData.getAttributes()).isEqualTo(telemetryDataSend.getAttributes());
      assertThat(telemetryData.getTimestamp()).isEqualTo(telemetryDataSend.getTimestamp());
    }
  }

  @Test
  public void testSend_ExceptionsAreHandled() {
    telemetrySender.start();

    RuntimeException exception = new RuntimeException();
    doThrow(exception).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));

    telemetrySender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));

    verify(mockHdsClient, timeout(10000)).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));
    await().atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(logOutput).atDebugLevel().contains("Failed to send telemetry.", exception));
  }

  @Test
  public void testSend_ClientUserAgent() {
    telemetrySender.start();

    String clientUserAgent = "test_client_user_agent";

    telemetrySender.send(new TelemetryData(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION), clientUserAgent);

    verify(mockHdsClient, timeout(10000)).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class),
        eq(clientUserAgent));
  }
}
