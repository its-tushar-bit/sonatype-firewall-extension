/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater.LicenseData;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

@ComponentH2Test
public class DefaultLicenseDataUpdaterUnitTest
    extends AbstractComponentH2Test
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  private LicenseDAO licenseDAO;

  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private DefaultLicenseDataUpdater defaultLicenseDataUpdater;

  @BeforeEach
  public void setUpDaoSpies() {
    licenseDAO = spy(lookup(LicenseDAO.class));
    multiLicenseDAO = spy(lookup(MultiLicenseDAO.class));
    applyBeanFieldOverride(DefaultLicenseDataUpdater.class, "licenseDAO", licenseDAO);
    applyBeanFieldOverride(DefaultLicenseDataUpdater.class, "multiLicenseDAO", multiLicenseDAO);
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
    defaultLicenseDataUpdater.doLoadLicenses();

    verify(licenseDAO).load();
    verify(multiLicenseDAO).load();
  }
}
