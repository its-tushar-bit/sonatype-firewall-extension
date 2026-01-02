/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DefaultOperationalDataStoreTest
    extends AbstractDataStoreTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(DefaultOperationalDataStore.class);

  @Override
  protected DataStore getTestDataStore() {
    return databaseRule.getOperationalDataStore();
  }

  @Test
  @Override
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DefaultOperationalDataStoreTest/Migrate")
  @Category(SlowTest.class)
  public void testInit_Migrate() {
    super.testInit_Migrate();
  }
}
