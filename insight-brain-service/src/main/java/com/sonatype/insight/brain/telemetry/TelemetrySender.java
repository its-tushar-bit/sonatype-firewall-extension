/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static com.sonatype.insight.brain.common.config.ConfigUtil.getBooleanConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Value;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

/**
 * @since 1.43.0
 */
@Named
@Singleton
public class TelemetrySender
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(TelemetrySender.class);

  private final HdsClient hdsClient;

  private final TelemetryReceiptService telemetryReceiptService;

  private final VersionService versionService;

  private final TelemetryId telemetryId;

  private final TenantUtil tenantUtil;

  @VisibleForTesting
  static final int DEFAULT_MAX_QUEUE_SIZE = 2_000;

  @VisibleForTesting
  static final long DEFAULT_DRAIN_TIMEOUT_MS = 30_000L;

  // Defensive upper bound to keep the deadline arithmetic in drainQueue() from overflowing if an operator misconfigures
  // the property. 24h is well above any sane shutdown budget while leaving the deadline well clear of Long.MAX_VALUE.
  private static final long MAX_DRAIN_TIMEOUT_MS = 24L * 60L * 60L * 1000L;

  // The depth at which the queue-depth warning starts firing; derived from maxQueueSize so that operators tuning
  // the cap up or down still get the depth signal at a meaningful fraction (50%) of capacity. With a hardcoded
  // value, a maxQueueSize below the threshold would silently disable the warning.
  private final int queueDepthWarnThreshold;

  // Repeating warnings (queue depth, drops) back off exponentially so a sustained problem warns immediately and then
  // progressively less often, capping out instead of flooding the log every interval.
  private static final Duration WARN_BACKOFF_INITIAL_INTERVAL = Duration.ofMinutes(1);

  private static final Duration WARN_BACKOFF_MAX_INTERVAL = Duration.ofHours(1);

  private final int maxQueueSize;

  private final BlockingQueue<TenantAwareOneTimeRunnable> submissions;

  private final AtomicLong droppedCount = new AtomicLong();

  private final ExponentialBackoffThrottle dropWarnThrottle = new ExponentialBackoffThrottle();

  private final ExponentialBackoffThrottle queueDepthWarnThrottle = new ExponentialBackoffThrottle();

  // Per-purpose counts logged to point at the noisiest producer. The queue-depth warning logs both:
  // - acceptedByPurposeEpisode: cleared on the cross-up edge so it attributes the current spike to the producer
  // active right now, not one that was noisy weeks ago on a long-running server;
  // - acceptedByPurposeTotal: never cleared, kept for an overall impression of telemetry volume since startup.
  // droppedByPurpose is cleared by send() on the recovery edge (first successful offer after drops).
  private final Map<TelemetryPurpose, LongAdder> acceptedByPurposeEpisode = new ConcurrentHashMap<>();

  private final Map<TelemetryPurpose, LongAdder> acceptedByPurposeTotal = new ConcurrentHashMap<>();

  private final Map<TelemetryPurpose, LongAdder> droppedByPurpose = new ConcurrentHashMap<>();

  private TelemetrySubmitter submitter;

  private static final String MULTIPART_FILE_NAME = "file";

  public static final String PRODUCT_PREFIX = "nexus-iq";

  public static final String FILE_FORMAT = "zip-bundle/1";

  public static final String HEADER_ENTRY_NAME = "header.json";

  public static final String DATA_ENTRY_NAME = "data.json";

  public static final String RESOURCE_PATH = "rest/environment/stats";

  public static final String ZIP_FILENAME = "telemetry.zip";

  private static final Duration SUBMITTER_JOIN_TIMEOUT = Duration.ofSeconds(5);

  private final long drainTimeoutMs;

  @Inject
  public TelemetrySender(
      HdsClient hdsClient,
      VersionService versionService,
      TelemetryId telemetryId,
      TenantUtil tenantUtil,
      TelemetryReceiptService telemetryReceiptService,
      @Value("${nxiq.telemetry.queue.maxSize:" + DEFAULT_MAX_QUEUE_SIZE + "}") int maxQueueSize,
      @Value("${nxiq.telemetry.drain.timeoutMillis:" + DEFAULT_DRAIN_TIMEOUT_MS + "}") long drainTimeoutMillis)
  {
    Preconditions.checkArgument(maxQueueSize > 0,
        "nxiq.telemetry.queue.maxSize must be greater than 0 but was %s", maxQueueSize);
    Preconditions.checkArgument(drainTimeoutMillis > 0 && drainTimeoutMillis <= MAX_DRAIN_TIMEOUT_MS,
        "nxiq.telemetry.drain.timeoutMillis must be in (0, %s] but was %s", MAX_DRAIN_TIMEOUT_MS, drainTimeoutMillis);
    this.hdsClient = hdsClient;
    this.versionService = versionService;
    this.telemetryId = telemetryId;
    this.tenantUtil = tenantUtil;
    this.telemetryReceiptService = telemetryReceiptService;
    this.maxQueueSize = maxQueueSize;
    this.queueDepthWarnThreshold = maxQueueSize / 2;
    this.submissions = new LinkedBlockingQueue<>(maxQueueSize);
    this.drainTimeoutMs = drainTimeoutMillis;
  }

  @Override
  public void start() {
    if (submitter == null) {
      submitter = new TelemetrySubmitter();
      submitter.start();
    }
  }

  @Override
  public void stop() {
    if (submitter != null) {
      submitter.interrupt();
      try {
        submitter.join(SUBMITTER_JOIN_TIMEOUT.toMillis());
      }
      catch (InterruptedException e) {
        log.warn("Interrupted while waiting for telemetry submitter to stop.", e);
        Thread.currentThread().interrupt();
      }
      drainQueue();
      submitter = null;
    }
  }

  @VisibleForTesting
  void drainQueue() {
    List<TenantAwareOneTimeRunnable> remaining = new ArrayList<>();
    submissions.drainTo(remaining);
    long deadline = System.currentTimeMillis() + drainTimeoutMs;
    int processed = 0;
    for (TenantAwareOneTimeRunnable task : remaining) {
      // Deadline gates whether we *start* another task; a single in-flight task (e.g. a slow HDS call) can still
      // run past it. HdsClient's HTTP socket timeout bounds that, so this is not a hard wall-clock shutdown cap.
      if (System.currentTimeMillis() > deadline) {
        log.warn("Telemetry drain timed out; {} of {} queued items were not sent.",
            remaining.size() - processed, remaining.size());
        break;
      }
      try {
        tenantUtil.validateNoCustomerTenantSet();
        task.run();
        processed++;
      }
      catch (Exception e) {
        log.debug("Failed to send telemetry during shutdown.", e);
      }
      catch (Throwable t) {
        log.error("Unexpected error sending telemetry during shutdown.", t);
      }
    }
  }

  public void send(TelemetryData telemetryData) {
    send(telemetryData, null /* clientUserAgent */);
  }

  public void send(List<TelemetryData> telemetryData) {
    send(telemetryData, null /* clientUserAgent */);
  }

  public void send(TelemetryData telemetryData, String clientUserAgent) {
    send(Collections.singletonList(telemetryData), clientUserAgent);
  }

  public void send(List<TelemetryData> telemetryData, String clientUserAgent) {
    if (telemetryData.isEmpty()) {
      return;
    }
    // Fully short-circuit when outbound telemetry is disabled: skip serialization, queuing, and the associated
    // drop/depth logging. HdsClient also fakes a 200 for telemetry URLs, but stopping here avoids all local work,
    // which matters when the queue would otherwise back up.
    if (isOutboundTelemetryDisabled()) {
      return;
    }
    // Best-effort early bail to avoid expensive zip serialization when the queue is already full. This is not an
    // atomic gate: capacity can change between this check and the offer() below (which is the authoritative check),
    // so a successful offer() is still possible after this returns false, and vice versa.
    if (submissions.remainingCapacity() == 0) {
      logDrop(telemetryData);
      return;
    }
    try {
      var telemetrySubmission = new TelemetrySubmission(createZip(createHeader(), telemetryData), clientUserAgent);
      // Capture the enqueue timestamp here so the receipt's submit time (created later, on the submitter thread)
      // reflects when the item entered the queue, keeping the queue-wait diagnostic accurate. The receipt itself is
      // created on the single submitter thread so the receipt list's add() has only one writer.
      long enqueueTimeMs = System.currentTimeMillis();
      boolean queued = submissions.offer(new TenantAwareOneTimeRunnable(
          () -> submitTelemetry(telemetrySubmission, telemetryData, enqueueTimeMs)));
      if (queued) {
        countByPurpose(acceptedByPurposeEpisode, telemetryData);
        countByPurpose(acceptedByPurposeTotal, telemetryData);
        long previouslyDropped = droppedCount.getAndSet(0);
        if (previouslyDropped > 0) {
          // Recovered: clear the per-episode drop counts and reset the backoff so the next backpressure episode
          // reports and warns fresh (keeps droppedByPurpose consistent with droppedCount in the drop warning).
          // Best-effort: the getAndSet and clear() are not atomic together, so a drop racing in between can have its
          // per-purpose entry wiped here (droppedCount stays accurate); that under-counts one purpose at the episode
          // boundary, which is acceptable for a diagnostic counter.
          droppedByPurpose.clear();
          dropWarnThrottle.reset();
          log.info("Telemetry queue accepting entries again; {} entries were dropped during backpressure.",
              previouslyDropped);
        }
      }
      else {
        logDrop(telemetryData);
      }
    }
    catch (Exception e) {
      log.warn("Failed to send telemetry.", e);
    }
  }

  private static boolean isOutboundTelemetryDisabled() {
    return getBooleanConfig(HdsClient.DISABLE_TELEMETRY_CONFIG_KEY, false);
  }

  private void logDrop(List<TelemetryData> telemetryData) {
    countByPurpose(droppedByPurpose, telemetryData);
    long dropped = droppedCount.incrementAndGet();
    if (dropWarnThrottle.shouldLog(System.currentTimeMillis())) {
      log.warn("Telemetry queue full ({} capacity), {} total entries dropped, dropped by purpose: [{}]. "
          + "The telemetry submitter may be blocked or unable to keep up with telemetry volume.",
          maxQueueSize, dropped, formatCounts(droppedByPurpose));
    }
  }

  private static void countByPurpose(Map<TelemetryPurpose, LongAdder> counters, List<TelemetryData> telemetryData) {
    for (TelemetryData data : telemetryData) {
      if (data.getPurpose() != null) {
        counters.computeIfAbsent(data.getPurpose(), purpose -> new LongAdder()).increment();
      }
    }
  }

  /**
   * Renders counters as a "PURPOSE=count" list ordered by count descending. The enum is bounded so the list stays
   * small.
   */
  private static String formatCounts(Map<TelemetryPurpose, LongAdder> counters) {
    return counters.entrySet()
        .stream()
        .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
        .map(entry -> entry.getKey() + "=" + entry.getValue().sum())
        .collect(Collectors.joining(", "));
  }

  /**
   * Time-based throttle for a repeating warning. {@link #shouldLog} returns true on the first call and then at
   * exponentially increasing intervals ({@link #WARN_BACKOFF_INITIAL_INTERVAL}, then doubling up to
   * {@link #WARN_BACKOFF_MAX_INTERVAL}), so a sustained problem warns immediately and then progressively less often.
   * {@link #reset} restores immediate logging once the condition clears. CAS-guarded so concurrent callers emit at
   * most one line per step.
   */
  @VisibleForTesting
  static final class ExponentialBackoffThrottle
  {
    private record Schedule(long nextAllowedMs, long intervalMs)
    {
    }

    private static final Schedule INITIAL = new Schedule(0L, WARN_BACKOFF_INITIAL_INTERVAL.toMillis());

    private final AtomicReference<Schedule> schedule = new AtomicReference<>(INITIAL);

    boolean shouldLog(long nowMs) {
      Schedule current = schedule.get();
      if (nowMs < current.nextAllowedMs()) {
        return false;
      }
      long nextInterval = Math.min(current.intervalMs() * 2, WARN_BACKOFF_MAX_INTERVAL.toMillis());
      return schedule.compareAndSet(current, new Schedule(nowMs + current.intervalMs(), nextInterval));
    }

    void reset() {
      schedule.set(INITIAL);
    }
  }

  private TelemetryHeader createHeader() {
    String product = PRODUCT_PREFIX + "/" + versionService.getVersion();
    String build = versionService.getBuild();
    Date createTime = new Date();
    return new TelemetryHeader(FILE_FORMAT, product, createTime, telemetryId.getId(),
        telemetryId.getClusterId(), build);
  }

  private byte[] createZip(TelemetryHeader telemetryHeader, List<TelemetryData> telemetryData) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipOutput = new ZipOutputStream(
            bos))
    {
      ZipEntry zipEntryHeader = new ZipEntry(HEADER_ENTRY_NAME);
      zipOutput.putNextEntry(zipEntryHeader);
      zipOutput.write(JsonUtils.generate(telemetryHeader));
      ZipEntry zipEntryData = new ZipEntry(DATA_ENTRY_NAME);
      zipOutput.putNextEntry(zipEntryData);
      zipOutput.write(JsonUtils.generate(telemetryData));
      zipOutput.finish();
      return bos.toByteArray();
    }
  }

  static class TelemetrySubmission
  {
    final byte[] zipData;

    final String clientUserAgent;

    TelemetrySubmission(byte[] zipData, String clientUserAgent) {
      this.zipData = zipData;
      this.clientUserAgent = clientUserAgent;
    }
  }

  class TelemetrySubmitter
      extends Thread
  {
    TelemetrySubmitter() {
      setName(getClass().getSimpleName());
      setDaemon(true);
    }

    // Submitter-thread-only state for edge-triggered episode reset; not accessed from any other thread.
    private boolean wasAboveDepthThreshold = false;

    @Override
    public void run() {
      while (true) {
        try {
          // Verify this thread should always run as `global` tenant for MTIQ and `single` for on-premise
          tenantUtil.validateNoCustomerTenantSet();

          TenantAwareOneTimeRunnable tenantAwareOneTimeRunnable = submissions.take();
          tenantAwareOneTimeRunnable.run();

          int queueDepth = submissions.size();
          boolean nowAboveThreshold = queueDepth > queueDepthWarnThreshold;
          if (nowAboveThreshold && !wasAboveDepthThreshold) {
            // Cross-up: starting a new backpressure episode. Clear the episode counter so the warning attributes
            // this spike to the producer active now; the cumulative counter is left untouched.
            acceptedByPurposeEpisode.clear();
          }
          if (nowAboveThreshold) {
            if (queueDepthWarnThrottle.shouldLog(System.currentTimeMillis())) {
              log.warn("Telemetry queue depth is {}, submitter may not be keeping up with telemetry volume. "
                  + "Submitted by purpose during current backpressure episode: [{}]. "
                  + "Submitted by purpose since startup: [{}].",
                  queueDepth, formatCounts(acceptedByPurposeEpisode), formatCounts(acceptedByPurposeTotal));
            }
          }
          else if (wasAboveDepthThreshold) {
            // Cross-down: episode ended. Reset the throttle so the next episode warns immediately.
            queueDepthWarnThrottle.reset();
          }
          wasAboveDepthThreshold = nowAboveThreshold;
        }
        catch (InterruptedException e) {
          // interrupt is our signal to quit
          return;
        }
        catch (Exception e) {
          log.debug("Failed to send telemetry.", e);
        }
        catch (Throwable t) {
          // Try to log to stderr before trying the standard logging because the standard logging may not be operational
          // at this point.
          t.printStackTrace();
          log.error(t.getMessage(), t);
          System.exit(2);
        }
      }
    }
  }

  private void submitTelemetry(
      final TelemetrySubmission telemetrySubmission,
      final List<TelemetryData> telemetryData,
      final long enqueueTimeMs)
  {
    var telemetryReceipt = telemetryReceiptService.onTelemetrySubmitted(telemetryData, enqueueTimeMs);
    try {
      telemetryReceipt.markSending();
      ContentBody fileBody = new ByteArrayBody(telemetrySubmission.zipData, ZIP_FILENAME);
      HttpEntity httpEntity = MultipartEntityBuilder.create().addPart(MULTIPART_FILE_NAME, fileBody).build();
      hdsClient.post(RESOURCE_PATH, httpEntity, telemetrySubmission.clientUserAgent);
      telemetryReceipt.markSent();
    }
    catch (Exception e) {
      telemetryReceipt.markInError(e);
      log.error("Failed to send telemetry.", e);
      throw (e);
    }
  }
}
