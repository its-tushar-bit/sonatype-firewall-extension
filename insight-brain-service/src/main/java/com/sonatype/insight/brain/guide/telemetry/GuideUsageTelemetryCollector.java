/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffers per-call Guide usage events in a bounded, per-tenant queue and drains them into
 * {@code GUIDE_SELF_HOSTED_CREDIT_CONSUMPTION} telemetry.
 *
 * <p>
 * {@link #record} runs on request threads (invoked by the Guide usage-event aspect on each lookup),
 * while {@link #collectAllData} runs on the daily telemetry-scheduler thread. The per-tenant
 * {@link TenantReference} wrapping a {@link java.util.concurrent.ConcurrentLinkedQueue} supports many
 * concurrent producers with a single drainer &mdash; keep it a concurrent collection. Memory is bounded
 * by an early flush at {@link #FLUSH_THRESHOLD} events between daily drains.
 */
@Named
@Singleton
public class GuideUsageTelemetryCollector
    implements TelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(GuideUsageTelemetryCollector.class);

  static final int FLUSH_THRESHOLD = 10_000;

  private final TenantReference<Queue<GuideUsageEventRecord>> buffer =
      new TenantReference<>(ConcurrentLinkedQueue::new);

  private final TelemetryId telemetryId;

  private final CurrentUser currentUser;

  private final TelemetrySender telemetrySender;

  private final LongSupplier clock;

  @Inject
  public GuideUsageTelemetryCollector(
      final TelemetryId telemetryId,
      final CurrentUser currentUser,
      final TelemetrySender telemetrySender)
  {
    this(telemetryId, currentUser, telemetrySender, System::currentTimeMillis);
  }

  GuideUsageTelemetryCollector(
      final TelemetryId telemetryId,
      final CurrentUser currentUser,
      final TelemetrySender telemetrySender,
      final LongSupplier clock)
  {
    this.telemetryId = telemetryId;
    this.currentUser = currentUser;
    this.telemetrySender = telemetrySender;
    this.clock = clock;
  }

  /** Called by {@link GuideUsageEventAspect} on each annotated SearchApiClient invocation. */
  public void record(final GuideOperationType restOperationType, final Object[] args) {
    GuideChannel channel = GuideChannelContext.getOrDefault();
    String operationType =
        channel == GuideChannel.MCP ? GuideOperationType.MCP_LOOKUP.value() : restOperationType.value();
    String hashedUserId = hashUserId(currentUser.getUsernameOrSystem());
    String identifier = GuideUsageIdentifiers.extract(args);

    Queue<GuideUsageEventRecord> queue = buffer.get();
    queue.add(new GuideUsageEventRecord(
        clock.getAsLong(), operationType, restOperationType, channel, hashedUserId, identifier));
    // size() is O(n) on ConcurrentLinkedQueue, but acceptable: the queue is bounded by this early-flush
    // (<= FLUSH_THRESHOLD) and self-hosted Guide call volume is modest, so the cost is negligible next to
    // the HDS lookup each call performs.
    if (queue.size() >= FLUSH_THRESHOLD) {
      List<TelemetryData> batch = drain(queue);
      try {
        telemetrySender.send(batch);
      }
      catch (RuntimeException e) {
        // Telemetry must never break a user's Guide lookup: drop this batch and continue.
        log.warn("Failed to flush {} Guide usage telemetry events; dropping batch", batch.size(), e);
      }
    }
  }

  @Override
  public List<TelemetryData> collectAllData() {
    return drain(buffer.get());
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }

  private List<TelemetryData> drain(final Queue<GuideUsageEventRecord> queue) {
    List<TelemetryData> out = new ArrayList<>();
    GuideUsageEventRecord record;
    while ((record = queue.poll()) != null) {
      out.add(toTelemetryData(record));
    }
    return out;
  }

  private TelemetryData toTelemetryData(final GuideUsageEventRecord r) {
    TelemetryData data = new TelemetryData(TelemetryPurpose.GUIDE_SELF_HOSTED_CREDIT_CONSUMPTION, r.timestampMillis());
    Map<String, Object> attributes = new HashMap<>();
    // Attribute keys are snake_case to match the shared Athena telemetry struct, where every other
    // purpose's top-level attribute keys are snake_case (terraform-iq-hds-telemetry::main.tf).
    attributes.put("event_type", "CREDIT_CONSUMED_SELF_HOSTED");
    attributes.put("event_component", "BACKEND");
    attributes.put("operation_type", r.operationType());
    attributes.put("channel", r.channel().name());
    attributes.put("plan_tier", "SELF_HOSTED");
    attributes.put("user_id", r.hashedUserId());
    if (r.identifier() != null) {
      // Key off the original annotated operation, not r.operationType(): for MCP that field is
      // overridden to "mcp_lookup", which would mis-bucket a vulnerability id under "purl".
      attributes.put(r.restOperationType() == GuideOperationType.VULNERABILITY_LOOKUP
          ? "vulnerability_id"
          : "purl", r.identifier());
    }
    data.setAttributes(attributes);
    return data;
  }

  private String hashUserId(final String username) {
    return Hashing.sha256()
        .hashString(telemetryId.getId() + username, StandardCharsets.UTF_8)
        .toString();
  }
}
