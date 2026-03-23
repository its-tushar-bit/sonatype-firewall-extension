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

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.InnerSourceVersion.INNER_SOURCE_VERSION;

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
    return tx.dsl()
        .selectFrom(INNER_SOURCE_VERSION)
        .where(INNER_SOURCE_VERSION.INNER_SOURCE_APPLICATION_ID.eq(innerSourceApplicationId))
        .fetch(this::toEntity);
  }

  public List<InnerSourceVersion> getByInnerSourceApplicationId(String innerSourceApplicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByInnerSourceApplicationId(tx, innerSourceApplicationId);
    }
  }

  public InnerSourceVersion getByInnerSourceApplicationIdAndStage(String innerSourceAppId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(INNER_SOURCE_VERSION)
          .where(INNER_SOURCE_VERSION.INNER_SOURCE_APPLICATION_ID.eq(innerSourceAppId));
      // Handle NULL stage type ID with IS NULL (SQL NULL comparison requires IS NULL)
      if (stageTypeId == null) {
        query = query.and(INNER_SOURCE_VERSION.STAGE_TYPE_ID.isNull());
      }
      else {
        query = query.and(INNER_SOURCE_VERSION.STAGE_TYPE_ID.eq(stageTypeId));
      }
      return toEntity(query.fetchOne());
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return INNER_SOURCE_VERSION;
  }

  @Override
  public Class<InnerSourceVersion> getEntityClass() {
    return InnerSourceVersion.class;
  }
}
