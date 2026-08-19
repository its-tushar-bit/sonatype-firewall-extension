/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.jupiter.api.Test;

/**
 * PostgreSQL-backed re-runs of the {@link SourceControlUserDAOTest} cases relocated from insight-brain-data
 * (CLM-45228). The H2 coverage stays in {@link SourceControlUserDAOTest}; these {@code @PostgresTest}
 * overrides live here so this module keeps a single (Postgres) DatabaseRule fixture type per JVM.
 */
public class SourceControlUserPostgresqlDAOTest
    extends SourceControlUserDAOTest
{
  @Override
  @Test
  @PostgresTest
  public void testGetByApplicationId() {
    super.testGetByApplicationId();
  }

  @Override
  @Test
  @PostgresTest
  public void testGetUserIdByEmailFilteringByApplicationId() {
    super.testGetUserIdByEmailFilteringByApplicationId();
  }

  @Override
  @Test
  @PostgresTest
  public void testInsertAllIfNew_onlyNewUsers() {
    super.testInsertAllIfNew_onlyNewUsers();
  }

  @Override
  @Test
  @PostgresTest
  public void testInsertAllIfNew_someUserExists_notFailAndIgnore() {
    super.testInsertAllIfNew_someUserExists_notFailAndIgnore();
  }

  @Override
  @Test
  @PostgresTest
  public void testDelete_CascadeToSourceControlUserActivity() {
    super.testDelete_CascadeToSourceControlUserActivity();
  }
}
