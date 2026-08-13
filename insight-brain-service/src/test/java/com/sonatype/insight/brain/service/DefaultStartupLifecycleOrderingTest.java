/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentCategoryUpdater;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.version.VersionService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DefaultStartupLifecycleOrderingTest
{
  @Test
  public void shouldCompleteApplicationBootBeforeStartingSchedulerAndRegisteringTenantManagedJobs() throws Exception {
    List<String> events = new ArrayList<>();

    DataMigrator dataMigrator = mock(DataMigrator.class);
    doAnswer(invocation -> record(events, "migrate")).when(dataMigrator).migrate();

    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    doAnswer(invocation -> record(events, "taskScheduler.initialize")).when(taskScheduler).initialize();
    doAnswer(invocation -> record(events, "taskScheduler.start")).when(taskScheduler).start();

    CLMLicenseManager licenseManager = mock(CLMLicenseManager.class);
    doAnswer(invocation -> record(events, "license.load")).when(licenseManager).loadLicense();

    NewInstancePopulator newInstancePopulator = mock(NewInstancePopulator.class);
    doAnswer(invocation -> record(events, "newInstance.populate"))
        .when(newInstancePopulator)
        .populateIfNewInstance();

    TenantManaged tenantManaged = mock(TenantManaged.class);
    doAnswer(invocation -> record(events, "tenantManaged.register")).when(tenantManaged).register();

    AuditRecorder auditRecorder = mock(AuditRecorder.class);
    AuditSession auditSession = mock(AuditSession.class);
    when(auditRecorder.recordSystemEvent(any())).thenReturn(auditSession);

    VersionService versionService = mock(VersionService.class);
    when(versionService.getLogDisplayVersion()).thenReturn("test-version");
    when(versionService.getBuild()).thenReturn("test-build");

    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.registerBean("staticInjectionInitializer", Object.class, Object::new);
      context.registerBean(InsightConfig.class, InsightConfig::new);
      context.registerBean(CLMLicenseManager.class, () -> licenseManager);
      context.registerBean(DataMigrator.class, () -> dataMigrator);
      context.registerBean(NewInstancePopulator.class, () -> newInstancePopulator);
      context.registerBean(DefaultLicenseDataUpdater.class, () -> mock(DefaultLicenseDataUpdater.class));
      context.registerBean(VersionService.class, () -> versionService);
      context.registerBean(AuditRecorder.class, () -> auditRecorder);
      context.registerBean(ComponentCategoryUpdater.class, () -> mock(ComponentCategoryUpdater.class));
      context.registerBean("taskScheduler", TaskScheduler.class, () -> taskScheduler);
      context.registerBean(ComponentCategoryDAO.class, () -> mock(ComponentCategoryDAO.class));
      context.registerBean(LicenseDAO.class, () -> mock(LicenseDAO.class));
      context.registerBean(MultiLicenseDAO.class, () -> mock(MultiLicenseDAO.class));
      context.registerBean(TenantManaged.class, () -> tenantManaged);
      context.register(DefaultApplicationLifecycle.class, DefaultTenantManagedInitializer.class);

      context.refresh();

      assertThat(events).containsExactly(
          "migrate",
          "taskScheduler.initialize",
          "license.load",
          "newInstance.populate",
          "taskScheduler.start",
          "tenantManaged.register");
    }
  }

  private static Object record(final List<String> events, final String event) {
    events.add(event);
    return null;
  }
}
