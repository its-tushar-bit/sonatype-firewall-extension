/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DefaultAggregationDataStoreTest
    extends AbstractDataStoreTest
{
  @Override
  protected DataStore getTestDataStore() {
    return databaseRule.getAggregationDataStore();
  }

  @Test
  @Override
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DefaultAggregationDataStoreTest/Migrate")
  @Category(SlowTest.class)
  public void testInit_Migrate() {
    super.testInit_Migrate();
  }
}
