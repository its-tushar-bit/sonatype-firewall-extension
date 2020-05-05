/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.cache.CacheBuilder;
import org.codehaus.plexus.util.FileUtils;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

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

  private final ThirdPartyDataService thirdPartyDataService;

  @Inject
  public ReportService(
      InsightWork work,
      ReportDownloader reportDownloader,
      PolicyEvaluationDAO policyEvaluationDAO,
      InsightConfig insightConfig,
      ApplicationDAO applicationDAO,
      ThirdPartyDataService thirdPartyDataService)
  {
    this.work = work;
    this.reportDownloader = reportDownloader;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.insightConfig = insightConfig;
    this.applicationDAO = applicationDAO;
    this.thirdPartyDataService = thirdPartyDataService;
  }

  public File fetchReport(final Application app, final String scanId)
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
        processThirdPartyData(scanId, tempFile);
        FileUtils.rename(tempFile, reportFile);
      }

      Report.applyChanges(app, reportFile);

      return reportFile;
    }
    finally {
      lock.unlock();
    }
  }

  //visible for testing
  void includeThirdPartyData(final File reportFile, final ThirdPartyApplicationReportDTO dto)
      throws IOException
  {
    if (dto != null) {
      Map<String, Object> env = new HashMap<>();
      env.put("create", "false");
      env.put("useTempFile", Boolean.TRUE); //to avoid large byte streams created in memory
      Path archivePath = reportFile.toPath();
      URI archiveUri = URI.create("jar:" + archivePath.toUri());
      try (FileSystem fs = FileSystems.newFileSystem(archiveUri, env)) {
        appendFileToReportZip(fs, THIRD_PARTY_BOM_JSON_FILENAME, dto.billOfMaterials);
        appendFileToReportZip(fs, THIRD_PARTY_SECURITY_JSON_FILENAME, dto.securityRows);
        appendFileToReportZip(fs, THIRD_PARTY_LICENSE_JSON_FILENAME, dto.licenseRows);
      }
    }
  }

  private void appendFileToReportZip(final FileSystem fs, final String filename, final List<?> data)
      throws IOException
  {
    Path newFile = fs.getPath(filename);
    try (Writer writer = Files.newBufferedWriter(newFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      writer.write(new String(JsonUtils.generate(JsonUtils.aaData(data)), StandardCharsets.UTF_8));
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

  private void processThirdPartyData(final String scanId, final File tempFile) throws IOException {
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = thirdPartyDataService.getScanData(scanId);
    if (thirdPartyApplicationReportDTO != null) {
      includeThirdPartyData(tempFile, thirdPartyApplicationReportDTO);
      thirdPartyDataService.indexVulnerabilities(scanId);
      thirdPartyDataService.deleteByScanId(scanId);
    }
  }

  public File getReport(final String appId, final String scanId) {
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
    return getReportMetadataNoAuth(applicationPublicId, scanId);
  }

  public ReportMetadataDTO getReportMetadataNoAuth(
      final String applicationPublicId,
      final String scanId) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    ReportMetadataDTO metadata = new ReportMetadataDTO();
    metadata.setApplication(application);

    File reportFile = getReport(application.getId(), scanId);
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
      metadata.setStageId(evaluation.getStageTypeId());
    }

    // For NVS where a scanLabel is set for the application name and the stage name doesn't matter
    if (Report.getEntry(reportFile, "template.properties") != null) {
      JsonNode scanLabelNode = data.path("scanLabel");
      if (scanLabelNode.isTextual()) {
        metadata.getApplication().setName(scanLabelNode.asText());
        metadata.setReportTitle("Report");
      }
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

    fetchReport(application, scanId);
  }

  public ReportEntry getBomForPolicyEvaluation(PolicyEvaluation policyEvaluation) throws IOException {
    if (policyEvaluation == null) {
      return null;
    }
    File reportFile = getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());

    return Report.getEntry(reportFile, "bom.json");
  }
}
