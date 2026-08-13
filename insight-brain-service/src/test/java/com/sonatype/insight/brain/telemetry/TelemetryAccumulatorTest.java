/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TelemetryAccumulatorTest
{
  @Test
  public void testAdd() {
    // given: some default accumulator with no records
    final var batchSize = 2;
    final var testPurpose = TelemetryPurpose.APPLICATION_CATEGORY;
    final var testTimestamp = System.currentTimeMillis();
    final var mockTelemetrySender = mock(TelemetrySender.class);
    final var testSubject = new TelemetryAccumulator(testPurpose, mockTelemetrySender, batchSize);

    // when: adding a single record
    final var testTelemetryData = new TelemetryData(testPurpose, testTimestamp);
    var recordsSent = testSubject.add(testTelemetryData);

    // then: the record was added, but not sent
    assertThat(recordsSent).isZero();

    // when: adding a second record
    final var testTelemetryData2 = new TelemetryData(testPurpose, testTimestamp);

    recordsSent = testSubject.add(testTelemetryData2);

    // then: the records were added and sent
    assertThat(recordsSent).isEqualTo(2);

    ArgumentCaptor<List<TelemetryData>> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockTelemetrySender, times(1)).send(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).containsExactly(testTelemetryData, testTelemetryData2);

    // when: flushing now
    recordsSent = testSubject.flush();

    // then: nothing was sent
    assertThat(recordsSent).isZero();
  }

  @Test
  public void testFlush_whenEmpty() {
    // given: some default accumulator
    final var batchSize = 10;
    final var telemetrySender = mock(TelemetrySender.class);
    final var testSubject = new TelemetryAccumulator(TelemetryPurpose.APPLICATION_CATEGORY, telemetrySender, batchSize);

    // when: flushing the accumulator
    var recordsFlushed = testSubject.flush();

    // then:
    assertThat(recordsFlushed).isZero();
    verify(telemetrySender, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testFlush_whenNotEmpty() {
    // given: some default accumulator with one record in it
    final var batchSize = 10;
    final var testPurpose = TelemetryPurpose.APPLICATION_CATEGORY;
    final var testTimestamp = System.currentTimeMillis();
    final var mockTelemetrySender = mock(TelemetrySender.class);
    final var testSubject = new TelemetryAccumulator(testPurpose, mockTelemetrySender, batchSize);

    final var testTelemetryData = new TelemetryData(testPurpose, testTimestamp);
    testSubject.add(testTelemetryData);

    // when: flushing the accumulator
    var recordsFlushed = testSubject.flush();

    // then: whatever was in the accumulator was sent
    assertThat(recordsFlushed).isOne();

    ArgumentCaptor<List<TelemetryData>> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(mockTelemetrySender, times(1)).send(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).containsExactly(testTelemetryData);

    // when: flush again
    recordsFlushed = testSubject.flush();

    // then: nothing was sent
    assertThat(recordsFlushed).isZero();
  }
}
