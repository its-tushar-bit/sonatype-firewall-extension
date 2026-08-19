/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a SearchApiClient lookup method as a billable/observable Guide operation. The
 * {@link GuideUsageEventAspect} emits one telemetry event per invocation. {@code operationType}
 * is the REST-surface type; the aspect overrides it to {@code mcp_lookup} when the call arrives via MCP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuideUsageEvent
{
  GuideOperationType operationType();
}
