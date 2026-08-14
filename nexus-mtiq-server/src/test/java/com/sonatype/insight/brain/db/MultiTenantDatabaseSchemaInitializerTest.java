/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class MultiTenantDatabaseSchemaInitializerTest
{
  @Mock
  private DataSource testMainDataSource;

  @Test
  public void testUseCustomPopulator() {
    MultiTenantDatabaseSchemaInitializer initializer = new MultiTenantDatabaseSchemaInitializer();
    assertThat(initializer.createDatabaseSchemaPopulator(testMainDataSource, PostgresDatabaseEngine.INSTANCE, "test",
        "test")).isInstanceOf(MultiTenantDatabaseSchemaPopulator.class);
  }
}
