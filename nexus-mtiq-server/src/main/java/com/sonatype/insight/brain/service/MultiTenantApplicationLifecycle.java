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
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
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
@Priority(MultiTenantApplicationLifecycle.PRIORITY)
public class MultiTenantApplicationLifecycle
    extends DefaultApplicationLifecycle
    implements Managed
{
  public static final int PRIORITY = 1;

  private final MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection;

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
      TaskScheduler taskScheduler,
      ComponentCategoryDAO componentCategoryDAO,
      LicenseDAO licenseDAO,
      MultiLicenseDAO multiLicenseDAO,
      MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection)
  {
    super(configuration, licenseManager, dataMigrator, newInstancePopulator, licenseDataUpdater, versionService,
        auditRecorder, componentCategoryUpdater, taskScheduler, componentCategoryDAO, licenseDAO, multiLicenseDAO);
    this.multiTenantGlobalSchemaProtection = multiTenantGlobalSchemaProtection;
  }

  @Override
  public void boot() throws Exception {
    super.boot();

    runAsGlobal(() -> {
      multiTenantGlobalSchemaProtection.createWriteProtection();
      return null;
    });
  }
}
