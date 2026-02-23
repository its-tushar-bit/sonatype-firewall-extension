/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.client.utils.RateLimitMetricsListener;
import com.sonatype.insight.client.utils.RateLimitRecorder;

import io.dropwizard.lifecycle.Managed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Named
@Singleton
public class ScmRateLimitMetrics
    implements RateLimitMetricsListener, Managed
{
  private static final Logger log = getLogger(ScmRateLimitMetrics.class);

  private static final String CALLS_METRIC = "scm.rate.limit.calls";

  private static final String REMAINING_METRIC = "scm.rate.limit.remaining";

  private static final String EXCEEDED_METRIC = "scm.rate.limit.exceeded";

  private static final String CLIENT_ID_TAG = "client_id";

  private static final String USER_ID_TAG = "user_id";

  private static final String TENANT_ID_TAG = "tenant_id";

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

    Tags tags = buildTags(clientId, userId);

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

    Tags tags = buildTags(clientId, userId);

    Counter.builder(EXCEEDED_METRIC)
        .tags(tags)
        .register(meterRegistry)
        .increment();
  }

  private Tags buildTags(final String clientId, final String userId) {
    Tenant tenant = TenantThreadLocal.getTenant();
    return Tags.of(CLIENT_ID_TAG, clientId, USER_ID_TAG, userId, TENANT_ID_TAG, tenant.tenantSlug);
  }
}
