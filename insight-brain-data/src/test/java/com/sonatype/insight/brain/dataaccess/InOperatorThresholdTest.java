/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.jooq.DialectHelper;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dataaccess.AbstractSqlDAO.H2_IN_OPERATOR_THRESHOLD;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.MigrationTracker.MIGRATION_TRACKER;
import static org.assertj.core.api.Assertions.assertThat;

public class InOperatorThresholdTest
    extends AbstractDatabaseTest
{
  private static final String KNOWN_ID = "root-organization";

  protected int getInOperatorThreshold() {
    return H2_IN_OPERATOR_THRESHOLD;
  }

  @Test
  public void testInClauseAboveThreshold() throws SQLException {
    assertInClauseFindsMatch(getInOperatorThreshold() + 1);
  }

  @Test
  public void testInClauseAtDoubleThreshold() throws SQLException {
    assertInClauseFindsMatch(getInOperatorThreshold() * 2);
  }

  @Test
  public void testInClauseAt65536() throws SQLException {
    assertInClauseFindsMatch(65_536);
  }

  @Test
  public void testInClauseAt100000() throws SQLException {
    assertInClauseFindsMatch(100_000);
  }

  protected void assertInClauseFindsMatch(int inClauseSize) throws SQLException {
    List<String> ids = new ArrayList<>(inClauseSize);
    ids.addAll(generateNonexistentIds(inClauseSize - 1));
    ids.add(KNOWN_ID);

    try (TransactionContext tx = createTx()) {
      List<String> results = tx.dsl()
          .select(MIGRATION_TRACKER.MIGRATION_TRACKER_ID)
          .from(MIGRATION_TRACKER)
          .where(MIGRATION_TRACKER.MIGRATION_TRACKER_ID.in(ids))
          .fetch(MIGRATION_TRACKER.MIGRATION_TRACKER_ID);

      assertThat(results).containsOnly(KNOWN_ID);
    }
  }

  protected TransactionContext createTx() throws SQLException {
    var ds = databaseRule.getOperationalDataStore();
    return new TransactionContext(
        ds.getDataSource(),
        DialectHelper.detectDialect(ds),
        ds.getDatabaseSchema());
  }

  private static List<String> generateNonexistentIds(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> "nonexistent-id-" + i)
        .toList();
  }
}
