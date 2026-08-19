/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PostgresDataSourceProviderTest
    extends AbstractDataSourceProviderTest
{
  @Override
  protected DataSourceProvider createTestDataSourceProvider() {
    return new PostgresDataSourceProvider();
  }

  @Override
  protected void assertEquality(final DataSource dataSource1, final DataSource dataSource2) {
    // Postgres produces a single data source
    assertThat(dataSource1).isEqualTo(dataSource2);
  }
}
