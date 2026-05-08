/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.PrintWriter;
import java.util.Set;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.model.HasStringId;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AsyncDbMigrationSchedulerTest
    extends AbstractComponentTest
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private PrintWriter mockPrintWriter;

  private AsyncDbMigrationScheduler underTest;

  private AbstractAsyncDbMigration asyncDbMigrationOne;

  private AbstractAsyncDbMigration asyncDbMigrationTwo;

  private AbstractAsyncDbMigration asyncDbMigrationThree;

  @Before
  public void setup() {
    asyncDbMigrationOne = spy(new TestEmptyAsyncDbMigration("migrationOne", Integer.MAX_VALUE));
    asyncDbMigrationTwo = spy(new TestEmptyAsyncDbMigration("migrationTwo", 2));
    asyncDbMigrationThree = spy(new TestEmptyAsyncDbMigration("migrationThree", 1));

    Set<AbstractAsyncDbMigration> mockAsyncDbMigrations =
        Set.of(asyncDbMigrationOne, asyncDbMigrationTwo, asyncDbMigrationThree);
    underTest = new AsyncDbMigrationScheduler(mockAsyncDbMigrations, taskScheduler);
  }

  @Test
  public void testSchedulesJob_onRegister() {
    underTest.register();

    verify(taskScheduler).scheduleOneTimeTask(underTest);
  }

  @Test
  public void execute() throws Exception {
    underTest.execute(null, mockPrintWriter);

    verify(taskScheduler).scheduleOneTimeTask(underTest);
  }

  @Test
  public void executeForTenant() {
    underTest.executeForTenant(mock(JobExecutionContext.class), mock(Tenant.class));

    InOrder inOrder = inOrder(asyncDbMigrationThree, asyncDbMigrationTwo, asyncDbMigrationOne);
    inOrder.verify(asyncDbMigrationThree).runMigration();
    inOrder.verify(asyncDbMigrationTwo).runMigration();
    inOrder.verify(asyncDbMigrationOne).runMigration();
  }

  private static class TestEmptyAsyncDbMigration
      extends TestAsyncDbMigration<HasStringId>
  {
    protected TestEmptyAsyncDbMigration(
        String migrationName,
        int migrationPriority)
    {
      super(null, null, "type", 0, migrationName, migrationPriority);
    }

    @Override
    public void runMigration() {
      // No-op, just to test the migration with the spy
    }
  }
}
