/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.hds.ComponentCategoryUpdater;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.version.VersionService;

import io.dropwizard.lifecycle.Managed;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

/**
 * This runs on MTIQ server boot and is responsible for setting up all things Global
 */
@Named
@Singleton
@Priority(1)
public class MultiTenantApplicationLifecycle
    extends DefaultApplicationLifecycle
    implements Managed
{
  @Inject
  public MultiTenantApplicationLifecycle(
      InsightConfig configuration,
      CLMLicenseManager licenseManager,
      DataMigrator dataMigrator,
      NewInstancePopulator newInstancePopulator,
      DefaultLicenseDataUpdater licenseDataUpdater,
      VersionService versionService,
      AuditRecorder auditRecorder,
      ComponentCategoryUpdater componentCategoryUpdater,
      TaskScheduler taskScheduler)
  {
    super(configuration, licenseManager, dataMigrator, newInstancePopulator, licenseDataUpdater, versionService,
        auditRecorder, componentCategoryUpdater, taskScheduler);
  }

  @Override
  public void boot() throws Exception {
    super.boot();

    runAsGlobal(() -> {
      new MultiTenantGlobalSchemaProtection().enableWriteProtection();
      return null;
    });
  }
}
