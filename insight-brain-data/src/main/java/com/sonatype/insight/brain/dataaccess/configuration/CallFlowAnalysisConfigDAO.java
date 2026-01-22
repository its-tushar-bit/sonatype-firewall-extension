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
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

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

  public CallFlowAnalysisConfig getByOwnerId(String ownerId) {
    String sQuery = "SELECT entity FROM CallFlowAnalysisConfig entity WHERE entity.ownerId=?1";
    return get(sQuery, ownerId);
  }

  @Override
  public void insert(TransactionContext tx, CallFlowAnalysisConfig entity) {
    if (getByOwnerId(entity.getOwnerId()) != null) {
      throw new BadRequestException("A call flow analysis config already exists for owner id " + entity.getOwnerId());
    }

    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, CallFlowAnalysisConfig entity) {

    CallFlowAnalysisConfig existingConfigByOwner = getByOwnerId(entity.getOwnerId());
    if (existingConfigByOwner != null && !existingConfigByOwner.getId().equals(entity.getId())) {
      throw new BadRequestException("A call flow analysis config already exists for owner id " + entity.getOwnerId());
    }

    super.update(tx, entity);
  }
}
