/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes an {@link Application} along with related configuration and data.
 *
 * @since 1.9
 */
@Named
public class ApplicationCleaner
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationCleaner.class);

  private final InsightWork work;

  private final FileCleaner fileCleaner;

  private final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator;

  private final ApplicationDAO applicationDAO;

  private final LifecycleReportPersistenceService lifecycleReportPersistenceService;

  private final ScanPersistenceService scanPersistenceService;

  private final SbomPersistenceService sbomPersistenceService;

  private final GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  public ApplicationCleaner(
      final InsightWork work,
      final FileCleaner fileCleaner,
      final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator,
      final ApplicationDAO applicationDAO,
      final LifecycleReportPersistenceService lifecycleReportPersistenceService,
      final ScanPersistenceService scanPersistenceService,
      final SbomPersistenceService sbomPersistenceService,
      final GitHubAppDeletionService gitHubAppDeletionService)
  {
    this.work = work;
    this.fileCleaner = fileCleaner;
    this.ownerMaintenanceTelemetryCreator = ownerMaintenanceTelemetryCreator;
    this.applicationDAO = applicationDAO;
    this.lifecycleReportPersistenceService = lifecycleReportPersistenceService;
    this.scanPersistenceService = scanPersistenceService;
    this.sbomPersistenceService = sbomPersistenceService;
    this.gitHubAppDeletionService = gitHubAppDeletionService;
  }

  public void delete(final TransactionContext tx, final Application application) throws IOException {
    fileCleaner.delete(work.getAuditDir(application.getId()));
    fileCleaner.delete(work.getSourceControlDir(application.getId()));

    scanPersistenceService.deleteScansFor(application.getId());
    sbomPersistenceService.deleteSbomsFor(application.getId());
    lifecycleReportPersistenceService.deleteReports(application.getId());

    File applicationIconDirectory = new File(work.getApplicationIconDir(), application.getId());
    try {
      fileCleaner.delete(applicationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete application icons: {}", applicationIconDirectory, e);
    }

    gitHubAppDeletionService.deactivateGitHubApps(tx, application.getId());

    // delete application last, this way the operation can be retried later if anything goes wrong
    applicationDAO.delete(tx, application);

    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(application,
        OwnerMaintenanceTelemetry.TYPE_DELETE);
  }
}
