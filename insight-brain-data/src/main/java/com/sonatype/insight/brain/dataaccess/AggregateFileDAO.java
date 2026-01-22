/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.dataaccess.TransactionContext;

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
  public void update(TransactionContext tx, AggregateFile entity) {
    throw new UnsupportedOperationException("AggregateFile does not support update operations");
  }

  public List<AggregateFile> getByApplicationComponentId(TransactionContext tx, String applicationComponentId) {
    String sQuery = "SELECT entity FROM AggregateFile entity" + //
        " WHERE entity.applicationComponentId=?1";
    return getList(tx, sQuery, applicationComponentId);
  }

  public List<AggregateFile> getByApplicationComponentId(String applicationComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationComponentId(tx, applicationComponentId);
    }
  }
}
