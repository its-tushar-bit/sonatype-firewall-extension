/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Date;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ScanHealthConfig.SCAN_HEALTH_CONFIG;

/**
 * Data access object for {@link ScanHealthConfig} entities.
 * <p>
 * Manages persistence of scan health configuration settings, including the "fail on zero components" feature.
 * Configuration can be set at either the application or organization level and supports hierarchical inheritance.
 */
@Named
@Singleton
public class ScanHealthConfigDAO
    extends AbstractOperationalSqlDAO<ScanHealthConfig>
{
  @Inject
  public ScanHealthConfigDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return SCAN_HEALTH_CONFIG;
  }

  @Override
  public Class<ScanHealthConfig> getEntityClass() {
    return ScanHealthConfig.class;
  }

  /**
   * Finds a scan health configuration by owner type and owner ID.
   *
   * @param ownerType the type of owner (e.g., "application" or "organization")
   * @param ownerId the internal ID of the owner
   * @return an Optional containing the configuration if found, or empty if not found
   */
  public Optional<ScanHealthConfig> findByOwner(final String ownerType, final String ownerId) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return findByOwner(tx, ownerType, ownerId);
    }
  }

  /**
   * Finds a scan health configuration by owner type and owner ID within an existing transaction.
   *
   * @param tx the transaction context
   * @param ownerType the type of owner (e.g., "application" or "organization")
   * @param ownerId the internal ID of the owner
   * @return an Optional containing the configuration if found, or empty if not found
   */
  public Optional<ScanHealthConfig> findByOwner(
      final TransactionContext tx,
      final String ownerType,
      final String ownerId)
  {
    return tx.dsl()
        .selectFrom(SCAN_HEALTH_CONFIG)
        .where(SCAN_HEALTH_CONFIG.OWNER_TYPE.eq(ownerType))
        .and(SCAN_HEALTH_CONFIG.OWNER_ID.eq(ownerId))
        .fetchOptional()
        .map(this::toEntity);
  }

  /**
   * Saves a scan health configuration. If a configuration already exists for the given owner,
   * it will be updated; otherwise, a new configuration will be inserted.
   *
   * @param entity the configuration to save
   */
  public void save(final ScanHealthConfig entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      save(tx, entity);
      tx.commit();
    }
  }

  private void save(final TransactionContext tx, final ScanHealthConfig entity) {
    Optional<ScanHealthConfig> existing = findByOwner(tx, entity.getOwnerType(), entity.getOwnerId());

    if (existing.isPresent()) {
      final ScanHealthConfig existingConfig = existing.get();
      entity.setId(existingConfig.getId());
      entity.setCreateTime(existingConfig.getCreateTime());
      entity.setUpdateTime(new Date());
      super.update(tx, entity);
    }
    else {
      super.insert(tx, entity);
    }
  }

  /**
   * Deletes a scan health configuration by owner type and owner ID.
   *
   * @param ownerType the type of owner (e.g., "application" or "organization")
   * @param ownerId the internal ID of the owner
   */
  public void delete(final String ownerType, final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, ownerType, ownerId);
      tx.commit();
    }
  }

  /**
   * Deletes a scan health configuration by owner type and owner ID within an existing transaction.
   *
   * @param tx the transaction context
   * @param ownerType the type of owner (e.g., "application" or "organization")
   * @param ownerId the internal ID of the owner
   */
  public void delete(final TransactionContext tx, final String ownerType, final String ownerId) {
    tx.dsl()
        .deleteFrom(SCAN_HEALTH_CONFIG)
        .where(SCAN_HEALTH_CONFIG.OWNER_TYPE.eq(ownerType))
        .and(SCAN_HEALTH_CONFIG.OWNER_ID.eq(ownerId))
        .execute();
  }

  /**
   * Deletes all scan health configurations for a specific owner ID.
   * This method is used for cascade deletion when an application or organization is deleted.
   * <p>
   * Note: This method does not filter by owner type, so it will delete all configurations
   * matching the owner ID regardless of whether they are application or organization level.
   *
   * @param tx the transaction context
   * @param ownerId the internal ID of the owner whose configurations should be deleted
   */
  public void deleteByOwnerId(final TransactionContext tx, final String ownerId) {
    tx.dsl()
        .deleteFrom(SCAN_HEALTH_CONFIG)
        .where(SCAN_HEALTH_CONFIG.OWNER_ID.eq(ownerId))
        .execute();
  }
}
