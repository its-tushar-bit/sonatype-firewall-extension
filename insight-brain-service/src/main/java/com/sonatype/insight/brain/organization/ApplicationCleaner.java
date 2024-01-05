/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
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

  @Inject
  public ApplicationCleaner(
      final InsightWork work,
      final FileCleaner fileCleaner,
      final OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator,
      final ApplicationDAO applicationDAO)
  {
    this.work = work;
    this.fileCleaner = fileCleaner;
    this.ownerMaintenanceTelemetryCreator = ownerMaintenanceTelemetryCreator;
    this.applicationDAO = applicationDAO;
  }

  public void delete(final TransactionContext tx, final Application application) throws IOException {
    fileCleaner.delete(work.getScanDir(application.getId()));
    fileCleaner.delete(work.getAuditDir(application.getId()));
    fileCleaner.delete(work.getReportDir(application.getId()));
    fileCleaner.delete(work.getSourceControlDir(application.getId()));
    File applicationIconDirectory = new File(work.getApplicationIconDir(), application.getId());
    try {
      fileCleaner.delete(applicationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete application icons: {}", applicationIconDirectory, e);
    }

    // delete application last, this way the operation can be retried later if anything goes wrong
    applicationDAO.delete(tx, application);

    ownerMaintenanceTelemetryCreator.sendOwnerMaintenanceTelemetry(application,
        OwnerMaintenanceTelemetry.TYPE_DELETE);
  }
}
