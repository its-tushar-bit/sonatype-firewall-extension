/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owner-side helper for hosted-repo scan pipelines. Resolves the {@link HostedRepositoryComponent}
 * row that identifies a hosted-repo artifact and pins {@code owner_component_id} to the outer
 * artifact's {@code owner_component} row after policy evaluation.
 * <p>
 * Kept out of {@code ScanPolicyEvaluator} so the evaluator's Owner-typed API remains agnostic
 * to hosted-repo-specific caller work.
 */
@Named
@Singleton
public class HostedRepositoryComponentResolver
{
  private static final Logger log = LoggerFactory.getLogger(HostedRepositoryComponentResolver.class);

  static final String PIN_MISSED_METRIC = "insight_brain_hosted_repo_pin_missed_total";

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private final OwnerComponentDAO ownerComponentDAO;

  private final MeterRegistry meterRegistry;

  @Inject
  public HostedRepositoryComponentResolver(
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO,
      final OwnerComponentDAO ownerComponentDAO,
      @Nullable final MeterRegistry meterRegistry)
  {
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
    this.ownerComponentDAO = ownerComponentDAO;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Returns the {@link HostedRepositoryComponent} for {@code (repositoryId, pathname)}, creating
   * a new row when absent and updating {@code hash} / {@code componentId} in place when the caller
   * supplies fresher values.
   * <p>
   * Idempotent. Concurrent callers racing on the same {@code (repositoryId, pathname)} pair see
   * the same HRC id after resolution completes; the loser of the unique-constraint race re-reads
   * the winner from a fresh transaction (the insert path uses {@code ignoreDuplicateKey=true}, so
   * the underlying DAO handles the race via a savepoint on H2 and {@code ON CONFLICT DO NOTHING}
   * on PostgreSQL).
   *
   * <p>
   * {@code hash} is required when the row does not yet exist — {@code hosted_repository_component.hash}
   * is {@code NOT NULL}, so a null would surface as a constraint violation from the insert. On an
   * existing row a null {@code hash} is tolerated and means "no fresher value", leaving the stored
   * hash alone. Both callers satisfy this: {@code ScanXmlParser} skips any {@code
   *
  <dir>
   * } without a
   * {@code sha1}, and the monitoring path selects components by an equality predicate on a non-null
   * hash.
   * <p>
   * On a hash change {@code owner_component_id} is cleared, because that pin points at an
   * {@code owner_component} row keyed on the previous hash. {@code pinOwnerComponent} repopulates it
   * after the next evaluation.
   *
   * @param componentIdOrNull optional NXRM component id; null does not overwrite an existing value.
   */
  public HostedRepositoryComponent getOrCreate(
      final String repositoryId,
      final String pathname,
      final String hash,
      @Nullable final String componentIdOrNull)
  {
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      HostedRepositoryComponent existing =
          hostedRepositoryComponentDAO.getByRepositoryIdAndPathname(tx, repositoryId, pathname);
      if (existing != null) {
        boolean dirty = false;
        boolean hashChanged = false;
        if (hash != null && !hash.equals(existing.getHash())) {
          existing.setHash(hash);
          hashChanged = true;
          dirty = true;
        }
        if (componentIdOrNull != null && !componentIdOrNull.equals(existing.getComponentId())) {
          existing.setComponentId(componentIdOrNull);
          dirty = true;
        }
        if (dirty) {
          tx.begin();
          // Targeted update for the same reason as pinOwnerComponent: a full-row write from this
          // copy would revert owner_component_id if a pin committed between the read above and here.
          hostedRepositoryComponentDAO.updateHashAndComponentId(
              tx, existing.getId(), existing.getHash(), existing.getComponentId());
          if (hashChanged) {
            // The existing pin points at an owner_component row keyed on the previous hash, so it no
            // longer describes this artifact. pinOwnerComponent refills it after the next evaluation.
            hostedRepositoryComponentDAO.updateOwnerComponentId(tx, existing.getId(), null);
            existing.setOwnerComponentId(null);
          }
          tx.commit();
        }
        return existing;
      }

      HostedRepositoryComponent fresh = new HostedRepositoryComponent(repositoryId, pathname, hash);
      if (componentIdOrNull != null) {
        fresh.setComponentId(componentIdOrNull);
      }
      tx.begin();
      int inserted = hostedRepositoryComponentDAO.insert(tx, fresh, true);
      tx.commit();
      if (inserted == 1) {
        return fresh;
      }
    }
    try (TransactionContext readTx = hostedRepositoryComponentDAO.createTransactionContext()) {
      HostedRepositoryComponent winner =
          hostedRepositoryComponentDAO.getByRepositoryIdAndPathname(readTx, repositoryId, pathname);
      if (winner == null) {
        throw new IllegalStateException("Duplicate-key insert reported no row inserted for "
            + "(repositoryId=" + repositoryId + ", pathname=" + pathname + "), but no winner row was found");
      }
      return winner;
    }
  }

  /**
   * Sets {@link HostedRepositoryComponent#setOwnerComponentId(String)} to the {@code owner_component}
   * row that {@link com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator} just wrote for
   * this HRC at the given stage and hash. WARN + increment {@link #PIN_MISSED_METRIC} + return when
   * no match is found; violations remain queryable on {@code hrc.owner_id} regardless of the pin.
   */
  public void pinOwnerComponent(
      final HostedRepositoryComponent hrc,
      final String scanId,
      final String stageTypeId)
  {
    OwnerComponent match = ownerComponentDAO.getByOwnerIdAndStageTypeIdAndHash(
        hrc.getId(), stageTypeId, hrc.getHash());
    if (match == null) {
      log.warn("Pin miss: no owner_component row for hrcId={} scanId={} stage={} hash={}",
          hrc.getId(), scanId, stageTypeId, hrc.getHash());
      recordPinMiss();
      return;
    }
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      // Single-column update, not update(tx, hrc): the caller read hrc before an HDS upload and a
      // policy evaluation, so a full-row write from that copy would revert whatever a concurrent
      // writer stored on the other columns meanwhile.
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), match.getId());
      tx.commit();
    }
    hrc.setOwnerComponentId(match.getId());
  }

  private void recordPinMiss() {
    if (meterRegistry != null) {
      meterRegistry.counter(PIN_MISSED_METRIC).increment();
    }
  }
}
