/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;

import io.opentelemetry.api.trace.Span;
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

  /**
   * A Provider because the control plane reaches back the other way: {@code IndexService} holds a
   * Provider of this scheduler, and {@code SearchIndexJobService} holds one of {@code IndexService}.
   */
  private final Provider<SearchIndexJobService> jobService;

  @Inject
  public IndexCreationScheduler(
      final IndexService indexService,
      final Provider<SearchIndexJobService> jobService)
  {
    this.indexService = indexService;
    this.jobService = jobService;
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      indexService.createSearchIndex();
      recordFinished(true, null);
      updateSpanName();
    }
    catch (Exception e) {
      log.error("Failed to update search index: {}", e.getMessage(), e);
      recordFinished(false, e.getMessage());
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  /**
   * Reports the outcome to the control plane, swallowing its own failures. A rebuild that ran is not
   * worth abandoning over a bookkeeping write, and letting one escape here would reach the
   * {@code Throwable} handler in {@link #execute}, which shuts the server down.
   */
  private void recordFinished(final boolean succeeded, final String errorMessage) {
    try {
      jobService.get().onFullRebuildFinished(succeeded, errorMessage);
    }
    catch (Exception e) {
      log.warn("Could not record search index build outcome: {}", e.getMessage(), e);
    }
  }

  private void updateSpanName() {
    Span span = Span.current();
    if (span.getSpanContext().isValid()) {
      span.updateName("class " + IndexCreationScheduler.class.getName());
    }
  }
}
