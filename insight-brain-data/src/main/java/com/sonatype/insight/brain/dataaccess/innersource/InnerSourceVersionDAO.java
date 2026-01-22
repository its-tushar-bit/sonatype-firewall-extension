/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class InnerSourceVersionDAO
    extends AbstractOperationalSqlDAO<InnerSourceVersion>
{
  @Inject
  public InnerSourceVersionDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<InnerSourceVersion> getByInnerSourceApplicationId(
      TransactionContext tx,
      String innerSourceApplicationId)
  {
    String sQuery = "SELECT entity FROM InnerSourceVersion entity WHERE entity.innerSourceApplicationId=?1";
    return getList(tx, sQuery, innerSourceApplicationId);
  }

  public List<InnerSourceVersion> getByInnerSourceApplicationId(String innerSourceApplicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByInnerSourceApplicationId(tx, innerSourceApplicationId);
    }
  }

  public InnerSourceVersion getByInnerSourceApplicationIdAndStage(String innerSourceAppId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM InnerSourceVersion entity" +
          " WHERE entity.innerSourceApplicationId=?1 AND entity.stageTypeId=?2";
      return get(tx, sQuery, innerSourceAppId, stageTypeId);
    }
  }
}
