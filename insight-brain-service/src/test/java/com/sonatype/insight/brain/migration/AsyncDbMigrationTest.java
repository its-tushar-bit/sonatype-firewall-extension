/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncDbMigrationTest
    extends AbstractComponentTest
{
  private MigrationTrackerDAO migrationTrackerDAO;

  private TestAsyncDbMigration<Application> underTest;

  private ApplicationDAO applicationDAO;

  private Organization testOrganization;

  @Mock
  private InsightConfig insightConfig;

  @Before
  public void setup() {
    testOrganization = tempEntity.newOrganization();

    // policyDAO is populated and used for the test migration
    applicationDAO = daoFactory.createApplicationDAO();
    migrationTrackerDAO = daoFactory.createMigrationTrackerDAO();
  }

  @Test
  public void testMigration() {
    when(insightConfig.isDatabaseEmbedded()).thenReturn(true);

    underTest = spy(new TestAsyncDbMigration<>(applicationDAO, migrationTrackerDAO, "test DB Migration", insightConfig,
        "TestMigration", 1));

    createApplication();

    // Ensure the migration tracker does not exist
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    migrationTrackerDAO.delete(migrationTracker);
    assertThat(migrationTracker).isNull();

    underTest.runMigration();
    verify(underTest).onStart();
    verify(underTest).migrate(eq(applicationDAO), any(Application.class), any(TransactionContext.class));
    verify(underTest).onCompletion();

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigration_numberOfRecordsAreTwicePageSize() {
    int pageSize = 2;
    underTest = spy(new TestAsyncDbMigration<>(applicationDAO, migrationTrackerDAO, "test DB Migration", pageSize,
        "TestMigration", 1));

    for (int i = 0; i < pageSize * 2; i++) {
      createApplication();
    }

    // Ensure the migration tracker does not exist
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(migrationTracker).isNull();

    underTest.runMigration();
    verify(underTest).onStart();
    verify(underTest, times(pageSize * 2)).migrate(eq(applicationDAO), any(Application.class),
        any(TransactionContext.class));
    verify(underTest).onCompletion();

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigration_doesNotRun_whenTrackerExists() {
    underTest = spy(new TestAsyncDbMigration<>(applicationDAO, migrationTrackerDAO, "test DB Migration", insightConfig,
        "TestMigration", 1));

    createApplication();

    migrationTrackerDAO.insertTracker(underTest.getMigrationName());

    underTest.runMigration();

    verify(underTest, never()).onStart();
    verify(underTest, never()).migrate(eq(applicationDAO), any(Application.class), any(TransactionContext.class));
    verify(underTest, never()).onCompletion();
  }

  @Test
  public void testMigration_compareTo() {
    TestAsyncDbMigration<Application> asyncDbMigrationOne =
        new TestAsyncDbMigration<>(applicationDAO, migrationTrackerDAO, "test DB Migration", insightConfig,
            "MigrationOne", 1);
    TestAsyncDbMigration<Application> asyncDbMigrationTwo =
        new TestAsyncDbMigration<>(applicationDAO, migrationTrackerDAO, "test DB Migration", insightConfig,
            "MigrationTwo", 2);
    TestAsyncDbMigration<Application> asyncDbMigrationThree =
        new TestAsyncDbMigration<>(applicationDAO, migrationTrackerDAO, "test DB Migration", insightConfig,
            "MigrationThree", 1);

    // Test that migrations are sorted by priority first
    assertThat(asyncDbMigrationOne.compareTo(asyncDbMigrationTwo)).isLessThan(0);
    assertThat(asyncDbMigrationTwo.compareTo(asyncDbMigrationOne)).isGreaterThan(0);

    // Test that migrations with the same priority are sorted by name
    assertThat(asyncDbMigrationOne.compareTo(asyncDbMigrationThree)).isLessThan(0);
    assertThat(asyncDbMigrationThree.compareTo(asyncDbMigrationOne)).isGreaterThan(0);
  }

  private void createApplication() {
    tempEntity.newApplication(testOrganization.getId());
  }
}
