/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;

/**
 * This class is responsible for enabling write protection of identified tables for the global tenant schema. To ensure
 * a correct deployment whereby some tables may have previously been protected but now require removal of that
 * protection, firstly, write protection is removed from all tables and then the protection is re-applied to those still
 * requiring it. This alternative approach would be to consider the tables individually and potentially having a rolling
 * migration similar to the other tenants migration requirements. This is not necessary at this point.
 */
@Named
@Singleton
public class MultiTenantGlobalSchemaProtection
{
  /* These tables have been identified as the only tables that should be writeable under the global schema
  all other tables raise an exception if any INSERT, UPDATE or DELETE is attempted.
   */
  private static final List<String> exemptTables = Arrays.asList(
      "schema_version",
      "system_configuration_property",
      "firewall_ignore_patterns",
      "lock",
      "perpetual_lock", // Used by scm
      "migration_tracker",
      "product_license",
      "component_category",
      "license",
      "multi_license",
      "multi_license_license",
      "mail_configuration",
      "deleted_tenant"
  );

  private static final String GLOBAL_SCHEMA_NAME = Tenant.GLOBAL_TENANT.databaseSchema;

  private static final String WRITE_PROTECT_TRIGGER_NAME = "write_protect_trigger";

  private static final String LOOP_CONTROL = "$loop$ ";

  private static final String DECLARE = "DECLARE ";

  private static final String RECORD = "    r RECORD; ";

  private static final String BEGIN = "BEGIN ";

  private static final String FOR_R_IN = "FOR r IN";

  private static final String SELECT_PG_TABLES = "SELECT tablename FROM pg_tables";

  private static final String SELECT_PG_TABLES_FOR_SCHEMA = "SELECT tablename FROM pg_tables WHERE schemaname =";

  private static final String LOOP = "    LOOP ";

  private static final String END_LOOP = "    END LOOP; ";

  private static final String END_LOOP_CONTROL = "END $loop$;";

  private static final String SET_SCHEMA = "SET SCHEMA";

  private static final String CREATE_TRIGGER_FUNCTION =
      "create or replace function write_protect()" +
          "    returns trigger " +
          "as " +
          "$func$ " +
          "    begin " +
          "        raise exception '" + GLOBAL_SCHEMA_NAME + " write protection'; " +
          "    end; " +
          "$func$ " +
          "language plpgsql;";

  private final OperationalDataStore operationalDataStore;

  @Inject
  public MultiTenantGlobalSchemaProtection(final OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
  }

  private static String buildExemptTables() {
    StringBuilder stringBuilder = new StringBuilder(" AND tablename NOT LIKE 'qrtz_%' ");
    for (String table : exemptTables) {
      stringBuilder.append(" AND tablename != '" + table + "' ");
    }
    return stringBuilder.toString();
  }

  private static String SQL_DROP_PROTECTION_QUERY =
      "DO " +
          LOOP_CONTROL +
          DECLARE +
          RECORD +
          BEGIN +
          FOR_R_IN + " (" + SELECT_PG_TABLES + ") " +
          LOOP +
          "    EXECUTE 'drop trigger if exists " + WRITE_PROTECT_TRIGGER_NAME + " on ' || " +
          "quote_ident(r.tablename) || ';';" +
          END_LOOP +
          END_LOOP_CONTROL;

  private static String SQL_CREATE_PROTECTION_QUERY =
      "DO " +
          LOOP_CONTROL +
          DECLARE +
          RECORD +
          BEGIN +
          FOR_R_IN + " (" + SELECT_PG_TABLES_FOR_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "' " +
          buildExemptTables() +
          ") " +
          LOOP +
          "    EXECUTE 'create or replace trigger " + WRITE_PROTECT_TRIGGER_NAME +
          " before insert or update or delete on ' || quote_ident(r.tablename) || " +
          "        ' for each row execute procedure write_protect()'; " +
          END_LOOP +
          END_LOOP_CONTROL;

  private static String SQL_ENABLE_PROTECTION_QUERY =
      "DO " +
          LOOP_CONTROL +
          DECLARE +
          RECORD +
          BEGIN +
          FOR_R_IN + " (" + SELECT_PG_TABLES_FOR_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "' " +
          buildExemptTables() +
          ") " +
          LOOP +
          "    EXECUTE 'alter table ' || quote_ident(r.tablename) || ' enable trigger  " +
          WRITE_PROTECT_TRIGGER_NAME + "'; " +
          END_LOOP +
          END_LOOP_CONTROL;

  private static String SQL_DISABLE_PROTECTION_QUERY =
      "DO " +
          LOOP_CONTROL +
          DECLARE +
          RECORD +
          BEGIN +
          FOR_R_IN + " (" + SELECT_PG_TABLES_FOR_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "' " +
          buildExemptTables() +
          ") " +
          LOOP +
          "    EXECUTE 'alter table ' || quote_ident(r.tablename) || ' disable trigger  " +
          WRITE_PROTECT_TRIGGER_NAME + "'; " +
          END_LOOP +
          END_LOOP_CONTROL;

  public void createWriteProtection() {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.executeUpdate(SET_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "';");
      statement.executeUpdate(CREATE_TRIGGER_FUNCTION);
      statement.executeUpdate(SQL_DROP_PROTECTION_QUERY);
      statement.executeUpdate(SQL_CREATE_PROTECTION_QUERY);
    }
    catch (SQLException e) {
      throw new RuntimeException("Error trying to create write protection for MultiTenant Global schema.", e);
    }
  }

  public void enableWriteProtection() {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.executeUpdate(SET_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "';");
      statement.executeUpdate(SQL_ENABLE_PROTECTION_QUERY);
    }
    catch (SQLException e) {
      throw new RuntimeException("Error trying to enable write protection for MultiTenant Global schema.", e);
    }
  }

  public void disableWriteProtection() {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.executeUpdate(SET_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "';");
      statement.executeUpdate(SQL_DISABLE_PROTECTION_QUERY);
    }
    catch (SQLException e) {
      throw new RuntimeException("Error trying to disable write protection for MultiTenant Global schema.", e);
    }
  }
}
