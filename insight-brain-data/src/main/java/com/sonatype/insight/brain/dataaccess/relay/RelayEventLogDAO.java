/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.relay;

import java.time.Duration;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.relay.RelayEventLog;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.SQLDialect;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RelayEventLog.RELAY_EVENT_LOG;

/**
 * DAO for {@link RelayEventLog}. The hot paths are an atomic
 * {@link #recordIfNew(String, String, Integer, String, String, String) recordIfNew} that swallows
 * the duplicate-key collision and a read-only secondary lookup.
 */
@Named
@Singleton
public class RelayEventLogDAO
    extends AbstractOperationalSqlDAO<RelayEventLog>
{
  @Inject
  public RelayEventLogDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Atomically records a processed relay event. Returns {@code true} if a new row was inserted,
   * {@code false} if a row with the same {@code eventId} already existed.
   *
   * <p>
   * The Postgres path uses {@code ON CONFLICT (event_id) DO NOTHING}; the H2 path falls back to
   * a SELECT-then-INSERT in a single transaction. Concurrent inserters will collapse to one
   * winner on Postgres; on H2 the second inserter sees the first via the SELECT.
   */
  public boolean recordIfNew(
      final String eventId,
      final String applicationPublicId,
      final Integer pullRequestNumber,
      final String commitHash,
      final String eventType,
      final String mode)
  {
    // Schema declares event_id NOT NULL. Guarding here gives both dialects the same
    // "nothing to record" semantics rather than letting Postgres throw an opaque
    // constraint violation through to the caller. Blank is treated like null so a
    // stray empty string cannot consume the unique-key slot for the empty value.
    if (StringUtils.isBlank(eventId)) {
      return false;
    }
    String id = newUUID();
    Date now = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl().dialect() == SQLDialect.H2
          ? recordIfNewH2(tx, id, eventId, applicationPublicId, pullRequestNumber, commitHash, eventType, mode, now)
          : recordIfNewPostgres(tx, id, eventId, applicationPublicId, pullRequestNumber, commitHash, eventType,
              mode, now);
    }
  }

  private boolean recordIfNewPostgres(
      final TransactionContext tx,
      final String id,
      final String eventId,
      final String applicationPublicId,
      final Integer pullRequestNumber,
      final String commitHash,
      final String eventType,
      final String mode,
      final Date now)
  {
    tx.begin();
    int rows = tx.dsl()
        .insertInto(RELAY_EVENT_LOG)
        .set(RELAY_EVENT_LOG.RELAY_EVENT_LOG_ID, id)
        .set(RELAY_EVENT_LOG.EVENT_ID, eventId)
        .set(RELAY_EVENT_LOG.APPLICATION_PUBLIC_ID, applicationPublicId)
        .set(RELAY_EVENT_LOG.PULL_REQUEST_NUMBER, pullRequestNumber)
        .set(RELAY_EVENT_LOG.COMMIT_HASH, commitHash)
        .set(RELAY_EVENT_LOG.EVENT_TYPE, eventType)
        .set(RELAY_EVENT_LOG.MODE, mode)
        .set(RELAY_EVENT_LOG.PROCESSED_AT, now)
        .onConflict(RELAY_EVENT_LOG.EVENT_ID)
        .doNothing()
        .execute();
    tx.commit();
    return rows > 0;
  }

  private boolean recordIfNewH2(
      final TransactionContext tx,
      final String id,
      final String eventId,
      final String applicationPublicId,
      final Integer pullRequestNumber,
      final String commitHash,
      final String eventType,
      final String mode,
      final Date now)
  {
    tx.begin();
    // eventId is guaranteed non-null by recordIfNew()'s guard above; H2 helper trusts that.
    boolean exists = tx.dsl()
        .fetchExists(tx.dsl().selectFrom(RELAY_EVENT_LOG).where(RELAY_EVENT_LOG.EVENT_ID.eq(eventId)));
    if (exists) {
      tx.commit();
      return false;
    }
    tx.dsl()
        .insertInto(RELAY_EVENT_LOG)
        .set(RELAY_EVENT_LOG.RELAY_EVENT_LOG_ID, id)
        .set(RELAY_EVENT_LOG.EVENT_ID, eventId)
        .set(RELAY_EVENT_LOG.APPLICATION_PUBLIC_ID, applicationPublicId)
        .set(RELAY_EVENT_LOG.PULL_REQUEST_NUMBER, pullRequestNumber)
        .set(RELAY_EVENT_LOG.COMMIT_HASH, commitHash)
        .set(RELAY_EVENT_LOG.EVENT_TYPE, eventType)
        .set(RELAY_EVENT_LOG.MODE, mode)
        .set(RELAY_EVENT_LOG.PROCESSED_AT, now)
        .execute();
    tx.commit();
    return true;
  }

  /**
   * Read-only check by primary {@code event_id}.
   */
  public boolean existsByEventId(final String eventId) {
    if (StringUtils.isBlank(eventId)) {
      return false;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .fetchExists(
              tx.dsl().selectFrom(RELAY_EVENT_LOG).where(RELAY_EVENT_LOG.EVENT_ID.eq(eventId)));
    }
  }

  /**
   * Read-only check for the secondary key
   * {@code (applicationPublicId, pullRequestNumber, commitHash, mode)}. Returns {@code false}
   * when {@code applicationPublicId} is null since the secondary index is meaningless without
   * an application context. {@code mode} discriminates rows produced under different relay
   * registration kinds (PAT vs GitHub App): a null {@code mode} parameter intentionally only
   * matches rows whose {@code mode} column is also null, so callers must pass the current
   * mode explicitly to avoid cross-mode over-matching.
   */
  public boolean isDuplicateBySecondaryKey(
      final String applicationPublicId,
      final Integer pullRequestNumber,
      final String commitHash,
      final String mode,
      final String eventType)
  {
    if (applicationPublicId == null) {
      return false;
    }
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(RELAY_EVENT_LOG)
          .where(RELAY_EVENT_LOG.APPLICATION_PUBLIC_ID.eq(applicationPublicId))
          .and(pullRequestNumber == null
              ? RELAY_EVENT_LOG.PULL_REQUEST_NUMBER.isNull()
              : RELAY_EVENT_LOG.PULL_REQUEST_NUMBER.eq(pullRequestNumber))
          .and(commitHash == null
              ? RELAY_EVENT_LOG.COMMIT_HASH.isNull()
              : RELAY_EVENT_LOG.COMMIT_HASH.eq(commitHash))
          .and(mode == null
              ? RELAY_EVENT_LOG.MODE.isNull()
              : RELAY_EVENT_LOG.MODE.eq(mode))
          // event_type discriminates close-vs-reopen on the same head SHA: both share
          // the (app, pr, commit, mode) tuple but produce distinct event_types
          // ('pull_request_closed' vs 'pull_request_opened') that drive different
          // downstream workflows. Without this clause the second event collides as a
          // secondary duplicate and is silently dropped. Cutover-dedup is unaffected
          // because relay-vs-legacy duplicates of the same logical event share the
          // event_type string (both 'pull_request_opened' / 'push' / etc.).
          .and(eventType == null
              ? RELAY_EVENT_LOG.EVENT_TYPE.isNull()
              : RELAY_EVENT_LOG.EVENT_TYPE.eq(eventType));
      return tx.dsl().fetchExists(query);
    }
  }

  /**
   * Deletes rows whose {@code processed_at} is older than {@code now - age}. Returns the number
   * of rows deleted.
   */
  public int deleteOlderThan(final Duration age) {
    Date cutoff = new Date(System.currentTimeMillis() - age.toMillis());
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int deleted = tx.dsl()
          .deleteFrom(RELAY_EVENT_LOG)
          .where(RELAY_EVENT_LOG.PROCESSED_AT.lt(cutoff))
          .execute();
      tx.commit();
      return deleted;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return RELAY_EVENT_LOG;
  }

  @Override
  public Class<RelayEventLog> getEntityClass() {
    return RelayEventLog.class;
  }
}
