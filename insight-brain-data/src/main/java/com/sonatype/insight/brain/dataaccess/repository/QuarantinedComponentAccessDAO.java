/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.QuarantinedComponentAccess.QUARANTINED_COMPONENT_ACCESS;

@Named
@Singleton
public class QuarantinedComponentAccessDAO
    extends AbstractOperationalSqlDAO<QuarantinedComponentAccess>
{
  private static final int DELETE_BATCH_SIZE = 100;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public QuarantinedComponentAccessDAO(
      final OperationalDataStore operationalDataStore,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    super(operationalDataStore);
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Override
  public Table<?> getJooqTable() {
    return QUARANTINED_COMPONENT_ACCESS;
  }

  @Override
  public List<QuarantinedComponentAccess> getAll(TransactionContext tx) {
    return tx.dsl().selectFrom(QUARANTINED_COMPONENT_ACCESS).fetch(super::toEntity);
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
    int deletedRows = 0;

    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      List<String> ids;
      do {
        ids = tx.dsl()
            .select(QUARANTINED_COMPONENT_ACCESS.QUARANTINED_COMPONENT_ACCESS_ID)
            .from(QUARANTINED_COMPONENT_ACCESS)
            .where(QUARANTINED_COMPONENT_ACCESS.GENERATE_TIME.lt(cutoffDate))
            .limit(DELETE_BATCH_SIZE)
            .fetch(QUARANTINED_COMPONENT_ACCESS.QUARANTINED_COMPONENT_ACCESS_ID);

        if (!ids.isEmpty()) {
          int deleted = tx.dsl()
              .deleteFrom(QUARANTINED_COMPONENT_ACCESS)
              .where(QUARANTINED_COMPONENT_ACCESS.QUARANTINED_COMPONENT_ACCESS_ID.in(ids))
              .execute();
          deletedRows += deleted;
        }
      }
      while (!ids.isEmpty());

      tx.commit();
    }
    return deletedRows;
  }

  public void deleteByRepositoryComponentId(final TransactionContext tx, final String repositoryComponentId) {
    tx.dsl()
        .deleteFrom(QUARANTINED_COMPONENT_ACCESS)
        .where(QUARANTINED_COMPONENT_ACCESS.PROXY_REPOSITORY_COMPONENT_ID.eq(repositoryComponentId))
        .execute();
  }

  public void deleteByRepositoryId(final TransactionContext tx, final String repositoryId) {
    tx.dsl()
        .deleteFrom(QUARANTINED_COMPONENT_ACCESS)
        .where(QUARANTINED_COMPONENT_ACCESS.REPOSITORY_ID.eq(repositoryId))
        .execute();
  }

  @Override
  public void delete(TransactionContext tx, QuarantinedComponentAccess entity) {
    tx.dsl()
        .deleteFrom(QUARANTINED_COMPONENT_ACCESS)
        .where(QUARANTINED_COMPONENT_ACCESS.QUARANTINED_COMPONENT_ACCESS_ID.eq(entity.getId()))
        .execute();
  }

  public void setAnonymousAccess(boolean enabled) {
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(
        SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, String.valueOf(enabled)));
  }

  public boolean isAnonymousAccessEnabled() {
    return Boolean.parseBoolean(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS)
        .getValue());
  }

  @Override
  public Class<QuarantinedComponentAccess> getEntityClass() {
    return QuarantinedComponentAccess.class;
  }
}
