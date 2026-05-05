/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import org.junit.Test;

public class DefaultThirdPartyScansDataStoreTest
    extends AbstractDataStoreTest
{
  @Override
  protected DataStore getTestDataStore() {
    return databaseRule.getThirdPartyScansDataStore();
  }

  @Test
  @Override
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "DefaultThirdPartyScansDataStoreTest/Migrate")
  public void testInit_Migrate() {
    super.testInit_Migrate();
  }
}
