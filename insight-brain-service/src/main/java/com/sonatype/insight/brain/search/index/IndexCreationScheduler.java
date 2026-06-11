/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;

import datadog.trace.api.DDTags;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class IndexCreationScheduler
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(IndexCreationScheduler.class);

  static final String TASK_NAME = "SearchIndexCreate";

  private final IndexService indexService;

  @Inject
  public IndexCreationScheduler(IndexService indexService) {
    this.indexService = indexService;
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      indexService.createSearchIndex();
      updateDatadogResourceName();
    }
    catch (Exception e) {
      log.error("Failed to update search index: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  private void updateDatadogResourceName() {
    final Span span = GlobalTracer.get().activeSpan();
    if (span != null) {
      span.setTag(DDTags.RESOURCE_NAME, "class com.sonatype.insight.brain.search.index.IndexCreationScheduler");
    }

    // OpenTelemetry equivalent for setting resource name (no-op when OTel agent is inactive)
    io.opentelemetry.api.trace.Span otelSpan = io.opentelemetry.api.trace.Span.current();
    if (otelSpan.getSpanContext().isValid()) {
      otelSpan.updateName("class com.sonatype.insight.brain.search.index.IndexCreationScheduler");
    }
  }
}
