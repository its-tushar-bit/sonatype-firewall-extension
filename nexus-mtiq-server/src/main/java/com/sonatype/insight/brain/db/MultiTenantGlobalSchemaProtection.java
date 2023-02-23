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
      "multi_license_license"
  );

  private static final String GLOBAL_SCHEMA_NAME = OperationalDataStoreProvider.getDatabaseSchema();

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

  private static String buildExemptTables() {
    StringBuilder stringBuilder = new StringBuilder(" AND tablename NOT LIKE 'qrtz_%' ");
    for (String table : exemptTables) {
      stringBuilder.append(" AND tablename != '" + table + "' ");
    }
    return stringBuilder.toString();
  }

  private static String SQL_QUERY =
      "DO " +
          "$loop$ " +
          "DECLARE " +
          "    r RECORD; " +
          "BEGIN " +
          "FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = '" + GLOBAL_SCHEMA_NAME + "' " +
          buildExemptTables() +
          ") " +
          "    LOOP " +
          "    EXECUTE 'create or replace trigger write_protect_trigger before insert or update or delete on ' || " +
          "quote_ident(r.tablename) || " +
          "        ' for each row execute procedure write_protect()'; " +
          "    END LOOP; " +
          "END $loop$;";

  public void enableWriteProtection() {
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.executeUpdate("SET SCHEMA '" + GLOBAL_SCHEMA_NAME + "';");
      statement.executeUpdate(CREATE_TRIGGER_FUNCTION);
      statement.executeUpdate(SQL_QUERY);
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
