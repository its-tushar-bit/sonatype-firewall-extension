/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.time.Duration;
import java.util.Collections;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;

import io.opentelemetry.api.trace.Span;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * @since 1.88
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class IndexService
    implements InsightJob, AllTenantsJob
{
  static final String TASK_NAME = "SearchIndexUpdate";

  private static final int JOB_EXECUTION_INTERVAL_IN_SECONDS = 3;

  private final SearchIndexClient searchIndexClient;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  private final Provider<IndexCreationScheduler> indexCreationScheduler;

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Inject
  public IndexService(
      final SearchIndexClient searchIndexClient,
      final TaskScheduler taskScheduler,
      final Provider<IndexCreationScheduler> indexCreationScheduler)
  {
    this.searchIndexClient = searchIndexClient;
    this.taskScheduler = taskScheduler;
    this.indexCreationScheduler = indexCreationScheduler;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(this, Duration.ofSeconds(JOB_EXECUTION_INTERVAL_IN_SECONDS));
  }

  @Override
  public void deregister() {
    // noop
  }

  /**
   * Manual re-creation of the search index - this happens via a one-time quartz task, triggered through the API
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void createIndexAsync() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    taskScheduler.scheduleOneTimeTask(indexCreationScheduler.get());
  }

  @Override
  public void executeForTenant(JobExecutionContext context, Tenant tenant) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      searchIndexClient.updateIndex();
      updateSpanName();
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

  /**
   * This class ends up being proxied by Guice and has a hash appended to its name which ruins trace span names. This
   * code alters the name to match the expected pattern. See CLM-25207.
   */
  private void updateSpanName() {
    Span span = Span.current();
    if (span.getSpanContext().isValid()) {
      span.updateName("class " + IndexService.class.getName());
    }
  }

  /**
   * Simply return if a full index is current in progress, triggered from {@link #createIndexAsync()} ()}
   */
  public boolean isFullIndexTriggered() {
    return taskScheduler.isJobTriggered(indexCreationScheduler.get(), Collections.emptyMap());
  }

  /**
   * Initial create of the search index and (re-)index ALL data
   */
  public void createSearchIndex() {
    searchIndexClient.populateIndex();
  }

  /**
   * Returns the index size - Used internally after {@link #createSearchIndex()} for telemetry - But also externally by
   * {@link com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryCollector}
   */
  public long getIndexSize() {
    return searchIndexClient.getIndexSize();
  }

  /**
   * Returns the last index time as an epoch.
   */
  public Long getLastIndexTime() {
    return searchIndexClient.getLastIndexTime();
  }
}
