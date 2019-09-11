/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.After;

import static java.util.stream.Collectors.toList;

public class AbstractSelectorTest
{
  private DataSource datasource = null;

  private Set<String> instertTables = new HashSet<>();

  @After
  public synchronized void after() throws Exception {
    for (String table : instertTables) {
      truncateTable(table);
    }
    instertTables.clear();
    DataSourceFactory.clear_ForTestsOnly();
    datasource = null;
  }

  protected void truncateTable(String table) throws Exception {
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute("TRUNCATE TABLE " + table);
    }
  }

  protected void insert(String table, String[] cols, Object[] vals) throws Exception {
    instertTables.add(table);
    insert(table, Arrays.asList(cols), Arrays.asList(vals));
  }

  protected void insert(String table, List<String> cols, List<Object> vals) throws Exception {
    List<String> quotedVals = vals.stream().map(o -> o instanceof String ? AbstractSelector.quote((String) o) : "" + o)
        .collect(toList());

    String insertsql = "INSERT INTO " + table + " (" + String.join(",", cols) + ") VALUES ("
        + String.join(",", quotedVals) + ");";

    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(insertsql);
    }
  }

  protected synchronized Connection getConnection() throws Exception {
    if (datasource == null) {
      datasource = getOdsDataSource();
    }

    Connection conn = datasource.getConnection();
    conn.setAutoCommit(true);
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("SET SCHEMA " + OperationalDataStoreProvider.ID + ";");
      stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
    }
    return conn;
  }

  private DataSource getOdsDataSource() throws Exception {
    return getOdsDataSource(true);
  }

  private DataSource getOdsDataSource(boolean inMemory) throws Exception {
    String memDb = "jdbc:h2:mem:dbutil;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE";
    String fileDb =
        "jdbc:h2:target/xyz/ods:dbutil;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE";
    DatabaseConfig odsDatabaseConfig = new DatabaseConfig();
    odsDatabaseConfig.setDriverClassName("org.h2.Driver");
    odsDatabaseConfig.setUrl(inMemory ? memDb : fileDb);
    odsDatabaseConfig.setUsername("sa");
    odsDatabaseConfig.setPassword("");
    odsDatabaseConfig.setMaxConnections(50);
    DataSource dataSource = new DataSourceFactory().newDataSource(odsDatabaseConfig, OperationalDataStoreProvider.ID);
    return dataSource;
  }
}
