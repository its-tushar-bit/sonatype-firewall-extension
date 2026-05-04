/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.List;

import com.sonatype.insight.brain.common.test.InsightFixtureRule;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.brain.db.fixture.h2.H2DiskDatabaseFixture;
import com.sonatype.insight.brain.db.fixture.h2.H2InMemoryDatabaseFixture;
import com.sonatype.insight.brain.db.fixture.postgres.PostgresDatabaseFixture;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrators;
import com.sonatype.insight.brain.db.rule.DatabaseRule.DatabaseType;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.db.DatabaseConfig;

import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.getSuppressMigrations;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.isH2DiskTest;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.isH2InMemoryTest;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.isPostgresTest;

/**
 * <p>
 * Encapsulate the database test fixtures needed for IQ as a JUNit rule. The intent of the rule is to manage the
 * {@link DatabaseFixture} which is the running database itself. This rule is a singleton designed to be used with the
 * {@link DatabaseRule#getInstance(Class)} method to encapsulate that logic. Namely that the db fixture itself can keep
 * running between tests but it still allows for 'before' and 'after' logic to reset it as needed.
 * </p>
 *
 * <p>
 * The test fixtures encapsulated are:
 * <ul>
 * <li>the {@link DatabaseFixture} itself</li>
 * <li>the {@link DatabaseType} (i.e. H2 in-memory (default), H2 disk, or Postgres)</li>
 * <li>the four {@link DataStore} objects</li>
 * </ul>
 * <p>
 * For each test it will manage if a new database needs to be provisioned and the supporting {@link DataStore} classes
 * to be re-created.
 * </p>
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * @Rule(order = 1)
 * public DatabaseRule databaseRule = DatabaseRule.getInstance();
 * }
 * </pre>
 * </p>
 *
 * <p>
 * Notes:
 * <ul>
 * <li>The 'order' <strong>MUST</strong> be 1. i.e. The database must come before anything that needs a database.</li>
 * <li>The default database is an h2 in-memory database.</li>
 * <li>Use the {@link H2DiskTest} annotation for an H2 disk database. Passed to {@link H2DiskDatabaseFixture}.</li>
 * <li>Use the {@link PostgresTest} annotation for a Postgres database. Passed to {@link PostgresDatabaseFixture}.</li>
 * <li>Use the @{@link H2InMemoryTest} annotation for a custom H2 in-memory database.</li>
 * <li>Each annotation has some common options as well as some custom ones. Common options:</li>
 * <ul>
 * <li>Use the `suppressMigrations` value on the annotations to prevent automatic db migration</li>
 * <li>Use the `cleanDatabase` value on the annotations to force a new clean database to be provisioned</li>
 * </ul>
 * </ul>
 * </p>
 */
public class DatabaseRule
    extends InsightFixtureRule<DatabaseType, DatabaseFixture>
    implements DataStoreProvider
{
  private static final DatabaseRule INSTANCE = new DatabaseRule();

  private boolean suppressMigrations;

  protected OperationalDataStore operationalDataStore;

  protected AggregationDataStore aggregationDataStore;

  protected DataMartDataStore dataMartDataStore;

  protected ThirdPartyScansDataStore thirdPartyScansDataStore;

  protected DatabaseRule() {
    // private constructor for singleton enforcement
  }

  private static Class<?> currentTestClassType;

  /**
   * Return the singleton {@link DatabaseRule}
   *
   * @param baseTestClassType Any class that uses this rule to manage a database, should pass in its class type here.
   *          The value is tracked between subsequent tests and when the value changes it is considered
   *          as making the currently active database NOT reusable and therefore a fresh database will
   *          be automatically be re-provisioned
   */
  public static DatabaseRule getInstance(Class<?> baseTestClassType) {
    if (currentTestClassType != baseTestClassType) {
      INSTANCE.markFixtureAsDirty();
      currentTestClassType = baseTestClassType;
    }
    return INSTANCE;
  }

  @Override
  protected void before() throws Throwable {
    suppressMigrations = getSuppressMigrations(annotation);
    super.before();
  }

  @Override
  protected boolean getLastTestHadCustomSettings(final Annotation annotation) {
    // Track non-default annotation settings so the next test re-initializes the fixture.
    // This ensures that e.g. @PostgresTest(suppressMigrations = true) is followed by a fresh fixture.
    return DatabaseRuleAnnotations.hasNonDefaultSettings(annotation);
  }

  @Override
  protected List<Class<? extends Annotation>> getAnnotationTypes() {
    return DatabaseRuleAnnotations.ANNOTATION_TYPES;
  }

  @Override
  protected boolean getForceClean(final Annotation annotation) {
    return DatabaseRuleAnnotations.getForceClean(annotation);
  }

  @Override
  protected boolean hasAnnotation() {
    if (!DatabaseRuleAnnotations.hasAnyAnnotation(annotation)) {
      return false;
    }
    // Type changes (e.g. H2 → Postgres) are already handled by hasFixtureTypeChanged().
    // Only require re-initialization when the annotation has non-default settings
    // (suppressMigrations, cleanDatabase, customSettings, copyExistingDatabase).
    return DatabaseRuleAnnotations.hasNonDefaultSettings(annotation);
  }

  @Override
  protected DatabaseFixture createNewFixture() {
    if (type.equals(DatabaseType.POSTGRES_DB)) {
      return new PostgresDatabaseFixture(testName, DatabaseRuleAnnotations.getPostgresTest(annotation));
    }
    else if (type.equals(DatabaseType.H2_DISK_DB)) {
      return new H2DiskDatabaseFixture(DatabaseRuleAnnotations.getH2DiskTest(annotation));
    }
    else {
      return new H2InMemoryDatabaseFixture(DatabaseRuleAnnotations.getH2InMemoryTest(annotation));
    }
  }

  @Override
  protected DatabaseType getType() {
    if (isPostgresTest(annotation)) {
      return DatabaseType.POSTGRES_DB;
    }
    else if (isH2DiskTest(annotation)) {
      return DatabaseType.H2_DISK_DB;
    }
    else if (isH2InMemoryTest(annotation)) {
      return DatabaseType.H2_IN_MEMORY_DB;
    }
    else {
      return DatabaseType.H2_IN_MEMORY_DB;
    }
  }

  /**
   * Get a {@link DatabaseConfig} to access the provisioned test database
   */
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    return getDatabaseConfig(databaseName.name());
  }

  /**
   * Get a {@link DatabaseConfig} to access the provisioned test database
   */
  public DatabaseConfig getDatabaseConfig(final String databaseName) {
    return fixture.getDatabaseConfig(databaseName);
  }

  /**
   * Get a {@link DataSourceProvider} to access the provisioned test database
   */
  public DataSourceProvider getDataSourceProvider() {
    return fixture.getDataSourceProvider();
  }

  @Override
  public OperationalDataStore getOperationalDataStore() {
    return operationalDataStore;
  }

  @Override
  public AggregationDataStore getAggregationDataStore() {
    return aggregationDataStore;
  }

  @Override
  public DataMartDataStore getDataMartDataStore() {
    return dataMartDataStore;
  }

  @Override
  public ThirdPartyScansDataStore getThirdPartyScansDataStore() {
    return thirdPartyScansDataStore;
  }

  @Override
  protected void afterInitializeFixture() {
    createNewDataStores();

    // init the data stores
    operationalDataStore.initialize();
    aggregationDataStore.initialize();
    dataMartDataStore.initialize();
    thirdPartyScansDataStore.initialize();

    if (!suppressMigrations) {
      migrateDatabase();
    }
  }

  @Override
  protected void beforeCloseFixture() {
    // Close all data stores before closing the database fixture
    // This ensures EntityManagerFactory instances are properly closed, preventing JDBCConfigurationImpl memory leaks
    log.info("Closing all data stores before closing database fixture");
    closeDataStore(operationalDataStore);
    closeDataStore(aggregationDataStore);
    closeDataStore(dataMartDataStore);
    closeDataStore(thirdPartyScansDataStore);
  }

  private void closeDataStore(final DataStore dataStore) {
    if (dataStore != null) {
      try {
        dataStore.close();
      }
      catch (Exception e) {
        log.warn("Error closing data store {}: {}", dataStore.getID(), e.getMessage(), e);
      }
    }
  }

  protected void createNewDataStores() {
    this.operationalDataStore =
        new DefaultOperationalDataStore(getDataSourceProvider(), getDatabaseConfig(DatabaseName.ods));
    this.aggregationDataStore =
        new DefaultAggregationDataStore(getDataSourceProvider(), getDatabaseConfig(DatabaseName.aggregation));
    this.dataMartDataStore = new DefaultDataMartDataStore(getDataSourceProvider(), getDatabaseConfig(DatabaseName.dm));
    this.thirdPartyScansDataStore = new DefaultThirdPartyScansDataStore(getDataSourceProvider(),
        getDatabaseConfig(DatabaseName.third_party_scans));
  }

  private void migrateDatabase() {
    new DatabaseMigrators(this).runMigrators();
  }

  public String dumpSchema(final String schema) {
    return fixture.dumpSchema(schema);
  }

  public void loadSqlDump(final Path sqlFile) {
    fixture.loadSqlDump(sqlFile);
  }

  public enum DatabaseType
  {
    H2_IN_MEMORY_DB,
    H2_DISK_DB,
    POSTGRES_DB
  }
}
