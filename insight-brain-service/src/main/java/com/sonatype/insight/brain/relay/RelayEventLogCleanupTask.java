/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.relay.RelayEventLogDAO;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.sonatype.insight.brain.service.AdminTask;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job and admin task that prunes {@code relay_event_log} rows older than
 * {@link #DEFAULT_RETENTION}. Gated by the relay feature; if the gate is closed the run is a
 * no-op so cleanup does not race with disabled customers.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class RelayEventLogCleanupTask
    extends AdminTask
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(RelayEventLogCleanupTask.class);

  public static final Duration DEFAULT_RETENTION = Duration.ofDays(7);

  private final RelayEventLogDAO relayEventLogDAO;

  private final RelayRegistrationService relayRegistrationService;

  private final Duration retention;

  @Inject
  public RelayEventLogCleanupTask(
      final RelayEventLogDAO relayEventLogDAO,
      final RelayRegistrationService relayRegistrationService)
  {
    this(relayEventLogDAO, relayRegistrationService, DEFAULT_RETENTION);
  }

  RelayEventLogCleanupTask(
      final RelayEventLogDAO relayEventLogDAO,
      final RelayRegistrationService relayRegistrationService,
      final Duration retention)
  {
    super("triggerRelayEventLogCleanup");
    this.relayEventLogDAO = relayEventLogDAO;
    this.relayRegistrationService = relayRegistrationService;
    this.retention = retention;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run relay event log cleanup for tenant {}", TenantThreadLocal.getTenant());
    execute(this::run, log, "Relay event log cleanup error");
    log.info("Next relay event log cleanup execution scheduled for {}", jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run relay event log cleanup");
    int deleted = run();
    printWriter.write("Completed relay event log cleanup; deleted " + deleted + " row(s)\n");
  }

  /**
   * Runs the cleanup. Returns the number of rows deleted (zero if the relay feature gate is
   * closed or no rows were stale).
   */
  public int run() {
    if (!relayRegistrationService.isFeatureGateOpen()) {
      log.debug("Relay feature gate closed; skipping relay event log cleanup");
      return 0;
    }
    int deleted = relayEventLogDAO.deleteOlderThan(retention);
    if (deleted > 0) {
      log.info("Pruned {} relay_event_log row(s) older than {}", deleted, retention);
    }
    return deleted;
  }

  @Override
  public String getJobName() {
    return RelayEventLogCleanupTask.class.getSimpleName();
  }
}
