/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.InsertValuesStep1;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class can be used to help overcome issues with the 65,535 parameter limit in PostgreSQL. If you pass more than
 * this max into an `IN` clause, it will throw an error. To overcome this, we can create a temporary table and insert
 * the IDs that we want to use in the `IN` clause. Then we can use a JOIN on this temporary table to get the data we
 * need.
 * <p>
 * Note the implementation was created for IQ applications but is in fact generic and can be used for anything.
 */
@Named
@Singleton
public class TemporaryTableHelper
{
  private static final Logger log = LoggerFactory.getLogger(TemporaryTableHelper.class);

  private static final org.jooq.Field<String> ID_FIELD = DSL.field("id", SQLDataType.VARCHAR);

  /**
   * This method will 'maybe' create the temporary table. It will only do it if actually necessary (more than 65,535
   * IDs) and then will return a boolean if the table was created or not. This will allow more efficient usage.
   *
   * @param tx The transaction context to participate in. Note `tx.begin` is called in this method.
   * @param ids The IDs to insert into the temporary table
   */
  public boolean maybeCreateTemporaryTableWithIds(
      TransactionContext tx,
      Collection<String> ids)
  {
    if (ids.size() <= AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD) {
      return false;
    }

    long start = System.currentTimeMillis();

    if (!tx.isActive()) {
      tx.begin();
    }

    // Create the temporary table using jOOQ DSL
    tx.dsl().execute("CREATE TEMPORARY TABLE temporary_ids (id text NOT NULL) ON COMMIT DROP");

    // Insert IDs in batches to avoid parameter issues
    int batchSize = 5_000; // 5k seems to be a sweet spot. Settles at ~200ms to insert a total of 66k IDs

    List<String> idsList = new ArrayList<>(ids);
    org.jooq.Table<?> tempTable = DSL.table("temporary_ids");

    for (int batchStart = 0; batchStart < idsList.size(); batchStart += batchSize) {
      int batchEnd = Math.min(batchStart + batchSize, idsList.size());
      List<String> batch = idsList.subList(batchStart, batchEnd);

      // Build batch insert using jOOQ
      InsertValuesStep1<?, String> insertStep = tx.dsl().insertInto(tempTable, ID_FIELD);
      for (String id : batch) {
        insertStep = insertStep.values(id);
      }
      insertStep.execute();
    }

    log.trace("Temporary table created with {} IDs in {} ms", ids.size(),
        System.currentTimeMillis() - start);

    return true;
  }
}
