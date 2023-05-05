/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.datadog;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import datadog.trace.api.DDTags;
import datadog.trace.api.interceptor.MutableSpan;
import datadog.trace.api.interceptor.TraceInterceptor;

/**
 * Perform some modifications on the traces sent to Datadog.
 * <p>
 * In the future if/when we have control of the Datadog agent (whether it's ours or SREs), this code should be moved
 * there using DD_APM_REPLACE_TAGS
 * </p>
 */
public class DatadogInterceptor
    implements TraceInterceptor
{
  static String POSTGRESQL_QUERY = "postgresql.query";

  // Pattern to look for tenant schemas in SQL queries. Basically `<space>t_<tenant id><period>`.
  private static final Pattern TENANT_SQL_PATTERN = Pattern.compile(" t_\\w*?\\.");

  @Override
  public Collection<? extends MutableSpan> onTraceComplete(final Collection<? extends MutableSpan> traceCollection) {
    for (final MutableSpan span : traceCollection) {
      if (POSTGRESQL_QUERY.contentEquals(span.getOperationName())) {
        processTenantInQuery(span);
      }
    }
    return traceCollection;
  }

  private void processTenantInQuery(final MutableSpan span) {
    String resourceName = span.getResourceName().toString();

    Matcher matcher = TENANT_SQL_PATTERN.matcher(resourceName);
    if (matcher.find()) {
      String newResourceName = matcher.replaceAll(" t_TENANT.");
      span.setTag(DDTags.RESOURCE_NAME, newResourceName);
    }
  }

  @Override
  public int priority() {
    // some high unique number so this interceptor is last
    return 100;
  }
}
