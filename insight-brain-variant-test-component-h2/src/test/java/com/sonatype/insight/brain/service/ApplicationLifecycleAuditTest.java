/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
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
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class ApplicationLifecycleAuditTest
    extends AbstractComponentH2Test
{
  @TempDir
  public File testTempDir;

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
  private ComponentCategoryUpdater componentCategoryUpdater;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private ComponentCategoryDAO componentCategoryDAO;

  @Mock
  private LicenseDAO licenseDAO;

  @Mock
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private InsightConfig config;

  private DefaultApplicationLifecycle lifecycle;

  private File configFile;

  private File originalConfigFile;

  private final Map<String, Object> capturedAuditData = new LinkedHashMap<>();

  @BeforeEach
  public void before() throws IOException {
    capturedAuditData.clear();
    originalConfigFile = ApplicationLifecycle.getConfigFile();
    configFile = new File(testTempDir, "config.yml");
    configFile.createNewFile();
    ApplicationLifecycle.setConfigFile(configFile);

    when(auditRecorder.recordSystemEvent(any())).thenAnswer(invocation -> {
      CapturingAuditData auditData = new CapturingAuditData(capturedAuditData);
      return new AuditSession(auditData);
    });
    when(versionService.getLogDisplayVersion()).thenReturn("test-version");
    when(versionService.getBuild()).thenReturn("test-build");

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

  @AfterEach
  public void after() {
    ApplicationLifecycle.setConfigFile(originalConfigFile);
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // noop - this test does not exercise LTG behavior
  }

  @Test
  public void testBoot() throws Exception {
    lifecycle.boot();

    ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditRecorder).recordSystemEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isEqualTo(AuditEvent.START_SERVER);

    assertLifecycleAuditData();
  }

  @Test
  public void testStop() throws Exception {
    lifecycle.stop();

    ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditRecorder).recordSystemEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isEqualTo(AuditEvent.STOP_SERVER);

    assertLifecycleAuditData();
  }

  private void assertLifecycleAuditData() {
    assertThat(capturedAuditData)
        .containsEntry("serverInstanceId", ApplicationLifecycle.getServerInstanceId())
        .containsEntry("serverConfigurationFile", configFile)
        .containsEntry("serverRelease", "test-version")
        .containsEntry("serverBuild", "test-build")
        .containsEntry("processOwner", System.getProperty("user.name"));
  }

  private static class CapturingAuditData
      extends AuditData
  {
    private final Map<String, Object> data;

    CapturingAuditData(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public AuditData setData(String key, Object value) {
      data.put(key, value);
      return this;
    }

    @Override
    protected AuditData forSubEvent(AuditEvent event, boolean independent, boolean system) {
      return this;
    }

    @Override
    protected <F> F continueAsync(java.util.function.Function<AuditData, F> taskSubmitter) {
      return taskSubmitter.apply(this);
    }

    @Override
    protected void commit() {
    }

    @Override
    public void commitSubEvents() {
    }

    @Override
    public void setUsername(String username) {
    }

    @Override
    public AuditEvent getEvent() {
      return null;
    }

    @Override
    public void setEvent(AuditEvent event) {
    }

    @Override
    public void setError(String error) {
    }

    @Override
    public void setException(Throwable error) {
    }

    @Override
    public void setHttpStatus(int httpStatus) {
    }
  }
}
