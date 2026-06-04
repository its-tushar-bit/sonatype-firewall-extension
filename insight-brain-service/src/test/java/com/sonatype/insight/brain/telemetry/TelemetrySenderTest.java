/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.common.config.ConfigUtil;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.http.HttpEntity;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
  private VersionService versionService;

  @Inject
  private TelemetryId telemetryId;

  private final HdsClient mockHdsClient = mock(HdsClient.class);

  @Test
  public void testSend_Empty() {
    TelemetrySender sender = newSender();
    sender.start();

    // An empty send returns immediately without queuing, so there is nothing async to wait for.
    sender.send(Collections.emptyList());

    verifyNoInteractions(mockHdsClient);
    sender.stop();
  }

  @Test
  public void testSend() throws Exception {
    TelemetrySender sender = newSender();
    sender.start();

    TelemetryData telemetryDataSend = new TelemetryData(TelemetryPurpose.DATABASE);
    telemetryDataSend.put("test-key", "test-value");

    Date expectedMinCreateTime = new Date();
    sender.send(telemetryDataSend);
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
      assertThat(telemetryHeaderReceived.getClusterId()).isEqualTo(telemetryId.getClusterId());

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
      TelemetryData[] telemetryDataReceived = json.readValue(zipInputStream, TelemetryData[].class);
      assertThat(telemetryDataReceived).hasSize(1);
      TelemetryData telemetryData = telemetryDataReceived[0];
      assertThat(telemetryData.getAttributes()).isEqualTo(telemetryDataSend.getAttributes());
      assertThat(telemetryData.getTimestamp()).isEqualTo(telemetryDataSend.getTimestamp());
    }
    sender.stop();
  }

  @Test
  public void testSend_ExceptionsAreHandled() {
    TelemetrySender sender = newSender();
    sender.start();

    RuntimeException exception = new RuntimeException();
    doThrow(exception).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));

    sender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));

    verify(mockHdsClient, timeout(10000)).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), eq(null));
    await().atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(logOutput).atErrorLevel().contains("Failed to send telemetry.", exception));
    sender.stop();
  }

  @Test
  public void testSend_ClientUserAgent() {
    TelemetrySender sender = newSender();
    sender.start();

    String clientUserAgent = "test_client_user_agent";

    sender.send(new TelemetryData(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION), clientUserAgent);

    verify(mockHdsClient, timeout(10000)).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class),
        eq(clientUserAgent));
    sender.stop();
  }

  @Test
  public void testStop_drainsQueuedItems() throws Exception {
    CountDownLatch firstItemStarted = new CountDownLatch(1);
    CountDownLatch firstItemBlocked = new CountDownLatch(1);

    // Block the submitter thread on the first item so the second item sits in the queue
    doAnswer(invocation -> {
      firstItemStarted.countDown();
      firstItemBlocked.await(10, TimeUnit.SECONDS);
      return null;
    }).doNothing().when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());

    TelemetrySender sender = newSender();
    sender.start();
    sender.send(new TelemetryData(TelemetryPurpose.DATABASE));
    assertThat(firstItemStarted.await(10, TimeUnit.SECONDS)).isTrue();

    // Second item is now queued while submitter is still blocked on first
    sender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));

    // This allows the submitter thread to finish its current loop and then see the interrupt
    firstItemBlocked.countDown();

    // stop() triggers the interrupt; because the first item is finished, the submitter exits
    // and drainQueue() runs synchronously to process the second item
    sender.stop();

    verify(mockHdsClient, times(2)).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());
  }

  @Test
  public void testStop_completesWithinTimeout() throws Exception {
    TelemetrySender sender = newSender();
    sender.start();
    sender.send(new TelemetryData(TelemetryPurpose.DATABASE));

    verify(mockHdsClient, timeout(10000)).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());

    long startMs = System.currentTimeMillis();
    sender.stop();
    long elapsedMs = System.currentTimeMillis() - startMs;

    assertThat(elapsedMs).isLessThan(10_000);
  }

  @Test
  public void testStop_noItemsQueued_doesNotHang() {
    TelemetrySender sender = newSender();
    sender.start();

    long startMs = System.currentTimeMillis();
    sender.stop();
    long elapsedMs = System.currentTimeMillis() - startMs;

    assertThat(elapsedMs).isLessThan(10_000);
    verifyNoInteractions(mockHdsClient);
  }

  @Test
  public void testSend_queueFull_dropsEntries() throws Exception {
    CountDownLatch submitterBlocked = new CountDownLatch(1);
    CountDownLatch releaseSubmitter = new CountDownLatch(1);

    doAnswer(invocation -> {
      submitterBlocked.countDown();
      releaseSubmitter.await(30, TimeUnit.SECONDS);
      return null;
    }).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());

    // Use a small queue so we can fill it with a handful of items instead of serializing 10k+ zips.
    TelemetrySender sender = newSenderWithQueueSize(2);
    sender.start();

    // The first item is taken by the submitter, which then blocks, leaving the queue free to fill.
    sender.send(new TelemetryData(TelemetryPurpose.DATABASE));
    assertThat(submitterBlocked.await(10, TimeUnit.SECONDS)).isTrue();

    // Overfill the 2-slot queue so the extras are dropped.
    for (int i = 0; i < 5; i++) {
      sender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));
    }

    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(logOutput).atWarnLevel().contains("Telemetry queue full"));

    // The drop warning attributes drops to the purpose that overflowed the queue.
    assertThat(logOutput).atWarnLevel()
        .contains(log -> log.contains("dropped by purpose", TelemetryPurpose.HIERARCHY_METRICS.name()));

    releaseSubmitter.countDown();
    sender.stop();
  }

  @Test
  public void testSend_queueFull_recoveryLogsInfo() throws Exception {
    CountDownLatch submitterBlocked = new CountDownLatch(1);
    CountDownLatch releaseSubmitter = new CountDownLatch(1);

    doAnswer(invocation -> {
      submitterBlocked.countDown();
      releaseSubmitter.await(30, TimeUnit.SECONDS);
      return null;
    }).doNothing().when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());

    TelemetrySender sender = newSenderWithQueueSize(2);
    sender.start();

    sender.send(new TelemetryData(TelemetryPurpose.DATABASE));
    assertThat(submitterBlocked.await(10, TimeUnit.SECONDS)).isTrue();

    // Overfill so entries are dropped and the "queue full" warning fires.
    for (int i = 0; i < 5; i++) {
      sender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));
    }
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(logOutput).atWarnLevel().contains("Telemetry queue full"));

    // Release the submitter so it drains the queue; once it catches up a send() is accepted again.
    // The recovery log is emitted by send() itself, so keep sending until one is accepted post-backpressure.
    releaseSubmitter.countDown();
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      sender.send(new TelemetryData(TelemetryPurpose.DATABASE));
      assertThat(logOutput).atInfoLevel().contains("Telemetry queue accepting entries again");
    });

    sender.stop();
  }

  private TelemetrySender newSender() {
    return newSender(TelemetrySender.DEFAULT_MAX_QUEUE_SIZE, TelemetrySender.DEFAULT_DRAIN_TIMEOUT_MS);
  }

  private TelemetrySender newSenderWithQueueSize(int maxQueueSize) {
    return newSender(maxQueueSize, TelemetrySender.DEFAULT_DRAIN_TIMEOUT_MS);
  }

  private TelemetrySender newSender(int maxQueueSize, long drainTimeoutMs) {
    TelemetryReceiptService receiptService = mock(TelemetryReceiptService.class);
    // lenient: tests that never enqueue (e.g. empty/no-op sends) won't exercise this stub.
    lenient().when(receiptService.onTelemetrySubmitted(any(), anyLong()))
        .thenReturn(mock(TelemetryReceiptService.TelemetryReceipt.class));
    return new TelemetrySender(mockHdsClient, versionService, telemetryId, mock(TenantUtil.class),
        receiptService, maxQueueSize, drainTimeoutMs);
  }

  @Test
  public void testDrainQueue_respectsTimeout() throws Exception {
    // Each submission is slow so the 1ms drain budget is exceeded after the first drained item.
    doAnswer(invocation -> {
      Thread.sleep(50);
      return null;
    }).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());

    // 1ms drain timeout; the submitter is never started, so drainQueue() processes the queued backlog directly,
    // making the test deterministic instead of depending on interrupting the submitter mid-request.
    TelemetrySender sender = newSender(TelemetrySender.DEFAULT_MAX_QUEUE_SIZE, 1L);
    for (int i = 0; i < 10; i++) {
      sender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));
    }

    sender.drainQueue();

    assertThat(logOutput).atWarnLevel().contains("Telemetry drain timed out");
  }

  @Test
  public void testSend_outboundTelemetryDisabled_doesNotQueueOrSend() {
    try (var configUtil = mockStatic(ConfigUtil.class)) {
      configUtil.when(() -> ConfigUtil.getBooleanConfig(HdsClient.DISABLE_TELEMETRY_CONFIG_KEY, false))
          .thenReturn(true);

      TelemetrySender sender = newSenderWithQueueSize(2);
      sender.start();

      // More sends than the queue can hold: with telemetry disabled none are queued, so nothing is sent or dropped.
      for (int i = 0; i < 5; i++) {
        sender.send(new TelemetryData(TelemetryPurpose.DATABASE));
      }

      sender.stop();

      verifyNoInteractions(mockHdsClient);
      assertThat(logOutput).atWarnLevel().doesNotContain("Telemetry queue full");
    }
  }

  @Test
  public void testExponentialBackoffThrottle_backsOffAndResets() {
    TelemetrySender.ExponentialBackoffThrottle throttle = new TelemetrySender.ExponentialBackoffThrottle();

    long oneMinute = Duration.ofMinutes(1).toMillis();

    assertThat(throttle.shouldLog(0)).isTrue(); // first call logs immediately
    assertThat(throttle.shouldLog(oneMinute - 1)).isFalse(); // suppressed within the 1m interval
    assertThat(throttle.shouldLog(oneMinute)).isTrue(); // next allowed after 1m; interval doubles to 2m
    assertThat(throttle.shouldLog(3 * oneMinute - 1)).isFalse(); // suppressed within the 2m interval
    assertThat(throttle.shouldLog(3 * oneMinute)).isTrue(); // next allowed after 2m more; interval doubles to 4m
    assertThat(throttle.shouldLog(7 * oneMinute - 1)).isFalse(); // suppressed within the 4m interval

    throttle.reset();

    assertThat(throttle.shouldLog(7 * oneMinute - 1)).isTrue(); // reset restores immediate logging
  }

  @Test
  public void testExponentialBackoffThrottle_capsAtMaxInterval() {
    TelemetrySender.ExponentialBackoffThrottle throttle = new TelemetrySender.ExponentialBackoffThrottle();

    long oneMinute = Duration.ofMinutes(1).toMillis();

    // Walk through the doubling sequence past the 60m cap. Without the cap, the gap after log 7 (at 63m) would
    // be 64m -> next fire at 127m; with the cap, the gap clamps to 60m -> next fire at 123m.
    assertThat(throttle.shouldLog(0)).isTrue(); // log 1, sets interval to 2m
    assertThat(throttle.shouldLog(oneMinute)).isTrue(); // log 2 at 1m, interval becomes 4m
    assertThat(throttle.shouldLog(3 * oneMinute)).isTrue(); // log 3 at 3m, interval becomes 8m
    assertThat(throttle.shouldLog(7 * oneMinute)).isTrue(); // log 4 at 7m, interval becomes 16m
    assertThat(throttle.shouldLog(15 * oneMinute)).isTrue(); // log 5 at 15m, interval becomes 32m
    assertThat(throttle.shouldLog(31 * oneMinute)).isTrue(); // log 6 at 31m, doubling would be 64m but clamps to 60m
    assertThat(throttle.shouldLog(63 * oneMinute)).isTrue(); // log 7 at 63m, gap was 32m, interval stays at 60m
    assertThat(throttle.shouldLog(123 * oneMinute - 1)).isFalse(); // still suppressed within the capped 60m gap
    assertThat(throttle.shouldLog(123 * oneMinute)).isTrue(); // capped (60m) gap, not the un-clamped 64m
  }

  @Test
  public void testSubmitter_queueDepthWarning_firesAboveThresholdWithAttribution() throws Exception {
    CountDownLatch submitterBlocked = new CountDownLatch(1);
    CountDownLatch releaseSubmitter = new CountDownLatch(1);

    // First post blocks the submitter; subsequent posts return immediately.
    doAnswer(invocation -> {
      submitterBlocked.countDown();
      releaseSubmitter.await(30, TimeUnit.SECONDS);
      return null;
    }).doNothing().when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class), any());

    // queue size 4 -> warn threshold = maxQueueSize / 2 = 2.
    TelemetrySender sender = newSenderWithQueueSize(4);
    sender.start();

    // Submitter takes item 1 and blocks, leaving the queue free to fill past the threshold.
    sender.send(new TelemetryData(TelemetryPurpose.DATABASE));
    assertThat(submitterBlocked.await(10, TimeUnit.SECONDS)).isTrue();

    // 3 more items take the queued depth to 3 (> threshold 2).
    for (int i = 0; i < 3; i++) {
      sender.send(new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS));
    }

    // Release the submitter; it completes item 1, observes depth = 3 > 2, crosses up and warns.
    releaseSubmitter.countDown();

    await().atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(logOutput).atWarnLevel().contains("Telemetry queue depth is 3"));

    // Both attribution sections (episode and lifetime) are present, and lifetime reflects the recent submissions.
    assertThat(logOutput).atWarnLevel()
        .contains(line -> line.contains("during current backpressure episode", "since startup",
            TelemetryPurpose.HIERARCHY_METRICS.name()));

    sender.stop();
  }
}
