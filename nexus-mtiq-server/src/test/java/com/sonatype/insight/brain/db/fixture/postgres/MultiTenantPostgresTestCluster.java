/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.SimpleDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTenantPostgresTestCluster
    extends PostgresTestCluster
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantPostgresTestCluster.class);

  private static final String TEMPLATE_TENANT_NAME = "templatetenant";

  private static PostgresTestCluster INSTANCE;

  public static PostgresTestCluster getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MultiTenantPostgresTestCluster();
    }
    return INSTANCE;
  }

  public MultiTenantPostgresTestCluster() {
    super();
  }

  @Override
  protected void createFullyMigratedTemplateDatabase() {
    createNewDatabase(TEMPLATE_DATABASE);

    DatabaseConfig databaseConfig = getDatabaseConfig(TEMPLATE_DATABASE);

    MultiTenantPostgresDataSourceProvider dataSourceProvider =
        new MultiTenantPostgresDataSourceProvider(databaseConfig, databaseConfig);
    OperationalDataStore operationalDataStore = new MultiTenantOperationalDataStore(dataSourceProvider, databaseConfig);
    AggregationDataStore aggregationDataStore = new MultiTenantAggregationDataStore(dataSourceProvider, databaseConfig);
    DataMartDataStore dataMartDataStore = new MultiTenantDataMartDataStore(dataSourceProvider, databaseConfig);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new MultiTenantThirdPartyScansDataStore(dataSourceProvider, databaseConfig);

    DataStoreProvider dataStoreProvider = new SimpleDataStoreProvider(operationalDataStore, aggregationDataStore,
        dataMartDataStore, thirdPartyScansDataStore);

    DatabaseMigrations databaseMigrations = new DatabaseMigrations(dataStoreProvider);

    DatabaseProvisioner databaseProvisioner = new DatabaseProvisioner(dataStoreProvider, databaseMigrations);

    // Initialize and migrate the global schema first
    TenantTestHelper.testAsTenant(Tenant.GLOBAL_TENANT, x -> {
      operationalDataStore.initialize();
      aggregationDataStore.initialize();
      dataMartDataStore.initialize();
      thirdPartyScansDataStore.initialize();

      databaseProvisioner.migrateDatabase();
    });

    // run the db initialization for the template tenant
    TenantTestHelper.testAsTenantAndInvalidate(TEMPLATE_TENANT_NAME, x -> {
      operationalDataStore.initialize();
      aggregationDataStore.initialize();
      dataMartDataStore.initialize();
      thirdPartyScansDataStore.initialize();

      databaseProvisioner.migrateDatabase();
    });

    // rename the template tenant schema to not be a real tenant
    renameTemplateTenant();
  }

  private void renameTemplateTenant() {
    log.debug("Renaming template schema");
    try (Connection conn = getTestUserConnection(TEMPLATE_DATABASE);
        Statement stmt = conn.createStatement())
    {
      stmt.execute("ALTER SCHEMA t_" + TEMPLATE_TENANT_NAME + " RENAME TO " + TEMPLATE_TENANT_NAME);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not rename template schema", e);
    }
  }

  /**
   * Clone the template tenant schema into a new schema within the same database.
   * Uses pure JDBC — no external tools (pg_dump/psql) required.
   * <p>
   * Steps:
   * 1. Create the new schema
   * 2. Clone each table structure using CREATE TABLE ... (LIKE ... INCLUDING ALL)
   * 3. Copy data from each table (including schema_version which tracks migration state)
   * 4. Reset sequences to match template values
   * 5. Clone views and functions
   */
  public void cloneTenant(final String databaseName, final String databaseSchema) {
    log.info("Cloning tenant into '{}.{}'", databaseName, databaseSchema);

    try (Connection conn = getTestUserConnection(databaseName);
        Statement stmt = conn.createStatement())
    {
      // 1. Get all tables in the template schema
      List<String> tableNames = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery(
          "SELECT tablename FROM pg_tables WHERE schemaname = '" + TEMPLATE_TENANT_NAME + "' ORDER BY tablename"))
      {
        while (rs.next()) {
          tableNames.add(rs.getString(1));
        }
      }
      log.info("Found {} tables in template schema '{}': {}", tableNames.size(), TEMPLATE_TENANT_NAME, tableNames);

      // If template has no tables (e.g. suppressMigrations=true), don't create the schema —
      // let initializeDatabaseWithMigration() create both the schema and tables from scratch.
      if (tableNames.isEmpty()) {
        log.info("Template schema '{}' is empty, skipping clone", TEMPLATE_TENANT_NAME);
        return;
      }

      // 2. Create the new schema
      stmt.execute("CREATE SCHEMA " + databaseSchema);
      // 3. Clone table structure (INCLUDING ALL copies columns, defaults, constraints, indexes, sequences)
      for (String tableName : tableNames) {
        stmt.execute("CREATE TABLE " + databaseSchema + "." + tableName +
            " (LIKE " + TEMPLATE_TENANT_NAME + "." + tableName + " INCLUDING ALL)");
      }

      // 4. Copy data from all template tables (including schema_version with migration state)
      for (String tableName : tableNames) {
        stmt.execute("INSERT INTO " + databaseSchema + "." + tableName +
            " SELECT * FROM " + TEMPLATE_TENANT_NAME + "." + tableName);
      }

      // 5. Reset sequences to match template values
      // Collect sequence info first, then execute setval — avoids closing ResultSet mid-iteration.
      // JDBC spec: executing a new query on a Statement implicitly closes any open ResultSet from that Statement.
      List<String> sequenceNames = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery(
          "SELECT sequence_name FROM information_schema.sequences " +
              "WHERE sequence_schema = '" + TEMPLATE_TENANT_NAME + "' ORDER BY sequence_name"))
      {
        while (rs.next()) {
          sequenceNames.add(rs.getString(1));
        }
      }
      for (String seqName : sequenceNames) {
        long lastVal;
        try (ResultSet valRs = stmt.executeQuery(
            "SELECT last_value FROM " + TEMPLATE_TENANT_NAME + "." + seqName))
        {
          if (!valRs.next()) {
            continue;
          }
          lastVal = valRs.getLong(1);
        }
        // The new sequence was auto-created by INCLUDING ALL with a different name.
        // Find the matching sequence in the new schema.
        String newSeqName;
        try (ResultSet newSeqRs = stmt.executeQuery(
            "SELECT sequence_name FROM information_schema.sequences " +
                "WHERE sequence_schema = '" + databaseSchema + "' " +
                "AND sequence_name LIKE '%" + seqName.replace(TEMPLATE_TENANT_NAME, "") + "%' " +
                "ORDER BY sequence_name LIMIT 1"))
        {
          if (!newSeqRs.next()) {
            continue;
          }
          newSeqName = newSeqRs.getString(1);
        }
        stmt.execute("SELECT setval('" + databaseSchema + "." + newSeqName + "', " + lastVal + ")");
      }

      // 6. Clone views — collect first, then execute to avoid closing ResultSet.
      // Views may depend on other views, so create in multiple passes until all succeed.
      List<String[]> views = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery(
          "SELECT viewname, definition FROM pg_views " +
              "WHERE schemaname = '" + TEMPLATE_TENANT_NAME + "' ORDER BY viewname"))
      {
        while (rs.next()) {
          views.add(new String[]{rs.getString("viewname"), rs.getString("definition")});
        }
      }
      List<String[]> remaining = new ArrayList<>(views);
      int maxPasses = remaining.size() + 1; // at most N passes for N views
      while (!remaining.isEmpty() && maxPasses-- > 0) {
        List<String[]> failed = new ArrayList<>();
        for (String[] view : remaining) {
          String newViewDef = view[1].replace(TEMPLATE_TENANT_NAME + ".", databaseSchema + ".");
          try {
            stmt.execute("CREATE VIEW " + databaseSchema + "." + view[0] + " AS " + newViewDef);
          }
          catch (Exception e) {
            failed.add(view);
          }
        }
        if (failed.size() == remaining.size()) {
          // no progress — break to avoid infinite loop; will fail at the end if views remain
          throw new IllegalStateException("Could not create views (circular or unresolvable dependencies): " +
              failed.stream().map(v -> v[0]).reduce((a, b) -> a + ", " + b).orElse(""));
        }
        remaining = failed;
      }

      // 7. Clone functions — collect first, then execute to avoid closing ResultSet
      List<String> funcDefs = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery(
          "SELECT p.proname, pg_get_functiondef(p.oid) as funcdef " +
              "FROM pg_proc p JOIN pg_namespace n ON p.pronamespace = n.oid " +
              "WHERE n.nspname = '" + TEMPLATE_TENANT_NAME + "' ORDER BY p.proname"))
      {
        while (rs.next()) {
          funcDefs.add(rs.getString("funcdef"));
        }
      }
      for (String funcDef : funcDefs) {
        String newFuncDef = funcDef.replace(TEMPLATE_TENANT_NAME + ".", databaseSchema + ".");
        stmt.execute(newFuncDef);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not clone tenant", e);
    }
  }
}
