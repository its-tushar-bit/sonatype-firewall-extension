/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SloViolationIndexAsyncDbMigrationTest
    extends AbstractComponentTest
{
  @Inject
  protected MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  protected SloViolationIndexAsyncDbMigration underTest;

  @Before
  public void setup() {
    migrationTrackerDAO.deleteById(underTest.getMigrationName());
  }

  @Test
  public void testRunMigration_completesSuccessfully() {
    underTest.runMigration();

    MigrationTracker tracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(tracker).isNotNull();
  }

  @Test
  public void testRunMigration_skipsWhenTrackerExists() {
    migrationTrackerDAO.insertTracker(underTest.getMigrationName());

    SloViolationIndexAsyncDbMigration spied = spy(underTest);
    spied.runMigration();

    verify(spied, never()).onStart();
  }
}
