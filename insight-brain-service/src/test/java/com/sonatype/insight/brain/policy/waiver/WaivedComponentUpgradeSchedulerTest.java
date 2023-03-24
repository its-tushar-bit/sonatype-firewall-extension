/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WaivedComponentUpgradeSchedulerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private Configuration configuration;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private WaivedComponentUpgradeTask waivedComponentUpgradeTask;

  private WaivedComponentUpgradeScheduler scheduler;

  public WaivedComponentUpgradeSchedulerTest() {
    super(WaivedComponentUpgradeScheduler.class);
  }

  @Before
  public void before() {
    scheduler = new WaivedComponentUpgradeScheduler(configuration, taskSchedulerMock, organizationDAO,
        waivedComponentUpgradeTask);
  }

  @Test
  public void testSchedulerCanScheduleTask() {
    Organization rootOrgMock = new Organization();
    rootOrgMock.setWaivedComponentUpgradeStageTypeId(Stage.ID_STAGE_RELEASE);

    scheduler.disableForTesting = false;
    when(organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenReturn(rootOrgMock);
    when(configuration.getWaivedComponentUpgradeInspectionHour()).thenReturn(0);
    scheduler.scheduleWaivedComponentUpgradeInspection();

    // We expect the message that the task is getting scheduled but not verifying configured time to not create
    // more complicated mock configurations
    assertThatLogMessagesContain(
        info("Next waived component upgrade inspection execution scheduled for null"));
  }

  @Test
  public void testSchedulerCannotScheduleTask_missingStage() {
    Organization rootOrgMock = new Organization();
    rootOrgMock.setWaivedComponentUpgradeStageTypeId(null);

    scheduler.disableForTesting = false;
    when(organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenReturn(rootOrgMock);
    when(configuration.getWaivedComponentUpgradeInspectionHour()).thenReturn(0);

    scheduler.scheduleWaivedComponentUpgradeInspection();

    assertThatLogMessagesContain(info("Waived component upgrade task not configured"));
  }

  @Test
  public void testSchedulerCannotScheduleTask_missingHourConfiguration() {
    scheduler.disableForTesting = false;
    // Organization mock is not required because code will not reach the associated assertion, which triggers a test
    // failure in the mockito runner for code cleanup, but it's an assumption that should not be made for testing
    when(configuration.getWaivedComponentUpgradeInspectionHour()).thenReturn(null);

    scheduler.scheduleWaivedComponentUpgradeInspection();

    assertThatLogMessagesContain(info("Waived component upgrade task not configured"));
  }

  @Test
  public void testSchedulerCannotScheduleTask_disabledForTesting() {
    scheduler.disableForTesting = true;

    scheduler.scheduleWaivedComponentUpgradeInspection();

    assertThatLogMessagesContain(info("Waived component upgrade task not configured"));
  }

  @Test
  public void testWaivedComponentUpgradeNotificationStageUpdated_nullStage() {
    WaivedComponentUpgradeScheduler waivedComponentUpgradeSchedulerSpy = spy(scheduler);

    waivedComponentUpgradeSchedulerSpy.waivedComponentUpgradeNotificationStageUpdated(null);
    verify(waivedComponentUpgradeSchedulerSpy).deregister();
    verify(waivedComponentUpgradeSchedulerSpy, never()).scheduleWaivedComponentUpgradeInspection();
  }

  @Test
  public void testWaivedComponentUpgradeNotificationStageUpdated_nonNullStage() {
    WaivedComponentUpgradeScheduler waivedComponentUpgradeSchedulerSpy = spy(scheduler);

    waivedComponentUpgradeSchedulerSpy.waivedComponentUpgradeNotificationStageUpdated(Stage.ID_DEVELOP);
    verify(waivedComponentUpgradeSchedulerSpy, never()).deregister();
    verify(waivedComponentUpgradeSchedulerSpy).scheduleWaivedComponentUpgradeInspection();
  }
}
