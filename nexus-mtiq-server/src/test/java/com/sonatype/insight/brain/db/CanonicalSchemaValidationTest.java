/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * Tests to ensure there is no schema drift introduced by migrations. Meaning that an existing IQ database that is
 * migrated to current schema using the incremental scripts <strong>MATCHES EXACTLY</strong> a schema for a new IQ
 * database.
 * </p>
 *
 * <h1>tl;dr</h1>
 * <ol>
 * <li>The canonical schema (i.e. the one true schema) is defined as the current contents of the four `schema.sql`
 * files.</li>
 * <li>
 * The <strong>base</strong> canonical schema, from which we will be applying the incremental scripts is defined
 * as the `schema.sql` files as of
 * <a href="https://github.com/sonatype/insight-brain/commit/3098e603667094c408ff6150b463a3fa31e2a42e">commit
 * 3098e60</a>
 * on June 26th, 2023.
 * </li>
 * <li>
 * These tests verify that a database from the original base canonical schema from commit 3098e60, with migrations
 * applied, <strong>MUST EXACTLY MATCH</strong> the current canonical schema.
 * </li>
 * </ol>
 *
 * <h1>Background</h1>
 * <p>
 * For the SaaS world and for MTIQ in particular, there are big challenges with DB migrations, most of which are
 * described in
 * <a href="https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations">
 * SaaS Friendly IQ Database Migrations<a/>. To handle these challenges, troubleshooting tools and techniques become
 * critical to be able to detect and fix any migrations problems on the contexts for MTIQ, so we have documented some of
 * those tools and techniques in
 * <a href="https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/79626270/Addressing+Database+Migration+Failures">
 * Addressing Database Migration Failures<a/>. That page describes a key aspect which is having a consistent schema for
 * tenants so we can easily spot differences in a reliable way. For example to know if a particular migration script was
 * executed on a tenant but not in another tenant.
 * </p>
 * <br/>
 * <p>
 * The tests in this class will help ensure we maintain a consistent schema. This means we try to ensure that the schema
 * of a new tenant will be the same as the one for an old tenant that is being migrated using the incremental scripts.
 * We do this by defining a base canonical schema, which is a snapshot of a schema for a tenant, in particular, the
 * canonical
 * schema was obtained from
 * <a href="https://github.com/sonatype/insight-brain/commit/3098e603667094c408ff6150b463a3fa31e2a42e">commit
 * 3098e60</a>.
 * The precise schema versions for the different data stores are:
 * <table border="1">
 * <tr>
 * <td>Data Store</td>
 * <td>Schema Version</td>
 * </tr>
 * <tr>
 * <td>insight_brain_third_party_scans</td>
 * <td>13</td>
 * </tr>
 * <tr>
 * <td>insight_brain_aggregation</td>
 * <td>13</td>
 * </tr>
 * <tr>
 * <td>insight_brain_ods</td>
 * <td>303</td>
 * </tr>
 * </table>
 * The idea is to use this base canonical schema to run the migrations over it and confirm there are no differences
 * between a tenant that is being migrated to the latest version and a new tenant created with the latest schema.sql
 * init scripts.
 * </p>
 * <br/>
 * <strong>Notes:</strong>
 * <ul>
 * <li>The canonical schemas are the <strong>src/main/resources/db/insight_brain-&#42;/schema.sql</strong> files.</li>
 * <li>The base canonical schema is the
 * <strong>src/test/resources/CanonicalSchemaValidationTest/canonical-schema-from-commit-3098e6.sql</strong> file.</li>
 * <li>
 * Keep in mind the canonical schema from commit 3098e6 schema should not be modified unless there is a good reason
 * for that. Over this schema we will apply the different incremental scripts to move an older tenant to the latest
 * schema version.
 * </li>
 * </ul>
 */
@Ignore("CLM-39891: Fix embedded-postgres schema validation after testcontainers removal")
@Category(SlowTest.class)
public class CanonicalSchemaValidationTest
    extends AbstractMultiTenantDatabaseTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  /**
   * This is a basic test to ensure that schema creation for new tenants is consistent, that means that for any new
   * tenants that are provisioned using the same IQ version, their schemas should be the same.
   */
  @Test
  public void testNewTenants_shouldHaveTheSameSQLDump() {
    Tenant tenant1 = provisionTestTenant();
    Tenant tenant2 = provisionTestTenant();

    assertTenantsSchemasAreTheExpected(tenant1, tenant2, (schema1, schema2) -> {
      assertThat(schema1).isEqualTo(schema2);
    });
  }

  /**
   * With this test we want to confirm that there are no differences between the schemas of a new tenant, and a tenant
   * that is being migrated to the latest schema version. Consider that new tenants are always created using the
   * schema.sql files, while a tenant that is being migrated, will check its current schema version, and will apply any
   * pending migration script that is higher that the current version. Note: We use the canonical schema from commit
   * 3098e6 as the base schema to apply all the incremental scripts.
   */
  @Test
  public void testMigratedTenantAndNewTenant_shouldHaveTheSameSQLDump() {
    Tenant newTenant = provisionTestTenant();

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      loadSQLDumpIntoSchema("canonical-schema-from-commit-3098e6.sql", tenant.databaseSchema);

      migrateTenant();

      assertTenantsSchemasAreTheExpected(newTenant, tenant, (schema1, schema2) -> {
        assertThat(schema1).as("The database schemas did not exactly match. " +
            "Verify that the migration SQL is written such that a migrated database exactly matches a new database")
            .isEqualTo(schema2);
      });
    });
  }

  /**
   * This is a check test to ensure we can detect a difference between tenant schemas. We are using a manipulated base
   * schema to ensure we always have a difference.
   */
  @Test
  public void testMigratedTenantWithDriftBaseSchemaAndNewTenant_shouldHaveDifferentSQLDumps() {
    Tenant newTenant = provisionTestTenant();

    TenantTestHelper.testAsNewTenant(testName, tenant -> {
      loadSQLDumpIntoSchema("modified-base-schema-with-differences.sql", tenant.databaseSchema);

      migrateTenant();

      assertTenantsSchemasAreTheExpected(newTenant, tenant, (schema1, schema2) -> {
        assertThat(schema1).isNotEqualTo(schema2);
      });
    });
  }

  private void migrateTenant() {
    DatabaseProvisioner databaseProvisioner = databaseRule.getDatabaseContainer().getDatabaseProvisioner();
    databaseProvisioner.initializeDatabaseWithoutMigration();
    new DatabaseMigrations(databaseRule).migrateDatabase();
  }

  private void loadSQLDumpIntoSchema(String dumpFile, String schema) throws IOException {
    File finalSQLFile = tempDir.newFile();
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/" + dumpFile);

    if (Objects.isNull(resource)) {
      throw new RuntimeException(String.format("Failed to load dump file: %s", dumpFile));
    }

    String sqlDump = FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
    FileUtils.write(finalSQLFile, sqlDump.replaceAll("t_TENANT", schema), StandardCharsets.UTF_8);

    loadSqlDump(Paths.get(finalSQLFile.toURI()));
  }

  private void assertTenantsSchemasAreTheExpected(
      final Tenant tenant1,
      final Tenant tenant2,
      BiConsumer<String, String> resultAssertion)
  {
    String tenant1Schema =
        dumpSchemaWithGenericTenantName(tenant1.databaseSchema);
    String tenant2Schema =
        dumpSchemaWithGenericTenantName(tenant2.databaseSchema);

    assertThat(tenant1Schema).isNotNull();
    assertThat(tenant2Schema).isNotNull();
    resultAssertion.accept(tenant1Schema, tenant2Schema);
  }

  private String dumpSchemaWithGenericTenantName(String schema) {
    return dumpSchema(schema).replaceAll(schema, "t_TENANT");
  }
}
