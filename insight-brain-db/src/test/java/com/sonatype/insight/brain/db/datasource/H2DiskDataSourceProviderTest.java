/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class H2DiskDataSourceProviderTest
    extends AbstractDataSourceProviderTest
{
  @Override
  protected DataSourceProvider createTestDataSourceProvider() {
    return new H2DiskDataSourceProvider();
  }

  @Override
  protected void assertEquality(final DataSource dataSource1, final DataSource dataSource2) {
    // H2 produces a data source for each data store
    assertThat(dataSource1).isNotEqualTo(dataSource2);
  }
}
