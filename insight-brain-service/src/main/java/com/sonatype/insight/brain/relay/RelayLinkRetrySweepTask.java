/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.RelayLinkState;
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
 * Hourly slow-sweep that promotes GitHub App rows from {@link RelayLinkState#FAILED} back to
 * {@link RelayLinkState#ERROR}, resetting the per-row attempt counter so the polling-cycle
 * pre-flight retries them on its next tick.
 *
 * <p>
 * The fast path ({@code RelayPollingService.pollOnce}) walks {@code UNREGISTERED}/{@code ERROR}
 * rows every minute; once a row's {@code relay_link_attempts} reaches
 * {@link RelayLinkState#MAX_ATTEMPTS} it transitions to {@code FAILED} and is no longer in the
 * fast loop. This sweep flips it back hourly so a relay outage that lasted longer than the
 * 10-attempt budget still recovers automatically without operator intervention.
 *
 * <p>
 * Gated on the relay feature flag the same way as {@link RelayEventLogCleanupTask}; if the
 * gate is closed the run is a no-op so disabled tenants don't churn rows.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class RelayLinkRetrySweepTask
    extends AdminTask
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(RelayLinkRetrySweepTask.class);

  private final GitHubAppDAO gitHubAppDAO;

  private final RelayRegistrationService relayRegistrationService;

  @Inject
  public RelayLinkRetrySweepTask(
      final GitHubAppDAO gitHubAppDAO,
      final RelayRegistrationService relayRegistrationService)
  {
    super("triggerRelayLinkRetrySweep");
    this.gitHubAppDAO = gitHubAppDAO;
    this.relayRegistrationService = relayRegistrationService;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run relay link retry sweep for tenant {}", TenantThreadLocal.getTenant());
    execute(this::run, log, "Relay link retry sweep error");
    log.info("Next relay link retry sweep execution scheduled for {}", jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run relay link retry sweep");
    int promoted = run();
    printWriter.write("Completed relay link retry sweep; promoted " + promoted + " row(s) FAILED -> ERROR\n");
  }

  /**
   * Runs the sweep. Returns the number of rows promoted (zero if the gate is closed or no
   * {@code FAILED} rows exist). Atomic per-row via a single bulk UPDATE.
   */
  public int run() {
    if (!relayRegistrationService.isFeatureGateOpen()) {
      log.debug("Relay feature gate closed; skipping relay link retry sweep");
      return 0;
    }
    int promoted = gitHubAppDAO.updateRelayLinkStateBulk(RelayLinkState.FAILED, RelayLinkState.ERROR);
    if (promoted > 0) {
      log.info("Promoted {} GitHub App row(s) from FAILED to ERROR for retry", promoted);
    }
    return promoted;
  }

  @Override
  public String getJobName() {
    return RelayLinkRetrySweepTask.class.getSimpleName();
  }
}
