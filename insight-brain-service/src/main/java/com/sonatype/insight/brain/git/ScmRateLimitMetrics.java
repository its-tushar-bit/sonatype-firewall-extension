/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.sonatype.insight.brain.git.ScmMetricsTags.buildTagsWithTenantId;
import static org.slf4j.LoggerFactory.getLogger;

import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.client.utils.RateLimitMetricsListener;
import com.sonatype.insight.client.utils.RateLimitRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nullable;
import com.sonatype.insight.brain.lifecycle.Managed;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

@Named
@Singleton
public class ScmRateLimitMetrics
    implements RateLimitMetricsListener, Managed
{
  private static final Logger log = getLogger(ScmRateLimitMetrics.class);

  private static final String CALLS_METRIC = "scm.rate.limit.calls";

  private static final String REMAINING_METRIC = "scm.rate.limit.remaining";

  private static final String EXCEEDED_METRIC = "scm.rate.limit.exceeded";

  private final MeterRegistry meterRegistry;

  private final TenantUtil tenantUtil = new TenantUtil();

  private final Map<Tags, AtomicInteger> remainingGauges = new ConcurrentHashMap<>();

  @Inject
  public ScmRateLimitMetrics(@Nullable final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void start() {
    if (tenantUtil.isSingleTenant()) {
      log.debug("SCM rate limit metrics disabled in single-tenant mode");
      return;
    }

    RateLimitRecorder.registerMetricsListener(this);
  }

  @Override
  public void stop() {
    RateLimitRecorder.registerMetricsListener(null);
  }

  @Override
  public void onRateLimitRemaining(final String clientId, final String userId, final int remaining) {
    if (meterRegistry == null) {
      return;
    }

    Tags tags = buildTagsWithTenantId(clientId, userId);

    Counter.builder(CALLS_METRIC)
        .tags(tags)
        .register(meterRegistry)
        .increment();

    remainingGauges.computeIfAbsent(tags, t -> {
      AtomicInteger remainingLimit = new AtomicInteger();
      meterRegistry.gauge(REMAINING_METRIC, t, remainingLimit);
      return remainingLimit;
    }).set(remaining);
  }

  @Override
  public void onRateLimitExceeded(final String clientId, final String userId) {
    if (meterRegistry == null) {
      return;
    }

    Tags tags = buildTagsWithTenantId(clientId, userId);

    Counter.builder(EXCEEDED_METRIC)
        .tags(tags)
        .register(meterRegistry)
        .increment();
  }
}
