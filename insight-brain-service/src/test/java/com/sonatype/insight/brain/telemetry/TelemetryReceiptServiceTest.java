/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceipt;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceiptDTO;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceiptsDTO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.telemetry.model.TelemetryPurpose.APPLICATION_CATEGORY;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.AUTO_POLICY_WAIVER;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.HISTORICAL_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.REST_ENDPOINT_USAGE;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TelemetryReceiptServiceTest
{
  @Mock
  private Configuration mockConfiguration;

  @Before
  public void setUp() {
    asProdHds(true);
    asBaseUrl("https://app.sonatype.com");
  }

  @Test
  public void testMissingConfiguration() {
    // given: no configuration mock
    final var testSubject = new TelemetryReceiptService(null);

    // when:
    var receipts = testSubject.getReceipts(List.of(""));

    // then:
    assertThat(receipts).isNotNull();
    assertThat(receipts.isLocalEnv()).isFalse();
    assertThat(receipts.isUsingProdHds()).isFalse();
    assertThat(receipts.captureExpirationTime()).isNull();
  }

  @Test
  public void testEnable_nonLocalhost() {
    // given:
    asBaseUrl("https://app.sonatype.com");
    final var testSubject = new TelemetryReceiptService(mockConfiguration);

    // when:
    var enabledHours = testSubject.enable(1);

    // then: enable works regardless of host
    assertThat(enabledHours).isEqualTo(1);
    var receipts = testSubject.getReceipts(List.of(""));
    assertThat(receipts.isLocalEnv()).isFalse();
    assertThat(receipts.captureExpirationTime()).isNotNull();
  }

  @Test
  public void testEnable_localhost() {
    // given:
    asBaseUrl("http://localhost:8080");
    final var testSubject = new TelemetryReceiptService(mockConfiguration);

    final var enableHours = 1;
    final var beforeExpirationTime = LocalDateTime.now().plusHours(enableHours).minusSeconds(1);
    final var afterExpirationTime = LocalDateTime.now().plusHours(enableHours).plusMinutes(1);

    // when:
    var enabledHours = testSubject.enable(enableHours);

    // then:
    var receipts = testSubject.getReceipts(List.of("auto_policy_waiver", "historical_policy_violation"));
    assertThat(enabledHours).isEqualTo(enableHours);
    assertThat(receipts.isLocalEnv()).isTrue();
    assertThat(receipts.captureStartTime()).isNotNull();
    assertThat(receipts.captureExpirationTime()).isAfter(beforeExpirationTime);
    assertThat(receipts.captureExpirationTime()).isBefore(afterExpirationTime);
    assertThat(receipts.detailPurposes()).containsOnly(HISTORICAL_POLICY_VIOLATION, AUTO_POLICY_WAIVER);
  }

  @Test
  public void testEnable_minMaxHours() {
    // given:
    final var testSubject = new TelemetryReceiptService(mockConfiguration);
    final var minBeforeExpirationTime = LocalDateTime.now().plusHours(1).minusSeconds(1);
    final var minAfterExpirationTime = LocalDateTime.now().plusHours(1).plusMinutes(1);
    final var maxBeforeExpirationTime = LocalDateTime.now().plusHours(24).minusSeconds(1);
    final var maxAfterExpirationTime = LocalDateTime.now().plusHours(24).plusMinutes(1);

    // when: exceed min hours
    var enabledHours = testSubject.enable(0);

    // then:
    var receipts = testSubject.getReceipts(List.of(""));
    assertThat(enabledHours).isEqualTo(1);
    assertThat(receipts.captureStartTime()).isNotNull();
    assertThat(receipts.captureExpirationTime()).isAfter(minBeforeExpirationTime);
    assertThat(receipts.captureExpirationTime()).isBefore(minAfterExpirationTime);

    // when: exceed max hours
    enabledHours = testSubject.enable(25);

    // then:
    receipts = testSubject.getReceipts(List.of(""));
    assertThat(enabledHours).isEqualTo(24);
    assertThat(receipts.captureStartTime()).isNotNull();
    assertThat(receipts.captureExpirationTime()).isAfter(maxBeforeExpirationTime);
    assertThat(receipts.captureExpirationTime()).isBefore(maxAfterExpirationTime);
  }

  @Test
  public void testGetReceipts_detailPurposes() {
    // given: set of purposes including an invalid one
    final var testSubject = new TelemetryReceiptService(mockConfiguration);
    testSubject.enable(1);

    List<String> detailPurposes = List.of(
        REST_ENDPOINT_USAGE.name(),
        APPLICATION_CATEGORY.name(),
        "historical_policy_violation", // lowercase
        "INVALID_PURPOSE" // Should be ignored
    );

    // when:
    TelemetryReceiptsDTO receipts = testSubject.getReceipts(detailPurposes);

    // then: verify only valid purposes are included
    assertThat(receipts).isNotNull();
    assertThat(receipts.detailPurposes()).hasSize(3);
    assertThat(receipts.detailPurposes()).contains(
        APPLICATION_CATEGORY,
        HISTORICAL_POLICY_VIOLATION,
        REST_ENDPOINT_USAGE);
  }

  @Test
  public void testDisable() {
    final var testSubject = new TelemetryReceiptService(mockConfiguration);

    // when:
    testSubject.disable();

    // then:
    var receipts = testSubject.getReceipts(Collections.emptyList());
    assertThat(receipts.captureExpirationTime()).isNull();
    assertThat(receipts.telemetryReceipts()).isEmpty();

    // when: enable and then disable
    testSubject.enable(1);
    testSubject.disable();

    // then:
    receipts = testSubject.getReceipts(List.of(""));
    assertThat(receipts.captureStartTime()).isNull();
    assertThat(receipts.captureExpirationTime()).isNull();
  }

  @Test
  public void testOnTelemetrySubmitted_notEnabled() {
    // given: telemetry service not enabled
    final var testSubject = new TelemetryReceiptService(mockConfiguration);

    // when:
    submitTelemetryFor(testSubject, HISTORICAL_POLICY_VIOLATION);

    // then:
    TelemetryReceiptsDTO receipts = testSubject.getReceipts(Collections.emptyList());
    assertThat(receipts).isNotNull();
    assertThat(receipts.telemetryReceipts()).isEmpty();
  }

  @Test
  public void testOnTelemetrySubmitted() throws InterruptedException {
    // given: telemetry service enabled
    final var testSubject = new TelemetryReceiptService(mockConfiguration);
    testSubject.enable(1);

    // when:
    TelemetryReceipt receipt = submitTelemetryFor(testSubject, HISTORICAL_POLICY_VIOLATION, APPLICATION_CATEGORY);

    // then:
    TelemetryReceiptsDTO receipts = testSubject.getReceipts(Collections.emptyList());
    assertThat(receipts.telemetryReceipts()).hasSize(1);

    var receiptDTO = receipts.telemetryReceipts().get(0);
    assertThat(receiptDTO.submitTime()).isNotNull();
    assertThat(receiptDTO.queueTimeMs()).isEqualTo(-1);
    assertThat(receiptDTO.httpTimeMs()).isEqualTo(-1);

    var purposeData = receiptDTO.dataByPurpose();
    assertThat(purposeData).containsOnlyKeys(HISTORICAL_POLICY_VIOLATION, APPLICATION_CATEGORY);

    // when: telemetry marked as sending (http call initiating) and receipts re-fetched
    final var queueTime = 100L;
    sleep(queueTime);
    receipt.markSending();

    // then:
    receipts = testSubject.getReceipts(Collections.emptyList());
    receiptDTO = receipts.telemetryReceipts().get(0);
    assertThat(receiptDTO.queueTimeMs()).isGreaterThanOrEqualTo(queueTime);
    assertThat(receiptDTO.httpTimeMs()).isEqualTo(-1);

    // when: telemetry marked as sent (http call completed) and receipts re-fetched
    final var httpTime = 200L;
    sleep(httpTime);
    receipt.markSent();
    receipts = testSubject.getReceipts(Collections.emptyList());
    receiptDTO = receipts.telemetryReceipts().get(0);
    assertThat(receiptDTO.queueTimeMs()).isGreaterThanOrEqualTo(queueTime);
    assertThat(receiptDTO.httpTimeMs()).isGreaterThanOrEqualTo(httpTime);
  }

  @Test
  public void testOnTelemetrySubmitted_withError() {
    // given: telemetry service enabled and telemetry submitted with simulated error
    final var testSubject = new TelemetryReceiptService(mockConfiguration);
    testSubject.enable(1);

    // Create test telemetry data
    var receipt = submitTelemetryFor(testSubject, HISTORICAL_POLICY_VIOLATION);

    // Simulate error
    Exception error = new RuntimeException("Test error");
    receipt.markInError(error);

    // when:
    TelemetryReceiptsDTO receipts = testSubject.getReceipts(Collections.emptyList());

    // then:
    assertThat(receipts).isNotNull();
    assertThat(receipts.telemetryReceipts()).hasSize(1);

    TelemetryReceiptDTO receiptDTO = receipts.telemetryReceipts().get(0);
    assertThat(receiptDTO.errorMessage()).contains("Test error");
  }

  private TelemetryReceipt submitTelemetryFor(TelemetryReceiptService receiptService, TelemetryPurpose... purposes) {
    var telemetryData = new ArrayList<TelemetryData>();
    for (TelemetryPurpose purpose : purposes) {
      telemetryData.add(new TelemetryData(purpose));
    }

    // Submit telemetry data
    return receiptService.onTelemetrySubmitted(telemetryData, System.currentTimeMillis());
  }

  private void asBaseUrl(String baseUrl) {
    when(mockConfiguration.getBaseUrl()).thenReturn(baseUrl);
  }

  private void asProdHds(boolean useProdHds) {
    when(mockConfiguration.getHdsUrl()).thenReturn(
        useProdHds ? "https://clm.sonatype.com" : "https://clm-staging.sonatype.com");
  }
}
