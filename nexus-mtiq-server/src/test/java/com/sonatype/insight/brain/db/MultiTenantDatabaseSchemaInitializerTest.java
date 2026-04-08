/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantDatabaseSchemaInitializerTest
{
  @Mock
  private DataSource testMainDataSource;

  @Test
  @Category(PostgresTestCategory.class)
  public void testUseCustomPopulator() {
    MultiTenantDatabaseSchemaInitializer initializer = new MultiTenantDatabaseSchemaInitializer();
    assertThat(initializer.createDatabaseSchemaPopulator(testMainDataSource, PostgresDatabaseEngine.INSTANCE, "test",
        "test")).isInstanceOf(MultiTenantDatabaseSchemaPopulator.class);
  }
}
