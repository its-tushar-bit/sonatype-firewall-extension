/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.CallFlowAnalysisConfig.CALL_FLOW_ANALYSIS_CONFIG;

/**
 * @since 1.172
 */
@Named
@Singleton
public class CallFlowAnalysisConfigDAO
    extends AbstractOperationalSqlDAO<CallFlowAnalysisConfig>
{
  @Inject
  public CallFlowAnalysisConfigDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public CallFlowAnalysisConfig getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public CallFlowAnalysisConfig getByOwnerId(final TransactionContext tx, final String ownerId) {
    return toEntity(tx.dsl()
        .selectFrom(CALL_FLOW_ANALYSIS_CONFIG)
        .where(CALL_FLOW_ANALYSIS_CONFIG.OWNER_ID.eq(ownerId))
        .fetchOne());
  }

  @Override
  public int insert(final TransactionContext tx, final CallFlowAnalysisConfig entity) {
    if (getByOwnerId(tx, entity.getOwnerId()) != null) {
      throw new BadRequestException("A call flow analysis config already exists for owner id " + entity.getOwnerId());
    }
    return super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final CallFlowAnalysisConfig entity) {
    CallFlowAnalysisConfig existingConfigByOwner = getByOwnerId(tx, entity.getOwnerId());
    if (existingConfigByOwner != null && !existingConfigByOwner.getId().equals(entity.getId())) {
      throw new BadRequestException("A call flow analysis config already exists for owner id " + entity.getOwnerId());
    }
    super.update(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return CALL_FLOW_ANALYSIS_CONFIG;
  }

  @Override
  public Class<CallFlowAnalysisConfig> getEntityClass() {
    return CallFlowAnalysisConfig.class;
  }
}
