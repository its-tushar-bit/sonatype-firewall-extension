/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.brain.model.telemetry.HistoricalTelemetryState;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.HistoricalTelemetryService.Status;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class HistoricalTelemetryServiceTest
    extends AbstractComponentTest
{
  private static final TelemetryPurpose TEST_PURPOSE = TelemetryPurpose.APPLICATION_CATEGORY;

  @Mock
  private TelemetrySender mockTelemetrySender;

  private HistoricalTelemetryStateDAO historicalTelemetryStateDAO;

  private HistoricalTelemetryService historicalTelemetryService;

  @Before
  public void setup() {
    final var batchSize = 10;
    historicalTelemetryStateDAO = daoFactory.createHistoricalTelemetryStateDAO();
    historicalTelemetryService = new TestableHistoricalTelemetryService(
        historicalTelemetryStateDAO,
        TEST_PURPOSE,
        mockTelemetrySender,
        batchSize,
        new Date());
  }

  @Test
  public void testCanCollectAndSendTelemetry() {
    // given: some initial telemetry state
    final var cutoffDate = new Date();
    final var batchSize = 10;
    final var minFreeMemoryMb = 0;

    HistoricalTelemetryState telemetryState = tempEntity.newHistoricalTelemetryState(
        TEST_PURPOSE.name(), cutoffDate, batchSize, minFreeMemoryMb, Status.PENDING.name());

    for (Status status : Status.values()) {
      // given: historical telemetry set to the given status
      telemetryState.setStatus(status.name());
      historicalTelemetryStateDAO.update(telemetryState);

      // when:
      boolean canCollectAndSend = historicalTelemetryService.canCollectAndSendTelemetry();

      // then:
      switch (status) {
        case PENDING, SKIPPED:
          assertThat(canCollectAndSend).isTrue();
          break;

        default:
          assertThat(canCollectAndSend).isFalse();
          break;
      }

      // and: no side-effects
      assertThat(telemetryState.getId()).isEqualTo(TEST_PURPOSE.name());
      assertThat(telemetryState.getCutoffDate()).isEqualTo(cutoffDate);
      assertThat(telemetryState.getBatchSize()).isEqualTo(batchSize);
      assertThat(telemetryState.getMinFreeMemoryMb()).isEqualTo(minFreeMemoryMb);
    }
  }

  @Test
  public void testCanCollectAndSendTelemetry_InsufficientMemory() {
    // given: some initial telemetry state with min memory set to zero
    tempEntity.newHistoricalTelemetryState(TEST_PURPOSE.name(), new Date(), Status.PENDING.name());
    HistoricalTelemetryState telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());
    telemetryState.setMinFreeMemoryMb(0);
    historicalTelemetryStateDAO.update(telemetryState);

    // when:
    boolean canCollectAndSend = historicalTelemetryService.canCollectAndSendTelemetry();

    // then:
    assertThat(canCollectAndSend).isTrue();

    // when: reset required memory to a high value
    telemetryState.setMinFreeMemoryMb(Integer.MAX_VALUE);
    historicalTelemetryStateDAO.update(telemetryState);
    canCollectAndSend = historicalTelemetryService.canCollectAndSendTelemetry();

    // then:
    assertThat(canCollectAndSend).isFalse();
  }

  @Test
  public void isTelemetryCollectionComplete() {
    // given: some initial telemetry state
    final var cutoffDate = new Date();
    final var batchSize = 10;
    final var minFreeMemoryMb = 0;

    HistoricalTelemetryState telemetryState = tempEntity.newHistoricalTelemetryState(
        TEST_PURPOSE.name(), cutoffDate, batchSize, minFreeMemoryMb, Status.PENDING.name());

    for (Status status : Status.values()) {
      // given: historical telemetry set to the given status
      telemetryState.setStatus(status.name());
      historicalTelemetryStateDAO.update(telemetryState);

      // when:
      boolean isTelemetryCollectionComplete = historicalTelemetryService.isTelemetryCollectionComplete();

      // then:
      switch (status) {
        case DONE:
          assertThat(isTelemetryCollectionComplete).isTrue();
          break;

        default:
          assertThat(isTelemetryCollectionComplete).isFalse();
          break;
      }

      // and: no side-effects
      assertThat(telemetryState.getId()).isEqualTo(TEST_PURPOSE.name());
      assertThat(telemetryState.getCutoffDate()).isEqualTo(cutoffDate);
      assertThat(telemetryState.getBatchSize()).isEqualTo(batchSize);
      assertThat(telemetryState.getMinFreeMemoryMb()).isEqualTo(minFreeMemoryMb);
    }
  }

  @Test
  public void isTelemetryCollectionComplete_telemetryStateNull() {
    // given:
    var telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());
    assertThat(telemetryState).isNull();

    // when:
    boolean isTelemetryCollectionComplete = historicalTelemetryService.isTelemetryCollectionComplete();

    // then:
    assertThat(isTelemetryCollectionComplete).isFalse();
  }

  @Test
  public void testInitialize() {
    // given: an initial default telemetry state
    final var testStartTime = new Date();
    final var cutoffDate = new Date();
    tempEntity.newHistoricalTelemetryState(TEST_PURPOSE.name(), cutoffDate, Status.PENDING.name());
    var telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());

    // check initial state
    assertThat(telemetryState.getStatus()).isEqualTo(Status.PENDING.name());
    assertThat(telemetryState.getStartTime()).isNull();
    assertThat(telemetryState.getLastUpdated()).isNull();
    assertThat(telemetryState.getCreated()).isNotNull();
    assertThat(historicalTelemetryService.canCollectAndSendTelemetry()).isTrue();

    // when: invoke initialize and refresh state
    historicalTelemetryService.initialize();
    telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());

    // then: the telemetry has been marked in progress
    assertThat(telemetryState.getStatus()).isEqualTo(Status.IN_PROGRESS.name());
    assertThat(telemetryState.getStartTime()).isAfterOrEqualTo(testStartTime);
    assertThat(telemetryState.getLastUpdated()).isAfterOrEqualTo(testStartTime);
  }

  @Test
  public void testPush_andDone() {
    // given: historical telemetry setup for batches of 2 and some data records to push telemetry for
    final var cutoffDate = new Date();
    final var batchSize = 2;
    final var minFreeMemoryMb = 0;

    tempEntity.newHistoricalTelemetryState(TEST_PURPOSE.name(), cutoffDate, batchSize, minFreeMemoryMb,
        Status.PENDING.name());

    final var record1 = new TestData(new Date(currentTimeMillis() - 10_000), "testId1");
    final var record2 = new TestData(new Date(currentTimeMillis()), "testId2");
    final var record3 = new TestData(new Date(currentTimeMillis() + 10_000), "testId3");

    // verify initial state
    assertThat(historicalTelemetryService.canCollectAndSendTelemetry()).isTrue();
    historicalTelemetryService.initialize();
    assertThat(historicalTelemetryService.getTotalRecordsSent()).isZero();

    // when: initialize and push one record
    historicalTelemetryService.push(new TelemetryData(TEST_PURPOSE, currentTimeMillis()), record1.date(), record1.id());

    // then: no telemetry sent yet and state set to in progress
    assertThat(historicalTelemetryService.getTotalRecordsSent()).isZero();
    verify(mockTelemetrySender, never()).send(any(List.class));
    var telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());
    assertThat(telemetryState.getStatus()).isEqualTo(Status.IN_PROGRESS.name());
    assertThat(telemetryState.getLastRecordTime()).isNull();
    assertThat(telemetryState.getLastRecordKey()).isNull();

    // when: push another record
    historicalTelemetryService.push(new TelemetryData(TEST_PURPOSE, currentTimeMillis()), record2.date(), record2.id());

    // then: telemetry sent and state updated
    assertThat(historicalTelemetryService.getTotalRecordsSent()).isEqualTo(2);
    verify(mockTelemetrySender, times(1)).send(any(List.class));
    telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());
    assertThat(telemetryState.getStatus()).isEqualTo(Status.IN_PROGRESS.name());
    assertThat(telemetryState.getLastRecordTime()).isEqualTo(record2.date());
    assertThat(telemetryState.getLastRecordKey()).isEqualTo(record2.id());

    // when: push another record
    historicalTelemetryService.push(new TelemetryData(TEST_PURPOSE, currentTimeMillis()), record3.date(), record3.id());

    // then: did not send telemetry as batch size not reached
    assertThat(historicalTelemetryService.getTotalRecordsSent()).isEqualTo(2);
    verify(mockTelemetrySender, times(1)).send(any(List.class));
    telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());
    assertThat(telemetryState.getStatus()).isEqualTo(Status.IN_PROGRESS.name());
    assertThat(telemetryState.getLastRecordTime()).isEqualTo(record2.date());
    assertThat(telemetryState.getLastRecordKey()).isEqualTo(record2.id());

    // when: done
    historicalTelemetryService.done();

    // then: previous record sent and state updated
    assertThat(historicalTelemetryService.getTotalRecordsSent()).isEqualTo(3);
    verify(mockTelemetrySender, times(2)).send(any(List.class));
    telemetryState = historicalTelemetryStateDAO.getById(TEST_PURPOSE.name());
    assertThat(telemetryState.getStatus()).isEqualTo(Status.DONE.name());
  }

  private class TestableHistoricalTelemetryService
      extends HistoricalTelemetryService
  {
    public TestableHistoricalTelemetryService(
        HistoricalTelemetryStateDAO historicalTelemetryStateDAO,
        TelemetryPurpose telemetryPurpose,
        TelemetrySender telemetrySender,
        int batchSize,
        Date cutoffDate)
    {
      super(historicalTelemetryStateDAO, telemetryPurpose, telemetrySender, batchSize, cutoffDate);
    }
  }

  private record TestData(Date date, String id)
  {
  }
}
