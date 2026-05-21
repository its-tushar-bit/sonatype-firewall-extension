/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.WaiverExpirationNotificationConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.WaiverExpirationNotificationConfig.WAIVER_EXPIRATION_NOTIFICATION_CONFIG;

/**
 * Data access object for {@link WaiverExpirationNotificationConfig} entities.
 * <p>
 * A missing row for an ownerId means the owner inherits from its parent.
 */
@Named
@Singleton
public class WaiverExpirationNotificationConfigDAO
    extends AbstractOperationalSqlDAO<WaiverExpirationNotificationConfig>
{
  @Inject
  public WaiverExpirationNotificationConfigDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return WAIVER_EXPIRATION_NOTIFICATION_CONFIG;
  }

  @Override
  public Class<WaiverExpirationNotificationConfig> getEntityClass() {
    return WaiverExpirationNotificationConfig.class;
  }

  /**
   * Finds the notification config for the given owner, or empty if none exists (meaning inherit).
   */
  public Optional<WaiverExpirationNotificationConfig> findByOwnerId(final String ownerId) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return findByOwnerId(tx, ownerId);
    }
  }

  public Optional<WaiverExpirationNotificationConfig> findByOwnerId(
      final TransactionContext tx,
      final String ownerId)
  {
    return tx.dsl()
        .selectFrom(WAIVER_EXPIRATION_NOTIFICATION_CONFIG)
        .where(WAIVER_EXPIRATION_NOTIFICATION_CONFIG.OWNER_ID.eq(ownerId))
        .fetchOptional()
        .map(this::toEntity);
  }

  /**
   * Saves (insert or update) the config for the given owner.
   */
  public void save(final WaiverExpirationNotificationConfig entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      save(tx, entity);
      tx.commit();
    }
  }

  private void save(final TransactionContext tx, final WaiverExpirationNotificationConfig entity) {
    Optional<WaiverExpirationNotificationConfig> existing = findByOwnerId(tx, entity.getOwnerId());
    if (existing.isPresent()) {
      entity.setId(existing.get().getId());
      super.update(tx, entity);
    }
    else {
      super.insert(tx, entity);
    }
  }

  /**
   * Returns all distinct non-null notification_days values stored across all config rows.
   * Each value is a raw comma-separated string (e.g. {@code "7,3,1"}).
   * Used by the email detection flow to discover which day thresholds to query for.
   */
  public List<String> findAllNotificationDays() {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return tx.dsl()
          .selectDistinct(WAIVER_EXPIRATION_NOTIFICATION_CONFIG.NOTIFICATION_DAYS)
          .from(WAIVER_EXPIRATION_NOTIFICATION_CONFIG)
          .where(WAIVER_EXPIRATION_NOTIFICATION_CONFIG.NOTIFICATION_DAYS.isNotNull())
          .fetch(WAIVER_EXPIRATION_NOTIFICATION_CONFIG.NOTIFICATION_DAYS);
    }
  }

  /**
   * Deletes the config for the given owner (reverts to inherit).
   */
  public void deleteByOwnerId(final TransactionContext tx, final String ownerId) {
    tx.dsl()
        .deleteFrom(WAIVER_EXPIRATION_NOTIFICATION_CONFIG)
        .where(WAIVER_EXPIRATION_NOTIFICATION_CONFIG.OWNER_ID.eq(ownerId))
        .execute();
  }
}
