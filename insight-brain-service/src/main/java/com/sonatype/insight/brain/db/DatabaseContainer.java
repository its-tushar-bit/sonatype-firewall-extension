/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;

/**
 * <p>
 * Encapsulate all the database layer objects.
 * </p>
 * <p>
 * The application performs all database connection and initialization before the Guice injection occurs. We need some
 * similar control over the instances created of database classes so this class can be considered to be a simple way to
 * track and manage those classes.
 * </p>
 */
public interface DatabaseContainer
    extends DataStoreProvider
{
  DataSourceProvider getDataSourceProvider();

  DatabaseProvisioner getDatabaseProvisioner();
}
