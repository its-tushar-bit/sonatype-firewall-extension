/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class ScmOperationMetrics
{
  private static final String PR_COMMENT_DURATION = "scm.pr.comment.duration";

  private static final String PR_COMMENT_COMPLETED = "scm.pr.comment.completed";

  private static final String PR_COMMENT_FAILED = "scm.pr.comment.failed";

  private static final String PR_CREATE_DURATION = "scm.pr.create.duration";

  private static final String PR_CREATE_COMPLETED = "scm.pr.create.completed";

  private static final String PR_CREATE_FAILED = "scm.pr.create.failed";

  private static final String PR_CREATE_INELIGIBLE = "scm.pr.create.ineligible";

  private static final String KIND_TAG = "kind";

  private static final String METRIC_KIND = "scm_operation";

  private static final String TENANT_ID_TAG = "tenant_id";

  private static final String OPERATION_TAG = "operation";

  private static final String PROVIDER_TAG = "provider";

  private static final String REASON_TAG = "reason";

  private final MeterRegistry meterRegistry;

  private final TenantUtil tenantUtil = new TenantUtil();

  @Inject
  public ScmOperationMetrics(@Nullable final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public ScmTimerContext startPrCreationTimer(final String provider) {
    if (meterRegistry == null || tenantUtil.isSingleTenant()) {
      return null;
    }

    return new ScmTimerContext(Timer.start(meterRegistry), null, provider);
  }

  public void recordPrCreationCompleted(final ScmTimerContext context) {
    if (meterRegistry == null || tenantUtil.isSingleTenant() || context == null) {
      return;
    }

    Tags tags = buildProviderTags(context.provider());
    context.sample().stop(meterRegistry.timer(PR_CREATE_DURATION, tags));
    meterRegistry.counter(PR_CREATE_COMPLETED, tags).increment();
  }

  public void recordPrCreationFailed(final ScmTimerContext context) {
    if (meterRegistry == null || tenantUtil.isSingleTenant() || context == null) {
      return;
    }

    meterRegistry.counter(PR_CREATE_FAILED, buildProviderTags(context.provider())).increment();
  }

  public void recordPrCreationIneligible(final ScmPrIneligibleReason reason) {
    if (meterRegistry == null || tenantUtil.isSingleTenant()) {
      return;
    }

    Tags tags = baseTags().and(REASON_TAG, reason.value());
    meterRegistry.counter(PR_CREATE_INELIGIBLE, tags).increment();
  }

  public ScmTimerContext startPrCommentTimer(final ScmCommentOperation operation, final String provider) {
    if (meterRegistry == null || tenantUtil.isSingleTenant()) {
      return null;
    }

    return new ScmTimerContext(Timer.start(meterRegistry), operation, provider);
  }

  public void recordPrCommentCompleted(final ScmTimerContext context) {
    if (meterRegistry == null || tenantUtil.isSingleTenant() || context == null) {
      return;
    }

    Tags tags = buildOperationTags(context.operation().value(), context.provider());
    context.sample().stop(meterRegistry.timer(PR_COMMENT_DURATION, tags));
    meterRegistry.counter(PR_COMMENT_COMPLETED, tags).increment();
  }

  public void recordPrCommentFailed(final ScmTimerContext context) {
    if (meterRegistry == null || tenantUtil.isSingleTenant() || context == null) {
      return;
    }

    meterRegistry.counter(PR_COMMENT_FAILED, buildOperationTags(context.operation().value(), context.provider()))
        .increment();
  }

  private Tags baseTags() {
    return Tags.of(KIND_TAG, METRIC_KIND, TENANT_ID_TAG, TenantThreadLocal.getTenant().tenantSlug);
  }

  private Tags buildProviderTags(final String provider) {
    return baseTags().and(PROVIDER_TAG, provider);
  }

  private Tags buildOperationTags(final String operation, final String provider) {
    return baseTags().and(OPERATION_TAG, operation).and(PROVIDER_TAG, provider);
  }
}
