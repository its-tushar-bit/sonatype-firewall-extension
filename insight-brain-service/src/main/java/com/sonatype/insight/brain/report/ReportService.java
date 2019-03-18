/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
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

  private final InsightConfig insightConfig;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ReportService(InsightWork work,
                       ReportDownloader reportDownloader,
                       PolicyEvaluationDAO policyEvaluationDAO,
                       InsightConfig insightConfig,
                       ApplicationDAO applicationDAO)
  {
    this.work = work;
    this.reportDownloader = reportDownloader;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.insightConfig = insightConfig;
    this.applicationDAO = applicationDAO;
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
          throw new NotFoundException("The report for application ID " + appId + " and scan ID " + scanId
              + " does not exist. Usually this means the report was deemed obsolete"
              + " according to the data retention policies and hence purged to the trash.");
        }
        // 0 indicates no retries
        int reportTimeoutInSeconds = 0;
        if (waitForReport) {
          reportTimeoutInSeconds = insightConfig.getReportTimeoutInSeconds();
        }
        final File tempFile = FileUtils.createTempFile("temp-", ".zip", reportFile.getParentFile());
        if (!reportDownloader.downloadReport(scanId, tempFile, reportTimeoutInSeconds, 5)) {
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

      Report.applyChanges(application, reportFile);

      MODIFICATION_COUNTS.put(appId + '-' + scanId, newCount);
    }
  }

  public static void flushReportChanges(final String appId, final String scanId) {
    MODIFICATION_COUNTS.remove(appId + '-' + scanId);
  }

  public static void flushReportChanges() {
    MODIFICATION_COUNTS.clear();
  }

  @Authorize(permission = Permission.READ)
  ReportMetadataDTO getReportMetadata(
      final @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      final String scanId) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    ReportMetadataDTO metadata = new ReportMetadataDTO();
    metadata.setApplication(application);

    File reportFile = fetchReport(work, application.getId(), scanId, false);
    final ContainerNode<?> data = JsonUtils.parse(Report.getEntry(reportFile, "data.json").buf);
    metadata.setExpandedCoverage(data.path("globals").path("expandedCoverage").booleanValue());

    if (metadata.isExpandedCoverage()) {
      metadata.setReportTime(new Date(data.path("globals").path("currentDate").longValue()));
      metadata.setReportTitle("Expanded Coverage Report");
    }
    else {
      PolicyEvaluation evaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(application.getId(),
          scanId);
      metadata.setReportTime(evaluation.getTime());
      metadata.setReportTitle(StageTypes.getById(evaluation.getStageTypeId()).getName() + " Report");
    }
    return metadata;
  }

  /**
   * Prepares the report for an expanded coverage scan to be available when the customer loads it in a browser.
   * It waits for the report to become available on the HDS.
   * 
   * @since 1.37
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  void prepareExpandedCoverageReport(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
                                     String scanId)
      throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    fetchReport(work, application.getId(), scanId, true /* waitForReport */);
  }
}
