/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.PostIncrementalMigrator;

/**
 * Dummy impl to allow tests to process schema_incremental_0115.cls.
 */
public class PolicyViolationMigrator
    implements PostIncrementalMigrator
{
  @Override
  public void migrate(final DataSource dataSource, final String databaseSchema) throws Exception {
  }
}
