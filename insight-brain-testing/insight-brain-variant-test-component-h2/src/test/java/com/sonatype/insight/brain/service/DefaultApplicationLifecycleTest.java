/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

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
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.version.VersionService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.sonatype.licensing.LicensingException;

@ComponentH2Test
public class DefaultApplicationLifecycleTest
    extends AbstractComponentH2Test
{
  @Inject
  private InsightConfig config;

  @Mock
  private DataMigrator dataMigrator;

  @Mock
  private CLMLicenseManager licenseManager;

  @Mock
  private DefaultLicenseDataUpdater licenseDataUpdater;

  @Mock
  private NewInstancePopulator newInstancePopulator;

  @Mock
  private VersionService versionService;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private AuditSession auditSession;

  @Mock
  private ComponentCategoryUpdater componentCategoryUpdater;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private ComponentCategoryDAO componentCategoryDAO;

  @Mock
  private LicenseDAO licenseDAO;

  @Mock
  private MultiLicenseDAO multiLicenseDAO;

  private DefaultApplicationLifecycle lifecycle;

  @BeforeEach
  public void setUpLifecycle() {
    lenient().when(auditRecorder.recordSystemEvent(any())).thenReturn(auditSession);
    lenient().when(versionService.getLogDisplayVersion()).thenReturn("test-version");
    lenient().when(versionService.getBuild()).thenReturn("test-build");

    lifecycle = new DefaultApplicationLifecycle(
        config,
        licenseManager,
        dataMigrator,
        newInstancePopulator,
        licenseDataUpdater,
        versionService,
        auditRecorder,
        componentCategoryUpdater,
        taskScheduler,
        componentCategoryDAO,
        licenseDAO,
        multiLicenseDAO);
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // noop - this test does not exercise LTG behavior
  }

  @Test
  public void testBoot_MigrateProxyConfigurationBeforeLicenseRegistrationWithHds() throws Exception {
    lifecycle.boot();

    InOrder inOrder = inOrder(dataMigrator, taskScheduler, licenseManager);
    inOrder.verify(dataMigrator).migrate();
    inOrder.verify(taskScheduler).initialize();
    inOrder.verify(licenseManager).loadLicense();
  }

  @Test
  public void testBoot_ContinueUponLicenseInstallationError() throws Exception {
    config.setLicenseFile("uninstallable.lic");
    doThrow(LicensingException.class).when(licenseManager).installLicenseIfUnlicensed(config.getLicenseFile());

    lifecycle.boot();

    verify(licenseManager).installLicenseIfUnlicensed(config.getLicenseFile());
  }
}
