/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Removes an {@link Application} along with related configuration and data.
 *
 * @since 1.9
 */
@Named
public class ApplicationCleaner
{
  private final ApplicationDAO applicationDAO;

  private final InsightWork work;

  private final FileCleaner fileCleaner;

  @Inject
  public ApplicationCleaner(final InsightWork work, final FileCleaner fileCleaner) {
    this.work = work;
    this.fileCleaner = fileCleaner;
    applicationDAO = new ApplicationDAO();
  }

  public void delete(final TransactionContext tx, final Application application) throws IOException {
    fileCleaner.delete(work.getScanDir(application.getId()));
    fileCleaner.delete(work.getAuditDir(application.getId()));
    fileCleaner.delete(work.getReportDir(application.getId()));

    // delete application last, this way the operation can be retried later if anything goes wrong
    applicationDAO.deleteWithIcon(tx, application, work.getApplicationIconDir());
  }
}
