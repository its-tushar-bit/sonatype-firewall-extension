/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.sonatype.insight.brain.git.ScmMetricsTags.buildTagsWithTenantId;
import static org.slf4j.LoggerFactory.getLogger;

import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.client.utils.ApiMetricsListener;
import com.sonatype.insight.client.utils.ApiMetricsRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import com.sonatype.insight.brain.lifecycle.Managed;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

@Named
@Singleton
public class ScmApiMetrics
    implements ApiMetricsListener, Managed
{
  private static final Logger log = getLogger(ScmApiMetrics.class);

  private static final String CALLS_METRIC = "scm.api.calls";

  private static final String AUTH_FAILURES_METRIC = "scm.api.auth.failures";

  private final MeterRegistry meterRegistry;

  private final TenantUtil tenantUtil = new TenantUtil();

  @Inject
  public ScmApiMetrics(@Nullable final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void start() {
    if (tenantUtil.isSingleTenant()) {
      log.debug("SCM API metrics disabled in single-tenant mode");
      return;
    }

    ApiMetricsRecorder.registerMetricsListener(this);
  }

  @Override
  public void stop() {
    ApiMetricsRecorder.registerMetricsListener(null);
  }

  @Override
  public void onApiCall(final String clientId, final String userId) {
    if (meterRegistry == null) {
      return;
    }

    Counter.builder(CALLS_METRIC)
        .tags(buildTagsWithTenantId(clientId, userId))
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void onApiAuthFailure(final String clientId, final String userId) {
    if (meterRegistry == null) {
      return;
    }

    Counter.builder(AUTH_FAILURES_METRIC)
        .tags(buildTagsWithTenantId(clientId, userId))
        .register(meterRegistry)
        .increment();
  }
}
