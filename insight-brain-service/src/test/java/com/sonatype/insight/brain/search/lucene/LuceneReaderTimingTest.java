/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class LuceneReaderTimingTest
{
  @Test
  public void record_separatesAcquisitionFromExecution() {
    LuceneReaderTiming.reset();
    LuceneReaderTiming.startAcquisition();
    LuceneReaderTiming.endAcquisition();
    LuceneReaderTiming.startExecution();
    LuceneReaderTiming.TimingState timing = LuceneReaderTiming.endExecution();
    assertThat(timing.lastAcquisitionNanos()).isGreaterThan(0);
    assertThat(timing.lastExecutionNanos()).isGreaterThanOrEqualTo(0);
    assertThat(timing.lastAcquisitionNanos())
        .isNotEqualTo(timing.lastExecutionNanos());
    assertThat(LuceneReaderTiming.lastAcquisitionNanos()).isZero();
  }

  @Test
  public void record_separatesSearchFromGroupDocumentsAndCountsLoads() {
    LuceneReaderTiming.reset();
    LuceneReaderTiming.startExecution();
    LuceneReaderTiming.startSearch();
    LuceneReaderTiming.endSearch(100, 40, 40L);
    LuceneReaderTiming.startGroupDocuments();
    LuceneReaderTiming.recordStoredFieldDocumentLoad();
    LuceneReaderTiming.recordStoredFieldDocumentLoad();
    LuceneReaderTiming.endGroupDocuments();
    LuceneReaderTiming.TimingState timing = LuceneReaderTiming.endExecution();
    assertThat(timing.lastSearchNanos()).isGreaterThanOrEqualTo(0);
    assertThat(timing.lastGroupNanos()).isGreaterThanOrEqualTo(0);
    assertThat(timing.storedFieldDocumentLoads()).isEqualTo(2);
  }

  @Test
  public void record_separatesQueryBuildFromParse() {
    LuceneReaderTiming.reset();
    LuceneReaderTiming.startExecution();
    LuceneReaderTiming.startQueryBuild();
    LuceneReaderTiming.endQueryBuild("applicationId:a OR applicationId:b AND (itemType:APPLICATION)", 2);
    LuceneReaderTiming.startQueryParse();
    LuceneReaderTiming.endQueryParse();
    LuceneReaderTiming.startSearch();
    LuceneReaderTiming.endSearch(1, 1, 1L);
    LuceneReaderTiming.TimingState timing = LuceneReaderTiming.endExecution();
    assertThat(timing.lastQueryBuildNanos()).isGreaterThanOrEqualTo(0);
    assertThat(timing.lastQueryParseNanos()).isGreaterThanOrEqualTo(0);
    assertThat(timing.lastFinalQueryChars()).isGreaterThan(0);
    assertThat(timing.lastFinalQueryOrClauses()).isEqualTo(1);
    assertThat(timing.lastRbacContextCount()).isEqualTo(2);
  }

  @Test
  public void startAcquisition_clearsPreviousRequestPhaseDataAndLoadCount() {
    LuceneReaderTiming.reset();
    LuceneReaderTiming.startSearch();
    LuceneReaderTiming.endSearch(100, 40, 40L);
    LuceneReaderTiming.startGroupDocuments();
    LuceneReaderTiming.recordStoredFieldDocumentLoad();
    LuceneReaderTiming.endGroupDocuments();

    LuceneReaderTiming.startAcquisition();

    assertThat(LuceneReaderTiming.lastSearchNanos()).isZero();
    assertThat(LuceneReaderTiming.lastGroupNanos()).isZero();
    assertThat(LuceneReaderTiming.storedFieldDocumentLoads()).isZero();
  }

  @Test
  public void abort_clearsPartialRequestPhaseDataAndLoadCount() {
    LuceneReaderTiming.reset();
    LuceneReaderTiming.startSearch();
    LuceneReaderTiming.endSearch(100, 40, 40L);
    LuceneReaderTiming.startGroupDocuments();
    LuceneReaderTiming.recordStoredFieldDocumentLoad();

    LuceneReaderTiming.abort();

    assertThat(LuceneReaderTiming.lastSearchNanos()).isZero();
    assertThat(LuceneReaderTiming.lastGroupNanos()).isZero();
    assertThat(LuceneReaderTiming.storedFieldDocumentLoads()).isZero();
  }

  @Test
  public void endExecution_clearsThreadLocalState() {
    LuceneReaderTiming.reset();
    LuceneReaderTiming.startAcquisition();
    LuceneReaderTiming.endAcquisition();
    LuceneReaderTiming.startExecution();
    LuceneReaderTiming.endExecution();
    assertThat(LuceneReaderTiming.lastAcquisitionNanos()).isZero();
    assertThat(LuceneReaderTiming.lastExecutionNanos()).isZero();
  }

  @Test
  public void requests_doNotCrossContaminateWhenAnotherThreadAborts() throws Exception {
    CountDownLatch successfulRequestMeasured = new CountDownLatch(1);
    CountDownLatch abortedRequestCleanedUp = new CountDownLatch(1);
    AtomicReference<RequestMeasurements> successful = new AtomicReference<>();
    AtomicReference<RequestMeasurements> aborted = new AtomicReference<>();
    AtomicReference<Throwable> threadFailure = new AtomicReference<>();

    Thread successfulThread = new Thread(() -> {
      try {
        LuceneReaderTiming.startAcquisition();
        LuceneReaderTiming.endAcquisition();
        LuceneReaderTiming.startExecution();
        LuceneReaderTiming.startSearch();
        LockSupport.parkNanos(1_000_000L);
        LuceneReaderTiming.endSearch(100, 40, 40L);
        LuceneReaderTiming.startGroupDocuments();
        LuceneReaderTiming.recordStoredFieldDocumentLoad();
        LockSupport.parkNanos(1_000_000L);
        LuceneReaderTiming.endGroupDocuments();
        successfulRequestMeasured.countDown();
        abortedRequestCleanedUp.await();
        LuceneReaderTiming.TimingState timing = LuceneReaderTiming.endExecution();
        successful.set(new RequestMeasurements(
            timing.lastSearchNanos(), timing.lastGroupNanos(), timing.storedFieldDocumentLoads()));
      }
      catch (Throwable t) {
        threadFailure.compareAndSet(null, t);
      }
    }, "successful-timing-request");

    Thread abortedThread = new Thread(() -> {
      try {
        successfulRequestMeasured.await();
        LuceneReaderTiming.startAcquisition();
        LuceneReaderTiming.endAcquisition();
        LuceneReaderTiming.startExecution();
        LuceneReaderTiming.startSearch();
        LuceneReaderTiming.recordStoredFieldDocumentLoad();
        LuceneReaderTiming.recordStoredFieldDocumentLoad();
        throw new IllegalStateException("simulated request failure");
      }
      catch (IllegalStateException expected) {
        LuceneReaderTiming.abort();
        aborted.set(currentMeasurements());
      }
      catch (Throwable t) {
        threadFailure.compareAndSet(null, t);
      }
      finally {
        abortedRequestCleanedUp.countDown();
      }
    }, "aborted-timing-request");

    successfulThread.start();
    abortedThread.start();
    successfulThread.join();
    abortedThread.join();

    assertThat(threadFailure.get()).isNull();
    assertThat(successful.get().searchNanos()).isPositive();
    assertThat(successful.get().groupNanos()).isPositive();
    assertThat(successful.get().storedFieldLoads()).isEqualTo(1);
    assertThat(aborted.get()).isEqualTo(new RequestMeasurements(0, 0, 0));
  }

  private static RequestMeasurements currentMeasurements() {
    return new RequestMeasurements(
        LuceneReaderTiming.lastSearchNanos(),
        LuceneReaderTiming.lastGroupNanos(),
        LuceneReaderTiming.storedFieldDocumentLoads());
  }

  private record RequestMeasurements(long searchNanos, long groupNanos, int storedFieldLoads)
  {
  }
}
