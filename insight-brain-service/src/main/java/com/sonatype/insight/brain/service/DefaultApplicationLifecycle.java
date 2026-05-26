/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.AbstractComponentCategoryUpdater;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentCategoryUpdater;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.version.VersionService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;
import org.springframework.context.annotation.DependsOn;

@Named
@Singleton
@DependsOn("staticInjectionInitializer")
public class DefaultApplicationLifecycle
    extends ApplicationLifecycle
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(DefaultApplicationLifecycle.class);

  private final InsightConfig configuration;

  private final CLMLicenseManager licenseManager;

  private final DataMigrator dataMigrator;

  private final NewInstancePopulator newInstancePopulator;

  private final LicenseDataUpdater licenseDataUpdater;

  private final VersionService versionService;

  private final AuditRecorder auditRecorder;

  private final ComponentCategoryUpdater componentCategoryUpdater;

  private final TaskScheduler taskScheduler;

  private final ComponentCategoryDAO componentCategoryDAO;

  private final LicenseDAO licenseDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private long startTime;

  @Inject
  public DefaultApplicationLifecycle(
      InsightConfig configuration,
      CLMLicenseManager licenseManager,
      DataMigrator dataMigrator,
      NewInstancePopulator newInstancePopulator,
      DefaultLicenseDataUpdater licenseDataUpdater,
      VersionService versionService,
      AuditRecorder auditRecorder,
      ComponentCategoryUpdater componentCategoryUpdater,
      TaskScheduler taskScheduler,
      ComponentCategoryDAO componentCategoryDAO,
      LicenseDAO licenseDAO,
      MultiLicenseDAO multiLicenseDAO)
  {
    this.configuration = configuration;
    this.licenseManager = licenseManager;
    this.dataMigrator = dataMigrator;
    this.newInstancePopulator = newInstancePopulator;
    this.licenseDataUpdater = licenseDataUpdater;
    this.versionService = versionService;
    this.auditRecorder = auditRecorder;
    this.componentCategoryUpdater = componentCategoryUpdater;
    this.taskScheduler = taskScheduler;
    this.componentCategoryDAO = componentCategoryDAO;
    this.licenseDAO = licenseDAO;
    this.multiLicenseDAO = multiLicenseDAO;
  }

  /**
   * Runs the boot sequence at startup.
   * The bridge {@link Managed} interface routes Spring's {@code afterPropertiesSet()} here,
   * ensuring the boot sequence runs during context initialization, before
   * {@link DefaultTenantManagedInitializer} which depends on this bean.
   */
  @Override
  public void start() throws Exception {
    boot();
  }

  @Override
  public void boot() throws Exception {
    startTime = System.currentTimeMillis();
    logServerInstanceMessage("Starting");
    auditServerLifecycle(AuditEvent.START_SERVER);

    dataMigrator.migrate();

    taskScheduler.initialize();

    loadIqLicense();

    LicenseDataUpdater.setUpdater(licenseDataUpdater);

    // This call must come after the DataMigrator. Specifically, the RootOrganizationConfigMigrator as the sample data
    // will interfere with its decision to determine a fresh install and mistakenly trigger the root org migration.
    newInstancePopulator.populateIfNewInstance();

    new Thread("Startup license data updater")
    {
      @Override
      public void run() {
        try {
          LicenseDataUpdater.update(licenseDAO, multiLicenseDAO);
        }
        catch (Exception e) {
          log.info("Failed to retrieve license data from Sonatype HDS", log.isDebugEnabled() ? e : null);
        }
      }
    }.start();

    AbstractComponentCategoryUpdater.setUpdater(componentCategoryUpdater);
    new Thread("Startup component category updater")
    {
      @Override
      public void run() {
        try {
          AbstractComponentCategoryUpdater.update(componentCategoryDAO);
        }
        catch (Exception e) {
          log.info("Failed to retrieve component categories from Sonatype HDS", log.isDebugEnabled() ? e : null);
        }
      }
    }.start();
  }

  void loadIqLicense() {
    licenseManager.loadLicense();
    // If a license is not installed and the config has a license file path, then try to install it from there.
    try {
      licenseManager.installLicenseIfUnlicensed(configuration.getLicenseFile());
    }
    catch (Exception e) {
      log.warn("The license {} could not be installed", configuration.getLicenseFile(), e);
    }
  }

  private void auditServerLifecycle(final AuditEvent auditEvent) {
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(auditEvent)) {
      AuditData.get()
          .setData("serverInstanceId", ApplicationLifecycle.getServerInstanceId())
          .setData("serverConfigurationFile", ApplicationLifecycle.getConfigFile())
          .setData("serverRelease", versionService.getLogDisplayVersion())
          .setData("serverBuild", versionService.getBuild())
          .setData("processOwner", System.getProperty("user.name"));
    }
  }

  @Override
  public void stop() throws Exception {
    String formattedStartTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    logServerInstanceMessage("Stopping", " Uptime: " + formattedStartTime);
    auditServerLifecycle(AuditEvent.STOP_SERVER);
  }

  private void logServerInstanceMessage(String action) {
    logServerInstanceMessage(action, "");
  }

  private void logServerInstanceMessage(String action, String suffix) {
    String version = versionService.getLogDisplayVersion();
    String message = action + " Nexus IQ Server 1 release " + version +
        " instance ID " + ApplicationLifecycle.getServerInstanceId() +
        " on " + ApplicationLifecycle.getLocalHostString() + "." + suffix;
    // Log to stdout first because the standard logging may not be operational at this point.
    System.out.println(message);
    log.info(message);
  }

}
