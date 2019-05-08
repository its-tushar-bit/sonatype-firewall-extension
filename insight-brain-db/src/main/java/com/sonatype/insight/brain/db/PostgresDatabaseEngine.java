/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.db.DatabaseEngine;

public class PostgresDatabaseEngine
    extends DatabaseEngine
{
  public static final DatabaseEngine INSTANCE = new PostgresDatabaseEngine();

  private PostgresDatabaseEngine() {
    // hide
  }

  @Override
  public String getId() {
    return "postgresql";
  }

  @Override
  public String buildSetSchemaSql(String schemaName) {
    return "SET SCHEMA '" + schemaName + "'";
  }
}
