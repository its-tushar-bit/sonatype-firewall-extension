/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public abstract class AbstractDatabaseMigratorTest
    extends AbstractDatabaseTest
{
  // system under test
  private DatabaseMigrator databaseMigrator;

  // note this will get used four times for each test, one for each data store
  public DataStoreMigrator mockDataStoreMigrator;

  @Before
  public void before() {
    mockDataStoreMigrator = mock();
    databaseMigrator = createDatabaseMigratorForTest();
  }

  @Test
  @H2DiskTest(suppressMigrations = true)
  @Category(SlowTest.class)
  public void testMigrationNeeded() {
    databaseMigrator.migrate();
    verify(mockDataStoreMigrator, times(4)).migrate();
  }

  @Test
  @H2DiskTest
  @Category(SlowTest.class)
  public void testMigrationIsNotNeeded() {
    // note default on @H2DiskTest is that migrations are already run
    databaseMigrator.migrate();
    verifyNoInteractions(mockDataStoreMigrator);
  }

  protected abstract DatabaseMigrator createDatabaseMigratorForTest();

  protected DataStoreMigrator createMockDataStoreMigrator() {
    return mockDataStoreMigrator;
  }
}
