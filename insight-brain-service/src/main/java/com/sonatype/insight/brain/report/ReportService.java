/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheBuilder;
import org.codehaus.plexus.util.FileUtils;

@Named
public class ReportService
{
  private final InsightWork work;

  private final ReportDownloader reportDownloader;

  static final ConcurrentMap<String, Integer> MODIFICATION_COUNTS = CacheBuilder.newBuilder().maximumSize(8192)
      .<String, Integer> build().asMap();

  private static final ConcurrentMap<String, Lock> LOCK_TABLE = CacheBuilder.newBuilder().weakValues()
      .<String, Lock> build().asMap();

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public ReportService(InsightWork work, ReportDownloader reportDownloader, PolicyEvaluationDAO policyEvaluationDAO) {
    this.work = work;
    this.reportDownloader = reportDownloader;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  public File fetchReport(final InsightWork work, final String appId, final String scanId, final boolean waitForReport)
      throws IOException
  {
    final File reportFile = work.getReportFile(appId, scanId);
    final Lock lock = lockFor(appId, scanId);
    if (waitForReport || reportFile.exists()) {
      // protect against concurrent download as well as concurrent editing of the report
      lock.lock();
    }
    else if (!lock.tryLock()) {
      throw new NotFoundException("The report for scan ID " + scanId + " is still being downloaded");
    }
    try {
      if (!reportFile.exists()) {
        if (policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId) != null) {
          throw new IllegalStateException(
              "The report file does not exist for application ID " + appId + " and scan ID " + scanId + ".");
        }
        int attempts = 0;
        int interval = 0;

        if (waitForReport) {
          attempts = 30;
          interval = 30;
        }
        final File tempFile = FileUtils.createTempFile("temp-", ".zip", reportFile.getParentFile());
        if (!reportDownloader.downloadReport(scanId, tempFile, attempts, interval)) {
          throw new NotFoundException("Could not download the report for scan ID " + scanId);
        }
        FileUtils.rename(tempFile, reportFile);
      }

      applyChanges(appId, scanId, reportFile);

      return reportFile;
    }
    finally {
      lock.unlock();
    }
  }

  private static Lock lockFor(final String appId, final String scanId) {
    Lock lock = LOCK_TABLE.get(appId + '-' + scanId);
    if (lock == null) {
      final Lock newLock = new ReentrantLock();
      lock = LOCK_TABLE.putIfAbsent(appId + '-' + scanId, newLock);
      if (lock == null) {
        lock = newLock;
      }
    }
    return lock;
  }

  public File getReport(final InsightWork work, final String appId, final String scanId) throws IOException {
    File reportFile = work.getReportFile(appId, scanId);
    if (!reportFile.exists()) {
      return null;
    }

    // protect against concurrent editing of the report
    final Lock lock = lockFor(appId, scanId);
    lock.lock();
    try {
      applyChanges(appId, scanId, reportFile);

      return reportFile;
    }
    finally {
      lock.unlock();
    }
  }

  private void applyChanges(String appId, String scanId, File reportFile) throws IOException {
    final File appAuditDir = work.getAuditDir(appId);
    int newCount = JsonUtils.fileStore(appAuditDir).modificationCount();
    Application application = new ApplicationDAO().getByIdNotNull(appId);
    File orgAuditDir = work.getAuditDir(application.getOrganizationId());
    newCount += JsonUtils.fileStore(orgAuditDir).modificationCount();
    final Integer oldCount = MODIFICATION_COUNTS.get(appId + '-' + scanId);

    if (oldCount == null || oldCount < newCount) {
      Report.deletePdf(reportFile);

      Report.applyChanges(application, reportFile, appAuditDir);

      MODIFICATION_COUNTS.put(appId + '-' + scanId, newCount);
    }
  }

  public static void flushReportChanges(final String appId, final String scanId) {
    MODIFICATION_COUNTS.remove(appId + '-' + scanId);
  }

  public static void flushReportChanges() {
    MODIFICATION_COUNTS.clear();
  }
}
