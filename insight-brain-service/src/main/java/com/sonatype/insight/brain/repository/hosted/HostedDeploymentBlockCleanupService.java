/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single source of truth for purging stale {@code hosted_deployment_block} (+ child violation)
 * rows. Both the periodic 24-hour {@link HostedDeploymentBlockCleanupTask Quartz job} and the
 * on-demand {@code HostedDeploymentBlockCleanupResource} REST endpoint delegate here so they
 * cannot drift in behaviour.
 * <p>
 * Child {@link com.sonatype.insight.brain.model.repository.HostedDeploymentBlockViolation}
 * rows cascade via FK; one DELETE handles both tables.
 */
@Named
@Singleton
public class HostedDeploymentBlockCleanupService
{
  static final Duration TESTING_TRIPWIRE_THRESHOLD = Duration.ofHours(1);

  private static final Logger log = LoggerFactory.getLogger(HostedDeploymentBlockCleanupService.class);

  private final HostedDeploymentBlockDAO hostedDeploymentBlockDAO;

  @Inject
  public HostedDeploymentBlockCleanupService(final HostedDeploymentBlockDAO hostedDeploymentBlockDAO) {
    this.hostedDeploymentBlockDAO = hostedDeploymentBlockDAO;
  }

  /**
   * Delete rows whose {@code blocked_time} is strictly before {@code now() - cutoffAge}.
   *
   * @param cutoffAge non-negative; {@code Duration.ZERO} deletes every row regardless of age
   *          (intended for destructive testing — caller is responsible for guarding
   *          against accidental production use)
   * @return outcome with rows-deleted and the absolute cutoff instant used
   */
  public CleanupOutcome runCleanup(final Duration cutoffAge) {
    Objects.requireNonNull(cutoffAge, "cutoffAge must not be null");
    if (cutoffAge.isNegative()) {
      throw new IllegalArgumentException("cutoffAge must not be negative: " + cutoffAge);
    }

    Instant cutoffTime = Instant.now().minus(cutoffAge);

    if (cutoffAge.compareTo(TESTING_TRIPWIRE_THRESHOLD) < 0) {
      // Production tripwire: anything below 1h is almost certainly someone running the test
      // harness against the real database, or a misconfigured trigger. Log loudly so it shows
      // up in ops dashboards.
      log.warn(
          "Hosted deployment block cleanup invoked with sub-1h cutoff: cutoffAge={}, cutoffTime={} "
              + "— this should only happen during test harness execution",
          cutoffAge, cutoffTime);
    }

    int deletedRows;
    try (TransactionContext tx = hostedDeploymentBlockDAO.createTransactionContext()) {
      tx.begin();
      deletedRows = hostedDeploymentBlockDAO.deleteOlderThan(tx, cutoffTime);
      tx.commit();
    }

    if (deletedRows > 0) {
      log.info("Purged {} hosted deployment block rows older than {} (cutoffAge={})",
          deletedRows, cutoffTime, cutoffAge);
    }
    else {
      log.debug("Hosted deployment block cleanup: no rows older than {}", cutoffTime);
    }
    return new CleanupOutcome(deletedRows, cutoffTime);
  }

  public record CleanupOutcome(int deleted, Instant cutoffTime)
  {
  }
}
