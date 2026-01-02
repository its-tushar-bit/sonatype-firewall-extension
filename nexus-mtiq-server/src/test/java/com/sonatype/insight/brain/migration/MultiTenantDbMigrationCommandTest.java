/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantDbMigrationCommandTest
    extends AbstractMultiTenantDatabaseTest
{
  @After
  public void after() {
    // databases for this are not reusable
    databaseRule.markFixtureAsDirty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(suppressMigrations = false, cleanDatabase = true)
  public void testQuartzTableDoesExist() {
    // do migration (setup global schema) and global quartz table does exist
    MultiTenantDbMigrationCommand multiTenantDbMigrationCommand = new TestMultiTenantDbMigrationCommand();
    assertThat(
        multiTenantDbMigrationCommand.quartzSchedulerStateTableExists(databaseRule.getOperationalDataStore())).isTrue();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(suppressMigrations = true, cleanDatabase = true)
  public void testQuartzTableDoesNotExist() {
    // do NOT DO migration (setup global schema) and global quartz table DOES NOT exist
    MultiTenantDbMigrationCommand multiTenantDbMigrationCommand = new TestMultiTenantDbMigrationCommand();
    assertThat(
        multiTenantDbMigrationCommand.quartzSchedulerStateTableExists(
            databaseRule.getOperationalDataStore())).isFalse();
  }

  private class TestMultiTenantDbMigrationCommand
      extends MultiTenantDbMigrationCommand
  {
    @Override
    public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
      return databaseRule.getDatabaseContainer();
    }
  }
}
