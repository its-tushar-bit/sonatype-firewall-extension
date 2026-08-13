/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.jooq.DialectHelper;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import org.jooq.impl.DSL;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@code TemporaryTableHelperTest} (CLM-45235). The reusable SQL builders
 * live in {@link TemporaryTableApplicationsSqlSupport}.
 */
@PostgresTest(suppressMigrations = true)
public class TemporaryTableHelperPgTest
    extends AbstractDatabaseTest
{
  private final TemporaryTableHelper temporaryTableHelper = new TemporaryTableHelper();

  @Rule
  public LogOutput logOutput = new LogOutput(TemporaryTableHelper.class);

  @Test
  public void testMaybeCreateTemporaryTableWithIds_LessThanMax() {
    List<String> ids = createIds(AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD);
    try (TransactionContext tx = createTransactionContext()) {
      boolean result = temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, ids);
      assertThat(result).isFalse();
    }
  }

  @Test
  public void testMaybeCreateTemporaryTableWithIds() {
    logOutput.setLogLevel(Level.TRACE);

    // insert 65,535+1 applications so that we are above the threshold for using a temporary table
    List<String> ids = createIds(AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD + 1);
    try (TransactionContext tx = createTransactionContext()) {
      boolean isCreated = temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, ids);
      assertThat(isCreated).isTrue();

      Long count = tx.dsl()
          .selectCount()
          .from(DSL.table("temporary_ids"))
          .fetchOne(0, Long.class);
      assertThat(count).isEqualTo(65536);
    }

    // simple verification that the temporary table was used
    assertThat(logOutput).atTraceLevel().contains("Temporary table created with 65536 IDs in");
  }

  private TransactionContext createTransactionContext() {
    DataStore dataStore = databaseRule.getOperationalDataStore();
    return new TransactionContext(
        dataStore.getDataSource(),
        DialectHelper.detectDialect(dataStore),
        dataStore.getDatabaseSchema());
  }

  private List<String> createIds(final int count) {
    return new ArrayList<>(Collections.nCopies(count, "x"));
  }
}
