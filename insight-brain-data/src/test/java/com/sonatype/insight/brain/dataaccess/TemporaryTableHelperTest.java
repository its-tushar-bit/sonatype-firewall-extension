/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresTest(suppressMigrations = true)
@Category({PostgresTestCategory.class, SlowTest.class})
public class TemporaryTableHelperTest
    extends AbstractDatabaseTest
{
  private final TemporaryTableHelper temporaryTableHelper = new TemporaryTableHelper();

  @Rule
  public LogOutput logOutput = new LogOutput(TemporaryTableHelper.class);

  public static String getInsertMaximumApplicationsSql(final String organizationId) {
    return """
        INSERT INTO insight_brain_ods.application (application_id, public_id, public_id_lowercase, name,
            name_lowercase_no_whitespace, organization_id)
        SELECT  REPLACE(gen_random_uuid()::text, '-', '') AS application_id,
        'application-' || g.id AS public_id,
        'application-' || g.id AS public_id_lowercase,
        'Application ' || g.id AS name,
        'application' || g.id AS name_lowercase_no_whitespace,
        '%s' AS organization_id
        FROM generate_series(1, %s) AS g (id)""".formatted(organizationId,
        AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD);
  }

  public static String getCleanupApplicationsSql() {
    return "DELETE FROM insight_brain_ods.application WHERE public_id LIKE 'application-%'";
  }

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

      Query query = tx.createNativeQuery("SELECT count(*) FROM temporary_ids");
      List<Long> result = query.getResultList();
      assertThat(result.size()).isEqualTo(1);
      assertThat(result.get(0)).isEqualTo(65536);
    }

    // simple verification that the temporary table was used
    assertThat(logOutput).atTraceLevel().contains("Temporary table created with 65536 IDs in");
  }

  private TransactionContext createTransactionContext() {
    DataStore dataStore = databaseRule.getOperationalDataStore();
    EntityManager entityManager = dataStore.getJPAEntityManagerFactory().createEntityManager();
    return new TransactionContext(entityManager);
  }

  private List<String> createIds(final int count) {
    return new ArrayList<>(Collections.nCopies(count, "x"));
  }
}
