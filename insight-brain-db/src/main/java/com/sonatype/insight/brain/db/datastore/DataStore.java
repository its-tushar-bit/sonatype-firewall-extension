/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * <b>Definition of an IQ store of data.</b>
 *
 * <p>
 * Some related details on IQ database terminology. The code references four terms, each of which can be used
 * differently depending on how IQ is deployed and what database is used. The terms are:
 * <ol>
 *   <li>Database - An actual database. The two supported are H2 and PostgreSQL.</li>
 *   <li>DataSource - As in the {@link javax.sql.DataSource} class, which also means the connection pools itself.</li>
 *   <li>Schema - A database feature that generally lets you namespace items such as tables, views, etc...</li>
 *   <li>DataStore - Refers to a related set of data/tables. There are four data stores in use for IQ: Operational,
 *   Aggregation, Data mart, Third party scans
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
 *   <li>Database - each of the 4 data stores is a fully separate H2 .db file.</li>
 *   <li>DataSource - each of the 4 data stores is a fully separate `javax.sql.DataSource`.</li>
 *   <li>Schema - within each of the 4 physical databases, it will use the data store name as the schema name.</li>
 * </ul>
 * </p>
 * <br/>
 * <p>
 * <b>Postgres - Single-Tenant</b>
 * <ul>
 *   <li>Database - A single database running on a PostgreSQL cluster. Note that 'cluster' is the correct term for a
 *   running PostgreSQL instance, and that cluster can have many databases on it.</li>
 *   <li>DataSource - each of the 4 data stores is a fully separate `javax.sql.DataSource`.</li>
 *   <li>Schema - within the single database, each of the 4 data stores is a separate PostgreSQL schema.</li>
 * </ul>
 * </p>
 * <br/>
 * <p>
 * <b>Postgres - Multi-Tenant (for our Sonatype Saas deployment only)</b>
 * <ul>
 *   <li>Database - A single database running on a PostgreSQL cluster.</li>
 *   <li>DataSource - we only want a single (large) connection pool so each of the 4 data store providers use the same
 *   `javax.sql.DataSource`. Connections are shared across tenants. Note that each tenant still has its own
 *   {@link javax.persistence.EntityManagerFactory}</li>
 *   <li>Schema - each tenant is in its own schema. The data store name is not used as the schema. It is tracked inside
 *   the `schema_version` table</li>
 * </ul>
 * </p>
 * <br/>
 *
 * <p>
 *   <b>Special mention: The 'locks' DataSource</b><br/>
 *   During the initiative to operate IQ in a clustered deployment, a custom locking mechanism was developed and the
 *   locks themselves were implemented in the database (CLM-16475). This applies to Postgres only (so a customer
 *   single-tenant clustered Postgres, and the Sonatype multi-tenant Saas). A separate {@link javax.sql.DataSource} was
 *   used for this to avoid deadlocks (CLM-17692) and in multi-tenant mode that separate DataSource is also used,
 *   though there is one globally and not per-tenant.
 * </p>
 */
public interface DataStore
{
  /**
   * Return the identifier for the data store
   */
  String getID();

  /**
   * Initialize the data store and perform database migrations. Initialization covers the creation of the necessary
   * objects for the data store (e.g. {@link DataSource} and {@link EntityManagerFactory} and also the population of new
   * and empty databases.
   *
   * @param databaseConfig
   * @param migrateToNewViolationModel IQ version 114 introduced a new violation model that required special handling.
   *                                   This flag indicates if that migration should be performed.
   */
  void initWithMigration(DatabaseConfig databaseConfig, Boolean migrateToNewViolationModel);

  /**
   * Same as {@link #initWithMigration} except does not perform migration.
   */
  void initWithoutMigration(DatabaseConfig databaseConfig);

  /**
   * Perform database migrations
   *
   * @param migrateToNewViolationModel See description in {@link #initWithMigration}
   */
  void migrate(Boolean migrateToNewViolationModel);

  /**
   * @return the {@link DataSource} for this data store
   */
  DataSource getDataSource();

  /**
   * @return the {@link DatabaseConfig} for this data store
   */
  DatabaseConfig getDatabaseConfig();

  /**
   * @return the {@link EntityManagerFactory} for this data store
   */
  EntityManagerFactory getJPAEntityManagerFactory();

  /**
   * Legacy method to clear data used in tests. With the advent of MTIQ the data store classes were moved from a static
   * model to a class based model. With the static model there needed to be a back door to reset the data source. This
   * change happened (will happen) incrementally and so this method will remain until the original *DataStoreProvider
   * classes can be removed. Specifically, classes like {@link OperationalDataStoreProvider} retain a single static
   * instance of {@link DefaultOperationalDataStore} and still require a way to reset that single instance.
   */
  void clear_ForTestsOnly();
}
