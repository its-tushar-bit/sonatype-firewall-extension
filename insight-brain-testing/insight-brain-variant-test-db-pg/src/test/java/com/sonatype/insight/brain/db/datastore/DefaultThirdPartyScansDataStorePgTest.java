/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

/**
 * PostgreSQL migration check for the third-party-scans data store, relocated from
 * {@code DefaultThirdPartyScansDataStoreTest} (CLM-45235). The H2 coverage stays in the origin.
 */
public class DefaultThirdPartyScansDataStorePgTest
    extends AbstractDataStorePgTest
{
  @Override
  protected DataStore getTestDataStore() {
    return databaseRule.getThirdPartyScansDataStore();
  }
}
