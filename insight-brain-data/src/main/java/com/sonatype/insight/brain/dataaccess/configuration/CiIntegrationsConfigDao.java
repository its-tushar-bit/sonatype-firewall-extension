/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import org.apache.shiro.util.CollectionUtils;

/**
 * DAO for managing CI integrations configuration.
 */
@Named
@Singleton
public class CiIntegrationsConfigDao
    extends AbstractOperationalSqlDAO<CiIntegrationsConfig>
{
  @Inject
  public CiIntegrationsConfigDao(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Find CI integrations config by owner type and ID.
   */
  public Optional<CiIntegrationsConfig> findByOwner(String ownerType, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return findByOwner(tx, ownerType, ownerId);
    }
  }

  /**
   * Find CI integrations config by owner type and ID within a transaction.
   */
  public Optional<CiIntegrationsConfig> findByOwner(
      TransactionContext tx,
      String ownerType,
      String ownerId)
  {
    String query = "SELECT entity FROM CiIntegrationsConfig entity " +
        "WHERE entity.ownerType = ?1 AND entity.ownerId = ?2";
    CiIntegrationsConfig config = get(tx, query, ownerType, ownerId);
    return Optional.ofNullable(config);
  }

  /**
   * Find all CI integrations configs for the given owner IDs.
   */
  public List<CiIntegrationsConfig> findByOwnerList(List<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return findByOwnerList(tx, ownerIds);
    }
  }

  /**
   * Find all CI integrations configs for the given owner IDs (hierarchy search) within a transaction.
   */
  private List<CiIntegrationsConfig> findByOwnerList(TransactionContext tx, List<String> ownerIds) {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return List.of();
    }

    String query = "SELECT entity FROM CiIntegrationsConfig entity WHERE entity.ownerId IN ?1";
    return getList(tx, query, ownerIds);
  }

  /**
   * Save (insert or update) a CI integration config.
   */
  public void save(CiIntegrationsConfig entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      save(tx, entity);
      tx.commit();
    }
  }

  /**
   * Save (insert or update) a CI integration config within a transaction.
   */
  private void save(TransactionContext tx, CiIntegrationsConfig entity) {
    Optional<CiIntegrationsConfig> existing = findByOwner(tx, entity.getOwnerType(), entity.getOwnerId());

    if (existing.isPresent()) {
      CiIntegrationsConfig existingConfig = existing.get();

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
   * Delete CI integrations config by owner type and ID.
   */
  public void delete(String ownerType, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, ownerType, ownerId);
      tx.commit();
    }
  }

  /**
   * Delete CI integrations config by owner type and ID within a transaction.
   */
  public void delete(TransactionContext tx, String ownerType, String ownerId) {
    Optional<CiIntegrationsConfig> config = findByOwner(tx, ownerType, ownerId);
    config.ifPresent(c -> delete(tx, c));
  }
}
