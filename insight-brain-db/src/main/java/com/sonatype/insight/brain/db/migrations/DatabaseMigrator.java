/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

public interface DatabaseMigrator
{
  String SCHEMA_MIGRATION_ENABLED = "SCHEMA_MIGRATION_ENABLED";

  void migrate();

  void validateMinimumSchemaVersion();
}
