/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater.LicenseData;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultLicenseDataUpdaterUnitTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private DefaultLicenseDataUpdater defaultLicenseDataUpdater;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Test
  public void testDoUpdate_Schedules_LoadLicenses_OnAllOtherNodes() {
    LicenseData licenseData = new LicenseData();
    licenseData.licenses = Collections.emptySet();
    licenseData.multiLicenses = Collections.emptySet();
    licenseData.multiLicenseMappings = Collections.emptyMap();
    when(mockHdsClient.get(LicenseData.class, DefaultLicenseDataUpdater.HDS_LICENSE_PATH, null)).thenReturn(
        licenseData);

    defaultLicenseDataUpdater.doUpdate();

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(defaultLicenseDataUpdater);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(DefaultLicenseDataUpdater.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() throws Exception {
    DefaultLicenseDataUpdater spyDefaultLicenseDataUpdater = spy(defaultLicenseDataUpdater);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spyDefaultLicenseDataUpdater).doLoadLicenses();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyDefaultLicenseDataUpdater.execute(mock(JobExecutionContext.class));
    }

    verify(spyDefaultLicenseDataUpdater).doLoadLicenses();
  }

  @Test
  public void testDoLoadLicenses() {
    try (MockedStatic<LicenseDataUpdater> mockLicenseDataUpdater = Mockito.mockStatic(LicenseDataUpdater.class)) {
      defaultLicenseDataUpdater.doLoadLicenses();

      mockLicenseDataUpdater.verify(LicenseDataUpdater::loadLicenses);
    }
  }
}
