/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;

import javax.inject.Inject;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.version.DefaultVersionService;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

@Category(SlowTest.class)
public class ApplicationLifecycleAuditTest
    extends AbstractComponentAuditTest
{
  @Inject
  private DefaultApplicationLifecycle lifecycle;

  private File configFile;

  private File originalConfigFile;

  @Before
  public void before() throws Exception {
    originalConfigFile = InsightBrainService.getConfigFile();
    configFile = tempDir.newFile("config.yml");
    InsightBrainService.setConfigFile(configFile);
  }

  @After
  public void after() {
    InsightBrainService.setConfigFile(originalConfigFile);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(DataMigrator.class).toInstance(mock(DataMigrator.class));
    binder.bind(DefaultLicenseDataUpdater.class).toInstance(mock(DefaultLicenseDataUpdater.class));
    binder.bind(NewInstancePopulator.class).toInstance(mock(NewInstancePopulator.class));
    super.configure(binder);
  }

  @Test
  public void testBoot() throws Exception {
    lifecycle.boot();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.START_SERVER, null, SYSTEM_USER);
    assertLifecycleAuditData(auditDTO);
  }

  @Test
  public void testStop() throws Exception {
    lifecycle.stop();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.STOP_SERVER, null, SYSTEM_USER);
    assertLifecycleAuditData(auditDTO);
  }

  private void assertLifecycleAuditData(final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "serverInstanceId", InsightBrainService.getInstanceId());
    assertCustomData(auditDTO, "serverConfigurationFile", configFile.toString());
    assertCustomData(auditDTO, "serverRelease", new DefaultVersionService().getLogDisplayVersion());
    assertCustomData(auditDTO, "processOwner", System.getProperty("user.name"));
  }
}
