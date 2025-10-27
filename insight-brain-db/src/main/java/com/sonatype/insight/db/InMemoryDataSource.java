/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class InMemoryDataSource
    implements DataSource
{
  private final DataSource wrappedDataSource;

  private String setSchemaStatement;

  public InMemoryDataSource(DataSource wrappedDataSource, String setSchemaStatement) {
    this.wrappedDataSource = wrappedDataSource;
    this.setSchemaStatement = setSchemaStatement;
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return wrappedDataSource.getLogWriter();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return wrappedDataSource.unwrap(iface);
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    wrappedDataSource.setLogWriter(out);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return wrappedDataSource.isWrapperFor(iface);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    wrappedDataSource.setLoginTimeout(seconds);
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection conn = wrappedDataSource.getConnection();
    setSchema(conn);
    return conn;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection conn = wrappedDataSource.getConnection(username, password);
    setSchema(conn);
    return conn;
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return wrappedDataSource.getLoginTimeout();
  }

  // only here for compatibility with JRE 1.7 whose CommonDataSource demands this method
  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  private void setSchema(final Connection conn) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(setSchemaStatement)) {
      stmt.execute();
    }
  }
}
