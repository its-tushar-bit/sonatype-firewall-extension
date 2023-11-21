/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Before;
import org.junit.Test;

public class SourceControlUserActivityPostgresqlDAOTest
    extends SourceControlUserActivityDAOTest
{
  @Before
  @Override
  public void setup() {
    // DO NOTHING as we need this initialization in the context of the postgres container
  }

  @Override
  @Test
  public void testInsertAllIfNew_onlyNewActivities() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.setup();
      super.testInsertAllIfNew_onlyNewActivities();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testInsertAllIfNew_someActivityExists_notFailAndIgnore() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.setup();
      super.testInsertAllIfNew_someActivityExists_notFailAndIgnore();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testDeleteBySourceControlUserId() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.setup();
      super.testDeleteBySourceControlUserId();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testUpdateActivitiesSentToTelemetry() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.setup();
      super.testUpdateActivitiesSentToTelemetry();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Override
  @Test
  public void testGetActivitiesNotSentToTelemetry() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      super.setup();
      super.testGetActivitiesNotSentToTelemetry();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
