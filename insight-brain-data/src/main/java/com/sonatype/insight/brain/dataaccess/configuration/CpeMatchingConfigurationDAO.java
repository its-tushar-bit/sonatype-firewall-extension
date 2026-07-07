/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.CpeMatchingConfiguration.CPE_MATCHING_CONFIGURATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

/**
 * @since 1.190
 */
@Named
@Singleton
public class CpeMatchingConfigurationDAO
    extends AbstractOperationalSqlDAO<CpeMatchingConfiguration>
{
  @Inject
  public CpeMatchingConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public CpeMatchingConfiguration getByOwnerIdWithHierarchyExcludingSelf(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(CPE_MATCHING_CONFIGURATION.fields())
          .from(CPE_MATCHING_CONFIGURATION)
          .join(OWNER_ANCESTOR)
          .on(CPE_MATCHING_CONFIGURATION.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
          .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
          .and(OWNER_ANCESTOR.ANCESTOR_DISTANCE.gt(0))
          .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
          .limit(1)
          .fetchOneInto(CpeMatchingConfiguration.class);
    }
  }

  public CpeMatchingConfiguration getByOwnerIdWithCpeEnabledWithHierarchyExcludingSelf(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(CPE_MATCHING_CONFIGURATION.fields())
          .from(CPE_MATCHING_CONFIGURATION)
          .join(OWNER_ANCESTOR)
          .on(CPE_MATCHING_CONFIGURATION.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
          .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
          .and(OWNER_ANCESTOR.ANCESTOR_DISTANCE.gt(0))
          .and(CPE_MATCHING_CONFIGURATION.CPE_ENABLED.isNotNull())
          .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
          .limit(1)
          .fetchOneInto(CpeMatchingConfiguration.class);
    }
  }

  public CpeMatchingConfiguration getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public CpeMatchingConfiguration getByOwnerId(final TransactionContext tx, final String ownerId) {
    return tx.dsl()
        .selectFrom(CPE_MATCHING_CONFIGURATION)
        .where(CPE_MATCHING_CONFIGURATION.OWNER_ID.eq(ownerId))
        .fetchOneInto(CpeMatchingConfiguration.class);
  }

  public void delete(final String internalOwnerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, internalOwnerId);
      tx.commit();
    }
  }

  public void delete(final TransactionContext tx, final String internalOwnerId) {
    CpeMatchingConfiguration cpeConfig = getByOwnerId(tx, internalOwnerId);
    if (cpeConfig != null) {
      delete(tx, cpeConfig);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return CPE_MATCHING_CONFIGURATION;
  }

  @Override
  public Class<CpeMatchingConfiguration> getEntityClass() {
    return CpeMatchingConfiguration.class;
  }
}
