/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Test;

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
