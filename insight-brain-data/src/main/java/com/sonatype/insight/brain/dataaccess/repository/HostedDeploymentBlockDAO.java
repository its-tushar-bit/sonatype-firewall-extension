/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.jooq.Query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlockViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedDeploymentBlock.HOSTED_DEPLOYMENT_BLOCK;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedDeploymentBlockViolation.HOSTED_DEPLOYMENT_BLOCK_VIOLATION;

/**
 * DAO for {@link HostedDeploymentBlock} records — synchronous hosted-repository deployment
 * attempts that were blocked by policy evaluation.
 * <p>
 * Child {@link HostedDeploymentBlockViolation} rows are persisted through this DAO (there is
 * no separate violation DAO) because violations have no independent lifecycle — they are always
 * created alongside their parent block, and the foreign-key CASCADE handles deletion.
 * Callers who need to retrieve violations for a block can use
 * {@link #getViolationsByBlockId(TransactionContext, String)}.
 * <p>
 * Continuous monitoring does not read from these tables by design — a blocked deployment means
 * the artifact never entered the repository, so there is nothing for CM to re-scan.
 */
@Named
@Singleton
public class HostedDeploymentBlockDAO
    extends AbstractOperationalSqlDAO<HostedDeploymentBlock>
{
  @Inject
  public HostedDeploymentBlockDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Persists a block record and its violations atomically.
   * <p>
   * The block is inserted first, then each violation. All rows land in a single transaction so
   * a partial write (block without violations) cannot occur.
   *
   * @param tx transaction context (caller owns begin/commit)
   * @param block the parent block record; its id must already be set
   * @param violations the policy violations that caused the block; may be empty but not null
   */
  public void insertWithViolations(
      final TransactionContext tx,
      final HostedDeploymentBlock block,
      final List<HostedDeploymentBlockViolation> violations)
  {
    Objects.requireNonNull(block, "block must not be null");
    Objects.requireNonNull(block.getId(), "block id must be set before insert");
    Objects.requireNonNull(violations, "violations must not be null (use empty list instead)");

    insert(tx, block, false);

    if (violations.isEmpty()) {
      return;
    }

    // Assign the parent FK on every violation before insert.
    for (HostedDeploymentBlockViolation violation : violations) {
      violation.setHostedDeploymentBlockId(block.getId());
    }
    // jOOQ batch insert — each violation must have its id set before the batch.
    // Callers are expected to generate ids upstream (alongside block.getId()).
    insertViolationBatch(tx, violations);
  }

  private void insertViolationBatch(
      final TransactionContext tx,
      final List<HostedDeploymentBlockViolation> violations)
  {
    // Single batch round-trip rather than one INSERT per violation. Components hitting
    // many policies can produce 10-20 violations; batching avoids the N+1 anti-pattern
    // while preserving the same atomicity (caller's transaction wraps the batch).
    List<Query> batch = new ArrayList<>(violations.size());
    for (HostedDeploymentBlockViolation v : violations) {
      Objects.requireNonNull(v.getId(), "violation id must be set before insert");
      batch.add(tx.dsl()
          .insertInto(HOSTED_DEPLOYMENT_BLOCK_VIOLATION)
          .set(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.HOSTED_DEPLOYMENT_BLOCK_VIOLATION_ID, v.getId())
          .set(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.HOSTED_DEPLOYMENT_BLOCK_ID, v.getHostedDeploymentBlockId())
          .set(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.POLICY_NAME, v.getPolicyName())
          .set(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.CONSTRAINT_NAME, v.getConstraintName())
          .set(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.REASON, v.getReason())
          .set(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.COMPONENT_IDENTIFIER, v.getComponentIdentifier()));
    }
    tx.dsl().batch(batch).execute();
  }

  /**
   * Returns the block record with the given id, or null if none exists.
   */
  public HostedDeploymentBlock getById(final TransactionContext tx, final String id) {
    Objects.requireNonNull(id, "id must not be null");
    return tx.dsl()
        .selectFrom(HOSTED_DEPLOYMENT_BLOCK)
        .where(HOSTED_DEPLOYMENT_BLOCK.HOSTED_DEPLOYMENT_BLOCK_ID.eq(id))
        .fetchOne(super::toEntity);
  }

  /**
   * Returns all block records for a repository, newest first.
   */
  public List<HostedDeploymentBlock> getByRepositoryId(
      final TransactionContext tx,
      final String repositoryId)
  {
    Objects.requireNonNull(repositoryId, "repositoryId must not be null");
    return tx.dsl()
        .selectFrom(HOSTED_DEPLOYMENT_BLOCK)
        .where(HOSTED_DEPLOYMENT_BLOCK.REPOSITORY_ID.eq(repositoryId))
        .orderBy(HOSTED_DEPLOYMENT_BLOCK.BLOCKED_TIME.desc())
        .fetch(super::toEntity);
  }

  /**
   * Returns all violations recorded for the given block id.
   */
  public List<HostedDeploymentBlockViolation> getViolationsByBlockId(
      final TransactionContext tx,
      final String blockId)
  {
    Objects.requireNonNull(blockId, "blockId must not be null");
    return tx.dsl()
        .selectFrom(HOSTED_DEPLOYMENT_BLOCK_VIOLATION)
        .where(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.HOSTED_DEPLOYMENT_BLOCK_ID.eq(blockId))
        .fetch(record -> {
          HostedDeploymentBlockViolation violation = new HostedDeploymentBlockViolation();
          violation.setId(record.get(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.HOSTED_DEPLOYMENT_BLOCK_VIOLATION_ID));
          violation
              .setHostedDeploymentBlockId(record.get(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.HOSTED_DEPLOYMENT_BLOCK_ID));
          violation.setPolicyName(record.get(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.POLICY_NAME));
          violation.setConstraintName(record.get(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.CONSTRAINT_NAME));
          violation.setReason(record.get(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.REASON));
          violation.setComponentIdentifier(record.get(HOSTED_DEPLOYMENT_BLOCK_VIOLATION.COMPONENT_IDENTIFIER));
          return violation;
        });
  }

  /**
   * Deletes block rows older than the given instant. Child violations cascade via FK.
   *
   * @param tx transaction context (caller owns begin/commit)
   * @param olderThan rows with {@code blocked_time} strictly before this instant will be deleted
   * @return number of parent rows deleted
   */
  public int deleteOlderThan(final TransactionContext tx, final Instant olderThan) {
    Objects.requireNonNull(olderThan, "olderThan must not be null");
    return tx.dsl()
        .deleteFrom(HOSTED_DEPLOYMENT_BLOCK)
        .where(HOSTED_DEPLOYMENT_BLOCK.BLOCKED_TIME.lt(Date.from(olderThan)))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return HOSTED_DEPLOYMENT_BLOCK;
  }

  @Override
  public Class<HostedDeploymentBlock> getEntityClass() {
    return HostedDeploymentBlock.class;
  }
}
