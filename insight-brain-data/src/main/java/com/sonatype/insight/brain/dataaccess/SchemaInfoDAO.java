/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.SchemaInfo;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.16
 */
public class SchemaInfoDAO
    extends AbstractOperationalSqlDAO<SchemaInfo>
{
  public SchemaInfo get() {
    try (TransactionContext tx = createTransactionContext()) {
      return get(tx);
    }
  }

  public SchemaInfo get(TransactionContext tx) {
    String sQuery = "SELECT entity FROM SchemaInfo entity";
    SchemaInfo schemaInfo = get(tx, sQuery);
    if (schemaInfo == null) {
      throw new IllegalStateException("ODS database corrupt, missing schema info");
    }
    return schemaInfo;
  }

  @Override
  public void insert(TransactionContext tx, SchemaInfo entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(TransactionContext tx, SchemaInfo entity) {
    throw new UnsupportedOperationException();
  }
}
