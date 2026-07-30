/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.AggregateFile.AGGREGATE_FILE;

/**
 * @since 1.104
 */
@Named
@Singleton
public class AggregateFileDAO
    extends AbstractOperationalSqlDAO<AggregateFile>
{
  @Inject
  public AggregateFileDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int update(TransactionContext tx, AggregateFile entity) {
    throw new UnsupportedOperationException("AggregateFile does not support update operations");
  }

  public List<AggregateFile> getByOwnerComponentId(TransactionContext tx, String ownerComponentId) {
    return tx.dsl()
        .selectFrom(AGGREGATE_FILE)
        .where(AGGREGATE_FILE.OWNER_COMPONENT_ID.eq(ownerComponentId))
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

  public List<AggregateFile> getByOwnerComponentId(String ownerComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerComponentId(tx, ownerComponentId);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return AGGREGATE_FILE;
  }

  @Override
  public Class<AggregateFile> getEntityClass() {
    return AggregateFile.class;
  }
}
