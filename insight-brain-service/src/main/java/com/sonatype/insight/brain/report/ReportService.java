/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
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

  private static final ConcurrentMap<String, Lock> LOCK_TABLE = CacheBuilder.newBuilder().weakValues()
      .<String, Lock> build().asMap();

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final InsightConfig insightConfig;

  private final ApplicationDAO applicationDAO;

  private final ApplicationAdapter applicationAdapter;

  @Inject
  public ReportService(
      InsightWork work,
      ReportDownloader reportDownloader,
      PolicyEvaluationDAO policyEvaluationDAO,
      InsightConfig insightConfig,
      ApplicationDAO applicationDAO,
      ApplicationAdapter applicationAdapter)
  {
    this.work = work;
    this.reportDownloader = reportDownloader;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.insightConfig = insightConfig;
    this.applicationDAO = applicationDAO;
    this.applicationAdapter = applicationAdapter;
  }

  public File fetchReport(final InsightWork work, final Application app, final String scanId)
      throws IOException
  {
    String appId = app.getId();
    final File reportFile = work.getReportFile(appId, scanId);
    final Lock lock = lockFor(appId, scanId);
    lock.lock();
    try {
      if (!reportFile.exists()) {
        int reportTimeoutInSeconds = insightConfig.getReportTimeoutInSeconds();
        final File tempFile = FileUtils.createTempFile("temp-", ".zip", reportFile.getParentFile());
        if (!reportDownloader.downloadReport(scanId, tempFile, reportTimeoutInSeconds, 5)) {
          throw new NotFoundException("Could not download the report for scan ID " + scanId);
        }
        FileUtils.rename(tempFile, reportFile);
      }

      Report.applyChanges(app, reportFile);

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

  public File getReport(final InsightWork work, final String appId, final String scanId) {
    File reportFile = work.getReportFile(appId, scanId);
    if (reportFile.exists()) {
      return reportFile;
    }

    if (policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId) != null) {
      throw new NotFoundException("The report for application ID " + appId + " and scan ID " + scanId
          + " does not exist. Usually this means the report was deemed obsolete"
          + " according to the data retention policies and hence purged to the trash.");
    }
    throw new NotFoundException("Could not find a report with ID " + scanId);
  }

  @Authorize(permission = Permission.READ)
  public ReportMetadataDTO getReportMetadata(
      final @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      final String scanId) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    ReportMetadataDTO metadata = new ReportMetadataDTO();
    metadata.setApplication(application);

    File reportFile = getReport(work, application.getId(), scanId);
    final ContainerNode<?> data = JsonUtils.parse(Report.getEntry(reportFile, Report.DATA_JSON_FILENAME).buf);
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

    fetchReport(work, application, scanId);
  }

  @Authorize(permission = Permission.READ)
  public Response printReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      String scanId) throws IOException
  {
    AuditData.get().setReportId(scanId);
    Application application = applicationDAO.getByPublicIdNotNull(appPublicId);
    String appId = application.getId();

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId);
    if (policyEvaluation == null) {
      throw new BadRequestException("Unable to locate scan " + scanId + " for application " + appId + ".");
    }

    File reportFile = getReport(work, appId, scanId);

    ContactDTO contact = applicationAdapter.getContact(application.getContactInternalName());
    String stageName = StageTypes.getById(policyEvaluation.getStageTypeId()).getName();
    File pdfFile = Report.printPdf(reportFile, application.getName(), stageName, contact);

    Date now = new Date();
    String filename =
        application.getName() + "-" + stageName + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(now) + ".pdf";

    ResponseBuilder responseBuilder = Response.ok();
    responseBuilder.lastModified(now).expires(now);
    responseBuilder.type("application/pdf");
    responseBuilder.header(HttpHeaders.CONTENT_LENGTH, pdfFile.length());
    responseBuilder.header("Content-Disposition", "attachment; filename=\"" + filename + '"');
    responseBuilder.entity(pdfFile);
    return responseBuilder.build();
  }
}
