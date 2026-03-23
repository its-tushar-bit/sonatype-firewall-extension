/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * <b>Definition of an IQ store of data.</b>
 *
 * <p>
 * Some related details on IQ database terminology. The code references four terms, each of which can be used
 * differently depending on how IQ is deployed and what database is used. The terms are:
 * <ol>
 * <li>Database - An actual database. The two supported are H2 and PostgreSQL.</li>
 * <li>DataSource - As in the {@link javax.sql.DataSource} class, which also means the connection pools itself.</li>
 * <li>Schema - A database feature that generally lets you namespace items such as tables, views, etc...</li>
 * <li>DataStore - Refers to a related set of data/tables. There are four data stores in use for IQ: Operational,
 * Aggregation, Data mart, Third party scans
 * </ol>
 * </p>
 *
 * <p>
 * Originally there was only support for the H2 database. H2 is a SQL database which exists as a disk-based file. Due to
 * locking issues in H2, the set of tables was split up into these four 'data stores' so that some of those locking
 * issues did not affect operation. This separation carried into PostgreSQL support, but is handled differently in
 * various deployment scenarios.
 * </p>
 *
 * <p>
 * <b>H2 - Single Tenant</b>
 * <ul>
 * <li>Database - each of the 4 data stores is a fully separate H2 .db file.</li>
 * <li>DataSource - each of the 4 data stores is a fully separate `javax.sql.DataSource`.</li>
 * <li>Schema - within each of the 4 physical databases, it will use the data store name as the schema name.</li>
 * </ul>
 * </p>
 * <br/>
 * <p>
 * <b>Postgres - Single-Tenant</b>
 * <ul>
 * <li>Database - A single database running on a PostgreSQL cluster. Note that 'cluster' is the correct term for a
 * running PostgreSQL instance, and that cluster can have many databases on it.</li>
 * <li>DataSource - each of the 4 data stores is a fully separate `javax.sql.DataSource`.</li>
 * <li>Schema - within the single database, each of the 4 data stores is a separate PostgreSQL schema.</li>
 * </ul>
 * </p>
 * <br/>
 * <p>
 * <b>Postgres - Multi-Tenant (for our Sonatype Saas deployment only)</b>
 * <ul>
 * <li>Database - A single database running on a PostgreSQL cluster.</li>
 * <li>DataSource - we only want a single (large) connection pool so each of the 4 data store providers use the same
 * `javax.sql.DataSource`. Connections are shared across tenants.</li>
 * <li>Schema - each tenant is in its own schema. The data store name is not used as the schema. It is tracked inside
 * the `schema_version` table</li>
 * </ul>
 * </p>
 * <br/>
 *
 * <p>
 * <b>Special mention: The 'locks' DataSource</b><br/>
 * During the initiative to operate IQ in a clustered deployment, a custom locking mechanism was developed and the
 * locks themselves were implemented in the database (CLM-16475). This applies to Postgres only (so a customer
 * single-tenant clustered Postgres, and the Sonatype multi-tenant Saas). A separate {@link javax.sql.DataSource} was
 * used for this to avoid deadlocks (CLM-17692) and in multi-tenant mode that separate DataSource is also used,
 * though there is one globally and not per-tenant.
 * </p>
 */
public interface DataStore
{
  /**
   * Return the identifier for the data store
   */
  String getID();

  /**
   * Perform the initialization of the data store. This includes creating all supporting objects (e.g.
   * {@link DataSource}), population of new databases, and migration on existing databases.
   */
  void initialize();

  /**
   * @return the {@link DataSource} for this data store
   */
  DataSource getDataSource();

  /**
   * @return the database schema used by this data store
   */
  String getDatabaseSchema();

  /**
   * @return the {@link DatabaseConfig} for this data store
   */
  DatabaseConfig getDatabaseConfig();

  DataSourceProvider getDataSourceProvider();

  /**
   * Is this a brand new data store (i.e. never been populated nor migrated)
   */
  boolean isDataStoreNew();

  /**
   * Does this data store use an embedded database (i.e. H2)?
   */
  boolean isDatabaseEmbedded();

  /**
   * Close and cleanup all resources associated with this data store.
   * This method should be called when the data store is no longer needed,
   * particularly when switching between different database types in tests.
   */
  void close() throws Exception;
}
