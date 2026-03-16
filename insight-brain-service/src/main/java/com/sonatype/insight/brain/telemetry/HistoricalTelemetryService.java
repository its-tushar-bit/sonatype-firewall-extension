/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.brain.model.telemetry.HistoricalTelemetryState;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to handle the boilerplate processing of historical telemetry data. Depending on
 * the telemetry data in question, this can be rather memory intensive. To mitigate possible memory issues we do
 * a couple of things:
 * - we check the available memory before starting to collect and send telemetry
 * - we expect data to be pushed to us rather than consuming the entire dataset in a single operation
 * - we stream the result set in hopes of making memory usage as efficient as possible
 * - if we reach a preset memory limit while processing a batch, we suspend the processing
 */
public abstract class HistoricalTelemetryService
{
  private static final Logger log = LoggerFactory.getLogger(HistoricalTelemetryService.class);

  private static final long BYTES_PER_MB = 1024L * 1024L;

  @VisibleForTesting
  enum Status
  {
    PENDING, // ok - initial state
    IN_PROGRESS, // ok - successfully started processing of historical telemetry
    DONE, // ok, terminal - successfully completed processing of historical telemetry
    ERROR, // terminal - an error occurred while processing historical telemetry - we won't try again
    SKIPPED, // ok - an error occurred while in the pending state - we can try again next time
    SUSPENDED // terminal - hit a memory limit while processing historical telemetry - we won't try again
  }

  private final TenantReference<HistoricalTelemetryState> historicalTelemetryState = new TenantReference<>();

  private final int batchSize;

  private final Date cutoffDate;

  private final HistoricalTelemetryStateDAO historicalTelemetryStateDAO;

  private final TenantReference<TelemetryAccumulator> telemetryAccumulator = new TenantReference<>();

  private final TelemetryPurpose telemetryPurpose;

  private final TelemetrySender telemetrySender;

  private final TenantReference<AtomicLong> totalRecordsSent = new TenantReference<>();

  protected HistoricalTelemetryService(
      HistoricalTelemetryStateDAO historicalTelemetryStateDAO,
      TelemetryPurpose telemetryPurpose,
      TelemetrySender telemetrySender,
      int batchSize,
      Date cutoffDate)
  {
    this.historicalTelemetryStateDAO = historicalTelemetryStateDAO;
    this.telemetryPurpose = telemetryPurpose;
    this.telemetrySender = telemetrySender;
    this.batchSize = batchSize;
    this.cutoffDate = cutoffDate;
  }

  public static long getFreeMemoryMb() {
    return Runtime.getRuntime().freeMemory() / BYTES_PER_MB;
  }

  /**
   * determines if we can collect and send telemetry by checking the current state of the historical telemetry
   * collection and the available memory
   *
   * @return true if collection hasn't arleady been started and there is sufficient memory, false otherwise
   */
  public boolean canCollectAndSendTelemetry() {
    boolean canTry = false;
    if (fetchHistoricalTelemetryStateAndCheckStatus()) {
      try {
        checkMemory();
        canTry = true;
      }
      catch (Exception e) {
        onError(e);
      }
    }
    return canTry;
  }

  protected void checkMemory() throws InsufficientMemoryException {
    long freeMemory = getFreeMemoryMb();
    if (freeMemory < historicalTelemetryState.get().getMinFreeMemoryMb()) {
      String message =
          String.format(
              "Insufficient free memory to continue processing %s telemetry, %dMB required, %dMB available",
              telemetryPurpose.name(),
              historicalTelemetryState.get().getMinFreeMemoryMb(),
              freeMemory);
      log.warn(message);
      throw new InsufficientMemoryException(message);
    }
  }

  protected long done() {
    return push(null, null, null);
  }

  protected int getBatchSize() {
    return historicalTelemetryState.get().getBatchSize();
  }

  protected Date getCutoffDate() {
    return historicalTelemetryState.get().getCutoffDate();
  }

  protected long getTotalRecordsSent() {
    return totalRecordsSent.get().get();
  }

  protected void onError(Exception e) {
    if (e instanceof InsufficientMemoryException) {
      Status status = Status.valueOf(historicalTelemetryState.get().getStatus());
      switch (status) {
        case PENDING, SKIPPED -> markSkipped();
        case IN_PROGRESS -> markSuspended();
        default -> markError(e);
      }
    }
    else {
      markError(e);
    }
  }

  protected void initialize() {
    int batchSize = getBatchSize();

    markStarted(batchSize);

    telemetryAccumulator.set(new TelemetryAccumulator(telemetryPurpose, telemetrySender, batchSize));

    totalRecordsSent.set(new AtomicLong(0));
  }

  protected long push(TelemetryData telemetryData, Date lastRecordTime, String lastRecordKey) {
    try {
      if (null != telemetryData) {
        int sendCount = accumulateTelemetry(telemetryData);

        if (sendCount > 0) {
          // a batch of telemetry data was sent
          addRecordsSent(sendCount);
          if (null != lastRecordTime && null != lastRecordKey) {
            updateProgress(lastRecordTime, lastRecordKey, getTotalRecordsSent());
          }
        }
      }
      else {
        int sendCount = flushTelemetry();
        addRecordsSent(sendCount);
        markComplete(getTotalRecordsSent());
      }
    }
    catch (Exception e) {
      markError(e);
    }

    return getTotalRecordsSent();
  }

  public boolean isTelemetryCollectionComplete() {
    HistoricalTelemetryState telemetryState = historicalTelemetryStateDAO.getById(telemetryPurpose.name());
    if (null == telemetryState) {
      return false;
    }

    String status = telemetryState.getStatus();
    return Status.DONE.name().equals(status);
  }

  private boolean fetchHistoricalTelemetryStateAndCheckStatus() {
    HistoricalTelemetryState telemetryState = historicalTelemetryStateDAO.getById(telemetryPurpose.name());
    if (null == telemetryState) {
      telemetryState = new HistoricalTelemetryState();
      telemetryState.setId(telemetryPurpose.name());
      telemetryState.setCreated(new Date());
      telemetryState.setStatus(Status.PENDING.name());
      telemetryState.setCutoffDate(cutoffDate);
      telemetryState.setBatchSize(batchSize);
      historicalTelemetryStateDAO.insert(telemetryState);
      telemetryState = historicalTelemetryStateDAO.getById(telemetryPurpose.name());
    }
    historicalTelemetryState.set(telemetryState);

    boolean okToTry = false;

    String status = telemetryState.getStatus();
    switch (Status.valueOf(status)) {
      case PENDING, SKIPPED -> okToTry = true;
      case IN_PROGRESS -> log.info("{} telemetry collection already in progress", telemetryPurpose.name());
      case DONE -> log.info("{} telemetry already collected and sent", telemetryPurpose.name());
      case ERROR -> log.warn("{} telemetry collection previously failed, skipping", telemetryPurpose.name());
      case SUSPENDED -> log.warn("{} telemetry collection previously suspended, skipping", telemetryPurpose.name());
    }

    return okToTry;
  }

  private int accumulateTelemetry(TelemetryData telemetryData) {
    return telemetryAccumulator.get().add(telemetryData);
  }

  private void addRecordsSent(long count) {
    totalRecordsSent.get().addAndGet(count);
  }

  private int flushTelemetry() {
    return telemetryAccumulator.get().flush();
  }

  private void markComplete(long recordsSent) {
    historicalTelemetryState.get().setStatus(Status.DONE.name());
    historicalTelemetryState.get().setLastUpdated(new Date());
    historicalTelemetryStateDAO.update(historicalTelemetryState.get());
    log.debug("{} telemetry collection complete, {} records sent", telemetryPurpose.name(), recordsSent);
  }

  private void markError(Exception e) {
    historicalTelemetryState.get().setStatus(Status.ERROR.name());
    historicalTelemetryState.get().setLastUpdated(new Date());
    historicalTelemetryStateDAO.update(historicalTelemetryState.get());
    log.error("Failed to send {} telemetry: {}", telemetryPurpose.name(), e.getMessage(), e);
  }

  private void updateProgress(
      Date lastRecordTime,
      String lastRecordKey,
      long sendCount)
  {
    historicalTelemetryState.get().setStatus(Status.IN_PROGRESS.name());
    historicalTelemetryState.get().setLastRecordTime(lastRecordTime);
    historicalTelemetryState.get().setLastRecordKey(lastRecordKey);
    historicalTelemetryState.get().setLastUpdated(new Date());
    historicalTelemetryStateDAO.update(historicalTelemetryState.get());
    log.trace("Sent {} {} telemetry entries up to {}.  Free memory = {} MB",
        sendCount,
        telemetryPurpose.name(),
        lastRecordTime,
        getFreeMemoryMb());
  }

  protected void markSkipped() {
    historicalTelemetryState.get().setStatus(Status.SKIPPED.name());
    historicalTelemetryState.get().setLastUpdated(new Date());
    historicalTelemetryStateDAO.update(historicalTelemetryState.get());
    log.warn("{} telemetry collection skipped", telemetryPurpose.name());
  }

  private void markStarted(int batchSize) {
    log.debug("Commencing {} telemetry collection, with cutoff date {}, batchSize: {}, free memory = {} MB",
        telemetryPurpose.name(), cutoffDate, batchSize, getFreeMemoryMb());

    Date now = new Date();
    historicalTelemetryState.get().setStartTime(now);
    historicalTelemetryState.get().setLastUpdated(now);
    historicalTelemetryState.get().setStatus(Status.IN_PROGRESS.name());
    historicalTelemetryStateDAO.update(historicalTelemetryState.get());
  }

  private void markSuspended() {
    historicalTelemetryState.get().setStatus(Status.SUSPENDED.name());
    historicalTelemetryState.get().setLastUpdated(new Date());
    historicalTelemetryStateDAO.update(historicalTelemetryState.get());
    log.warn("{} telemetry collection suspended", telemetryPurpose.name());
  }
}
