/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Map;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.brain.db.fixture.h2.H2DiskDatabaseFixture;
import com.sonatype.insight.brain.db.fixture.h2.H2InMemoryDatabaseFixture;
import com.sonatype.insight.brain.db.fixture.postgres.PostgresDatabaseFixture;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseName;
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
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.getCleanDatabase;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.getSuppressMigrations;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.isH2DiskTest;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.isH2InMemoryTest;
import static com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.isPostgresTest;

/**
 * <p>
 * Encapsulate the database test fixtures needed for IQ as a JUNit rule. The intent of the rule is to mange the
 * {@link DatabaseFixture} which is the running database itself. This rule is a singleton designed to be used with the
 * {@link DatabaseRule#getInstance(Class)} method to encapsulate that logic. Namely that the db fixture itself can keep
 * running between tests but it still allows for 'before' and 'after' logic to reset it as needed.
 * </p>
 *
 * <p>
 * The test fixtures encapsulated are:
 * <ul>
 *   <li>the {@link DatabaseFixture} itself </li>
 *   <li>the {@link DatabaseType} (i.e. H2 in-memory (default), H2 disk, or Postgres)</li>
 *   <li>the four {@link DataStore} objects</li>
 * </ul>
 * <p>
 * For each test it will manage if a new database needs to be provisioned and the supporting {@link DataStore} classes
 * to be re-created.
 * </p>
 *
 * <p>
 * Example:
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
 *   <li>The 'order' <strong>MUST</strong> be 1. i.e. The database must come before anything that needs a database.</li>
 *   <li>The default database is an h2 in-memory database.</li>
 *   <li>Use the {@link H2DiskTest} annotation for an H2 disk database. Passed to {@link H2DiskDatabaseFixture}.</li>
 *   <li>Use the {@link PostgresTest} annotation for a Postgres database. Passed to {@link PostgresDatabaseFixture}.</li>
 *   <li>Use the @{@link H2InMemoryTest} annotation for a custom H2 in-memory database.</li>
 *   <li>Each annotation has some common options as well as some custom ones. Common options:</li>
 *   <ul>
 *     <li>Use the `suppressMigrations` value on the annotations to prevent automatic db migration</li>
 *     <li>Use the `cleanDatabase` value on the annotations to force a new clean database to be provisioned</li>
 *   </ul>
 * </ul>
 * </p>
 */
public class DatabaseRule
    extends ExternalResource
    implements DataStoreProvider
{
  protected static final Logger log = LoggerFactory.getLogger(DatabaseRule.class);

  private static final DatabaseRule INSTANCE = new DatabaseRule();

  protected DatabaseFixture databaseFixture;

  private DatabaseType databaseType;

  private boolean suppressMigrations;

  private boolean cleanDatabase;

  private DatabaseType previousDatabaseType;

  protected OperationalDataStore operationalDataStore;

  protected AggregationDataStore aggregationDataStore;

  protected DataMartDataStore dataMartDataStore;

  protected ThirdPartyScansDataStore thirdPartyScansDataStore;

  // Track if the current database fixture is brand new for the current test
  // - True if the database fixture was just (re-)initialized during the current single test
  // - False if the database fixture has remained the same compared to the previous test
  private boolean isNewDatabaseFixtureForCurrentTest;

  // A test can mark the current database as dirty meaning it needs to be closed and a fresh one created
  private boolean isCurrentDatabaseDirty = true;

  private boolean lastTestHadCustomSettings = false;

  protected Annotation annotation;

  protected volatile String testName;

  protected DatabaseRule() {
    // private constructor for singleton enforcement
  }

  private static Class<?> currentTestClassType;

  /**
   * Return the singleton {@link DatabaseRule}
   *
   * @param baseTestClassType Any class that uses this rule to manage a database, should pass in its class type here.
   *                          The value is tracked between subsequent tests and when the value changes it is considered
   *                          as making the currently active database NOT reusable and therefore a fresh database will
   *                          be automatically be re-provisioned
   */
  public static DatabaseRule getInstance(Class<?> baseTestClassType) {
    if (currentTestClassType != baseTestClassType) {
      INSTANCE.markDatabaseAsDirty();
      currentTestClassType = baseTestClassType;
    }
    return INSTANCE;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    // grab the annotation for this test
    annotation = DatabaseRuleAnnotations.getAnnotation(description);
    testName = description.getMethodName();

    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    // get the current database type, if defined, for the current method under test
    databaseType = getDatabaseType();
    suppressMigrations = getSuppressMigrations(annotation);
    cleanDatabase = getCleanDatabase(annotation);

    if (needsDatabaseInitialization()) {
      log.info("(Re)initializing test database");

      initializeDatabaseFixture();

      initializeDataStores();
    }
  }

  @Override
  protected void after() {
    isNewDatabaseFixtureForCurrentTest = false;

    // after each test method is complete, mark the database type that was used for it
    previousDatabaseType = databaseType;

    lastTestHadCustomSettings = DatabaseRuleAnnotations.hasCustomSettings(annotation);

    if (databaseFixture != null && !databaseFixture.isFixtureReusable()) {
      closePreviousFixture();
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
    return databaseFixture.getDatabaseConfig(databaseName);
  }

  /**
   * Checks if the current test needs to re-initialize the databases
   */
  private boolean needsDatabaseInitialization() {
    if (hasDatabaseTypeChanged()) {
      log.info("Database type has changed from '{}' to '{}'. Need to re-initialize database.", previousDatabaseType,
          databaseType);
      return true;
    }

    // If any db annotation (@H2DiskTest, @H2InMemoryTest, @PostgresTest) exists it means that the database needs to
    // be re-provisioned.
    if (hasAnnotation()) {
      log.info("Current test is using custom database configuration. Need to re-initialize database.");
      return true;
    }

    if (lastTestHadCustomSettings()) {
      log.info("Last test had `customSettings`. Need to re-initialize database.");

      // reset it
      lastTestHadCustomSettings = false;
      return true;
    }

    if (cleanDatabase) {
      log.info("Clean database requested. Need to re-initialize database.");
      return true;
    }

    if (isCurrentDatabaseDirty) {
      log.info("Database marked as dirty. Need to re-initialize database.");
      return true;
    }

    return false;
  }

  private boolean hasAnnotation() {
    return DatabaseRuleAnnotations.hasAnyAnnotation(annotation);
  }

  private boolean lastTestHadCustomSettings() {
    return lastTestHadCustomSettings;
  }

  /**
   * Has the database type changed between the previous test and current test
   */
  public boolean hasDatabaseTypeChanged() {
    return previousDatabaseType != databaseType;
  }

  /**
   * Get a {@link DataSourceProvider} to access the provisioned test database
   */
  public DataSourceProvider getDataSourceProvider() {
    return databaseFixture.getDataSourceProvider();
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

  private void initializeDatabaseFixture() {
    // close the previous database fixture if necessary
    if (previousDatabaseType != null) {
      closePreviousFixture();
    }

    initializeNewDatabaseFixture();
    isCurrentDatabaseDirty = false;
  }

  private void closePreviousFixture() {
    if (databaseFixture != null) {
      try {
        databaseFixture.close();
      }
      catch (Exception e) {
        throw new RuntimeException("Unable to close previous database fixture", e);
      }
    }
  }

  private void initializeDataStores() {
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
    new DatabaseMigrator(this).migrate();
  }

  private void initializeNewDatabaseFixture() {
    databaseFixture = createNewDatabaseFixture();
    isNewDatabaseFixtureForCurrentTest = true;
  }

  protected DatabaseFixture createNewDatabaseFixture() {
    log.info("Creating new database fixture: " + databaseType);
    if (databaseType.equals(DatabaseType.POSTGRES_DB)) {
      return new PostgresDatabaseFixture(testName, DatabaseRuleAnnotations.getPostgresTest(annotation));
    }
    else if (databaseType.equals(DatabaseType.H2_DISK_DB)) {
      return new H2DiskDatabaseFixture(DatabaseRuleAnnotations.getH2DiskTest(annotation));
    }
    else {
      return new H2InMemoryDatabaseFixture(DatabaseRuleAnnotations.getH2InMemoryTest(annotation));
    }
  }

  public Map<String, Object> getDatabaseMetadata() {
    return databaseFixture.getDatabaseMetadata();
  }

  /**
   * Special test method which forces a shutdown of the database. Only use if you test requires it. Will nuke the
   * database fixture so no other calls to it can be made, and a new fixture will be provisioned for the next test.
   */
  public void shutdown() throws Exception {
    databaseFixture.close();
    databaseFixture = null;
  }

  public boolean isDatabaseFixtureReusable() {
    // Not reusable if this is a new database fixture for the current test
    return !isNewDatabaseFixtureForCurrentTest;
  }

  public String dumpSchema(final String schema) {
    return databaseFixture.dumpSchema(schema);
  }

  public void loadSqlDump(final Path sqlFile) {
    databaseFixture.loadSqlDump(sqlFile);
  }

  /**
   * Any test that fudges the database should mark it as dirty so the next test will get a cleanly provisioned db
   */
  public void markDatabaseAsDirty() {
    isCurrentDatabaseDirty = true;
  }

  protected DatabaseType getDatabaseType() {
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

  protected enum DatabaseType
  {
    H2_IN_MEMORY_DB, H2_DISK_DB, POSTGRES_DB
  }
}
