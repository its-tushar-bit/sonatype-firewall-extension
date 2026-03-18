/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@H2DiskTest(cleanDatabase = true, suppressMigrations = true)
@Category(SlowTest.class)
public class OrganizationAncestorInitialPopulationMigratorTest
    extends AbstractDatabaseTest
{
  private static final String CREATE_AND_SET_SCHEMA =
      "CREATE SCHEMA IF NOT EXISTS insight_brain_ods; SET SCHEMA insight_brain_ods";

  protected static final String SELECT_ORG_ANCESTORS =
      "SELECT organization_id, ancestor_id, ancestor_distance FROM insight_brain_ods.organization_ancestor";

  private DataStore dataStore;

  private Connection connection;

  private LegacyDataStoreMigrator dataStoreMigrator;

  @Before
  public void setup() throws Exception {
    dataStore = databaseRule.getOperationalDataStore();
    connection = dataStore.getDataSource().getConnection();
    dataStoreMigrator = new LegacyDataStoreMigrator(dataStore)
    {
      @Override
      public int getDesiredVersion(String dataStoreId) {
        return 336;
      }
    };

    // create a schema populated to version 335
    dataStoreMigrator.runScript(CREATE_AND_SET_SCHEMA, getClass().getSimpleName() + "/prior_schema.sql");
    dataStoreMigrator.updateLegacyDatabaseSchemaVersion(dataStore.getDataSource(), dataStore.getID(),
        dataStore.getDatabaseSchema(), 335);

    populate();
  }

  @After
  public void closeConnection() throws Exception {
    if (connection != null) {
      connection.close();
      connection = null;
    }
  }

  @Test
  public void testUpgrade336() throws Exception {
    assertThatThrownBy(this::queryOrgAncestors)
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("Table \"organization_ancestor\" not found");

    // Update to the latest schema. The expectation is that this will create the organization_ancestor
    // table and populate it using the OrganizationAncestorInitialPopulationMigrator
    dataStoreMigrator.migrate();

    List<Object[]> orgAncestors = queryOrgAncestors();

    assertThat(orgAncestors).satisfiesExactlyInAnyOrder(
        row -> assertThat(row).containsExactly("ROOT_ORGANIZATION_ID", "ROOT_ORGANIZATION_ID", 0),
        row -> assertThat(row).containsExactly("1", "ROOT_ORGANIZATION_ID", 1),
        row -> assertThat(row).containsExactly("2", "ROOT_ORGANIZATION_ID", 1),
        row -> assertThat(row).containsExactly("11", "ROOT_ORGANIZATION_ID", 2),
        row -> assertThat(row).containsExactly("12", "ROOT_ORGANIZATION_ID", 2),
        row -> assertThat(row).containsExactly("111", "ROOT_ORGANIZATION_ID", 3),
        row -> assertThat(row).containsExactly("1", "1", 0),
        row -> assertThat(row).containsExactly("11", "1", 1),
        row -> assertThat(row).containsExactly("12", "1", 1),
        row -> assertThat(row).containsExactly("111", "1", 2),
        row -> assertThat(row).containsExactly("111", "11", 1),
        row -> assertThat(row).containsExactly("2", "2", 0),
        row -> assertThat(row).containsExactly("11", "11", 0),
        row -> assertThat(row).containsExactly("12", "12", 0),
        row -> assertThat(row).containsExactly("111", "111", 0));
  }

  protected void populate() throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO insight_brain_ods.organization " +
              "(organization_id, parent_organization_id, name, name_lowercase_no_whitespace) " +
              "VALUES " +
              "('1', 'ROOT_ORGANIZATION_ID', 'org1', 'org1'), " +
              "('2', 'ROOT_ORGANIZATION_ID', 'org2', 'org2'), " +
              "('11', '1', 'org11', 'org11'), " +
              "('12', '1', 'org12', 'org12'), " +
              "('111', '11', 'org111', 'org111')");
    }
  }

  private List<Object[]> queryOrgAncestors() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      return readResultSet(statement.executeQuery(SELECT_ORG_ANCESTORS));
    }
  }

  private static List<Object[]> readResultSet(ResultSet results) throws SQLException {
    int colCount = results.getMetaData().getColumnCount();
    List<Object[]> retval = new ArrayList<>();

    while (results.next()) {
      Object[] arr = new Object[colCount];

      for (int i = 0; i < colCount; i++) {
        arr[i] = results.getObject(i + 1);
      }

      retval.add(arr);
    }

    return retval;
  }
}
