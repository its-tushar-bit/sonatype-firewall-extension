/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class MultiTenantDbMigrationCommandTest
    extends AbstractMultiTenantDatabaseTest
{
  @AfterEach
  public void after() {
    // databases for this are not reusable
    databaseRule.markFixtureAsDirty();
  }

  @Test
  @PostgresTest(suppressMigrations = false, cleanDatabase = true)
  public void testQuartzTableDoesExist() {
    // do migration (setup global schema) and global quartz table does exist
    MultiTenantDbMigrationCommand multiTenantDbMigrationCommand = new MultiTenantDbMigrationCommand();
    assertThat(
        multiTenantDbMigrationCommand.quartzSchedulerStateTableExists(databaseRule.getOperationalDataStore())).isTrue();
  }

  @Test
  @PostgresTest(suppressMigrations = true, cleanDatabase = true)
  public void testQuartzTableDoesNotExist() {
    // do NOT DO migration (setup global schema) and global quartz table DOES NOT exist
    MultiTenantDbMigrationCommand multiTenantDbMigrationCommand = new MultiTenantDbMigrationCommand();
    assertThat(
        multiTenantDbMigrationCommand.quartzSchedulerStateTableExists(
            databaseRule.getOperationalDataStore())).isFalse();
  }
}
