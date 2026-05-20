/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;

/**
 * Static helpers for constructing {@link ConsumptionEvent} instances.
 *
 * @since 1.204
 */
public final class ConsumptionEvents
{
  private ConsumptionEvents() {
  }

  /**
   * Returns a builder prefilled with envelope fields from {@code ctx}: orgId, source, tier, eventTimestamp,
   * billingMonth.
   *
   * @param ctx non-null context; its orgId/tier/source must be non-null
   * @throws NullPointerException if any required field is null
   */
  public static ConsumptionEvent.Builder builderFromContext(final ConsumptionContext ctx) {
    Objects.requireNonNull(ctx, "ConsumptionContext must not be null");
    String orgId = Objects.requireNonNull(ctx.getOrgId(), "ConsumptionContext.orgId must not be null");
    String tier = Objects.requireNonNull(ctx.getTier(), "ConsumptionContext.tier must not be null");
    String source = Objects.requireNonNull(ctx.getSource(), "ConsumptionContext.source must not be null");
    return ConsumptionEvent.builder()
        .orgId(orgId)
        .source(source)
        .tier(tier)
        .eventTimestamp(Instant.now())
        .billingMonth(BillingWindowUtil.calculateWindowStart(
            LocalDate.now(ZoneOffset.UTC), BillingWindowUtil.DEFAULT_SUBSCRIPTION_DAY));
  }
}
