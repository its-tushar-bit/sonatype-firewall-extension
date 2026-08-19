/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.shiro.util.CollectionUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.CiIntegrationsConfig.CI_INTEGRATIONS_CONFIG;

@Named
@Singleton
public class CiIntegrationsConfigDao
    extends AbstractOperationalSqlDAO<CiIntegrationsConfig>
{
  @Inject
  public CiIntegrationsConfigDao(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return CI_INTEGRATIONS_CONFIG;
  }

  @Override
  public Class<CiIntegrationsConfig> getEntityClass() {
    return CiIntegrationsConfig.class;
  }

  public Optional<CiIntegrationsConfig> findByOwner(final String ownerType, final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return findByOwner(tx, ownerType, ownerId);
    }
  }

  public Optional<CiIntegrationsConfig> findByOwner(
      final TransactionContext tx,
      final String ownerType,
      final String ownerId)
  {
    return tx.dsl()
        .selectFrom(CI_INTEGRATIONS_CONFIG)
        .where(CI_INTEGRATIONS_CONFIG.OWNER_TYPE.eq(ownerType))
        .and(CI_INTEGRATIONS_CONFIG.OWNER_ID.eq(ownerId))
        .fetchOptional()
        .map(this::toEntity);
  }

  public List<CiIntegrationsConfig> findByOwnerList(final List<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return findByOwnerList(tx, ownerIds);
    }
  }

  private List<CiIntegrationsConfig> findByOwnerList(final TransactionContext tx, final List<String> ownerIds) {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return List.of();
    }

    return tx.dsl()
        .selectFrom(CI_INTEGRATIONS_CONFIG)
        .where(CI_INTEGRATIONS_CONFIG.OWNER_ID.in(ownerIds))
        .fetch(this::toEntity);
  }

  public void save(final CiIntegrationsConfig entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      save(tx, entity);
      tx.commit();
    }
  }

  private void save(final TransactionContext tx, final CiIntegrationsConfig entity) {
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

  public void delete(final String ownerType, final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, ownerType, ownerId);
      tx.commit();
    }
  }

  public void delete(final TransactionContext tx, final String ownerType, final String ownerId) {
    Optional<CiIntegrationsConfig> config = findByOwner(tx, ownerType, ownerId);
    config.ifPresent(c -> delete(tx, c));
  }
}
