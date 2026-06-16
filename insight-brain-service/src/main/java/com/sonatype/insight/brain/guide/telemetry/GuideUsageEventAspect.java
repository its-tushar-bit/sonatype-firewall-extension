/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Emits one Guide usage event per invocation of a {@link GuideUsageEvent}-annotated method (on success). */
@Aspect
public class GuideUsageEventAspect
{
  private static final Logger log = LoggerFactory.getLogger(GuideUsageEventAspect.class);

  private volatile GuideUsageTelemetryCollector collector;

  @jakarta.inject.Inject
  void setCollector(final GuideUsageTelemetryCollector collector) {
    this.collector = collector;
  }

  @Around("execution(* *(..)) && @annotation(guideUsageEvent)")
  public Object recordUsage(
      final ProceedingJoinPoint joinPoint,
      final GuideUsageEvent guideUsageEvent) throws Throwable
  {
    Object result = joinPoint.proceed(); // emit only on success; lookup errors propagate unchanged
    GuideUsageTelemetryCollector c = collector; // may be null if Spring never resolved the aspect bean
    if (c != null) {
      try {
        c.record(guideUsageEvent.operationType(), joinPoint.getArgs());
      }
      catch (RuntimeException e) {
        // Telemetry must never break a user's Guide lookup: swallow and log.
        log.warn("Failed to record Guide usage telemetry", e);
      }
    }
    return result;
  }
}
