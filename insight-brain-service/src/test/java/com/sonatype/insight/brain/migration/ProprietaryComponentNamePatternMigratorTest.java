/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.migrations.LegacyDataStoreMigrator;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProprietaryComponentNamePatternMigratorTest
    extends AbstractDatabaseTest
{
  private void runScript(String scriptName) throws Exception {
    String scriptResource;
    if (scriptName.startsWith("schema_incremental_")) {
      scriptResource = "db/insight_brain_ods/" + scriptName;
    }
    else {
      scriptResource = getClass().getSimpleName() + '/' + scriptName;
    }

    new LegacyDataStoreMigrator(databaseRule.getOperationalDataStore())
        .runScript("", scriptResource + ".sql");
  }

  private void populateH2Database(String scriptName) throws Exception {
    runScript("create_schema");
    runScript("set_schema_h2");
    runScript("schema");
    runScript("schema_incremental_0291");
    runScript(scriptName);
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrate_H2() throws Exception {
    populateH2Database("data_before");
    testMigrate();
  }

  private void testMigrate() throws Exception {
    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();
    new ProprietaryComponentNamePatternMigrator().migrate(dataSource,
        databaseRule.getOperationalDataStore().getDatabaseSchema());
    runScript("schema_incremental_0292");

    // Assert the repo manager was added correctly
    String repoManagerId = null;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement select = connection.prepareStatement(
            "SELECT * FROM insight_brain_ods.repository_manager WHERE instance_id='testRepoManagerInstanceId'"))
    {
      int repoManagerCount = 0;
      try (ResultSet resultSet = select.executeQuery()) {
        while (resultSet.next()) {
          repoManagerCount++;
          repoManagerId = resultSet.getString("repository_manager_id");
          assertThat(repoManagerId).isNotNull();
          assertThat(resultSet.getString("instance_id")).isEqualTo("testRepoManagerInstanceId");
        }
      }
      assertThat(repoManagerCount).isEqualTo(1);
    }

    // Assert the hosted repo was added correctly
    String repoId = null;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement select =
            connection.prepareStatement(
                "SELECT * FROM insight_brain_ods.repository WHERE public_id='testRepoPublicId'"))
    {
      int repoCount = 0;
      try (ResultSet resultSet = select.executeQuery()) {
        while (resultSet.next()) {
          repoCount++;
          repoId = resultSet.getString("repository_id");
          assertThat(repoId).isNotNull();
          assertThat(resultSet.getString("repository_manager_id")).isEqualTo(repoManagerId);
          assertThat(resultSet.getString("public_id")).isEqualTo("testRepoPublicId");
          assertThat(resultSet.getString("repository_type")).isEqualTo("hosted");
          assertThat(resultSet.getString("format")).isEqualTo("npm");
          assertThat(resultSet.getBoolean("enabled")).isFalse();
          assertThat(resultSet.getBoolean("quarantine_enabled")).isFalse();
          assertThat(resultSet.getBoolean("policy_compliant_component_selection_enabled")).isFalse();
          assertThat(resultSet.getBoolean("namespace_confusion_protection_enabled")).isTrue();
        }
      }
      assertThat(repoCount).isEqualTo(1);
    }

    // Assert that the patterns were migrated correctly
    try (Connection connection = dataSource.getConnection();
        PreparedStatement select =
            connection.prepareStatement("SELECT * FROM insight_brain_ods.proprietary_component_name_pattern"
                + " ORDER BY proprietary_component_name_pattern_id"))
    {
      int migratedPatternCount = 0;
      try (ResultSet resultSet = select.executeQuery()) {
        while (resultSet.next()) {
          migratedPatternCount++;
          assertThat(resultSet.getString("format")).isEqualTo("npm");
          assertThat(resultSet.getString("repository_id")).isEqualTo(repoId);
          if (migratedPatternCount == 1) {
            assertThat(resultSet.getString("proprietary_component_name_pattern_id")).isEqualTo("testPatternId1");
            assertThat(resultSet.getString("namespace_pattern")).isEqualTo("@namespacePattern");
            assertThat(resultSet.getString("name_pattern")).isEqualTo("");
            assertThat(resultSet.getBoolean("enabled")).isTrue();
          }
          else {
            assertThat(resultSet.getString("proprietary_component_name_pattern_id")).isEqualTo("testPatternId2");
            assertThat(resultSet.getString("namespace_pattern")).isEqualTo("");
            assertThat(resultSet.getString("name_pattern")).isEqualTo("namePattern");
            assertThat(resultSet.getBoolean("enabled")).isFalse();
          }
        }
      }
      assertThat(migratedPatternCount).isEqualTo(2);
    }
  }
}
