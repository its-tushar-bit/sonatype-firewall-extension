/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.model.HasStringId;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;

@ComponentH2Test
public class AsyncDbMigrationSchedulerTest
    extends AbstractComponentH2Test
{
  @Mock
  private TaskScheduler taskScheduler;

  private AsyncDbMigrationScheduler underTest;

  private AbstractAsyncDbMigration asyncDbMigrationOne;

  private AbstractAsyncDbMigration asyncDbMigrationTwo;

  private AbstractAsyncDbMigration asyncDbMigrationThree;

  @BeforeEach
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
  public void execute() {
    underTest.execute(mock(JobExecutionContext.class));

    assertMigrationsRunInPriorityOrder();
  }

  @Test
  public void executeForTenant() {
    underTest.executeForTenant(mock(JobExecutionContext.class), mock(Tenant.class));

    assertMigrationsRunInPriorityOrder();
  }

  private void assertMigrationsRunInPriorityOrder() {
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
