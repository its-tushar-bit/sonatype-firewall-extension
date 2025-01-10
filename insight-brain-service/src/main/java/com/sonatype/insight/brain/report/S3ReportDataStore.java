/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.S3DataStoreConfig;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.annotations.VisibleForTesting;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Named
public class S3ReportDataStore
    implements ReportDataStore
{
  private static final Logger log = LoggerFactory.getLogger(S3ReportDataStore.class);

  private static final String REPORT_ZIP = "report.zip";

  private final S3Client s3Client;

  private final ReportDownloader reportDownloader;

  private final Configuration configuration;

  private final S3DataStoreConfig s3Config;

  private final InsightWork insightWork;

  @Inject
  public S3ReportDataStore(
      final InsightConfig config,
      final ReportDownloader reportDownloader,
      final Configuration configuration,
      final S3Client s3Client,
      final InsightWork insightWork)
  {
    this.reportDownloader = reportDownloader;
    this.configuration = configuration;
    this.s3Client = s3Client;
    this.insightWork = insightWork;
    s3Config = config.getReportDataStoreConfig().getS3Config();
  }

  @Override
  public ApplicationReport downloadReport(
      final String applicationId,
      final String scanId,
      final DownloadReportPostAction action) throws IOException, NotFoundException
  {
    S3ApplicationReport applicationReport = getApplicationReport(applicationId, scanId);
    if (!applicationReport.exists()) {
      int reportTimeoutInSeconds = configuration.getReportTimeoutInSeconds();
      FileApplicationReport tempReport = tempReport(applicationId, scanId);
      if (!reportDownloader.downloadReport(scanId, tempReport, reportTimeoutInSeconds, 5)) {
        throw new NotFoundException("Could not download the report for scan ID " + scanId);
      }
      action.apply(scanId, tempReport, applicationId);
      putReportZip(tempReport, applicationId, scanId);
      putReportEntries(tempReport, applicationReport);
      if (!tempReport.getFile().delete()) {
        log.warn("Failed to delete temp file {}", tempReport.getFile());
      }
    }
    return applicationReport;
  }

  /**
   * Reads all files in the report zip and PUTs them as S3 objects
   *
   * @param tempReport
   * @param applicationReport
   * @throws IOException
   */
  private void putReportEntries(final FileApplicationReport tempReport, final S3ApplicationReport applicationReport)
      throws IOException
  {
    Map<String, Object> env = new HashMap<>();
    env.put("create", "false");
    env.put("useTempFile", Boolean.TRUE); //to avoid large byte streams created in memory
    Path archivePath = tempReport.getFile().toPath();
    URI archiveUri = URI.create("jar:" + archivePath.toUri());
    try (FileSystem fs = FileSystems.newFileSystem(archiveUri, env)) {
      try (Stream<Path> paths = Files.walk(fs.getPath("/"))) {
        paths.filter(Files::isRegularFile)
            .forEach(f -> {
              PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                  .bucket(s3Config.getBucketName())
                  .key(applicationReport.getKey(f.getFileName().toString()).toString())
                  .build();
              s3Client.putObject(putObjectRequest, RequestBody.fromFile(f));
            });
      }
    }
  }

  @VisibleForTesting
  public FileApplicationReport tempReport(final String appId, final String scanId) {
    final File tempFile = FileUtils.createTempFile("temp-", ".zip", insightWork.getReportDir(appId, scanId));
    return new FileApplicationReport(tempFile);
  }

  /**
   * Stores the report.zip in S3
   *
   * @param tempSource
   * @param applicationId
   * @param scanId
   */
  private void putReportZip(final FileApplicationReport tempSource, final String applicationId, final String scanId) {
    var applicationReport =
        createS3ObjectApplicationReportEntity(applicationId, scanId, S3ReportDataStore.REPORT_ZIP);
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(s3Config.getBucketName())
        .key(applicationReport.getKey().toString())
        .build();
    s3Client.putObject(putObjectRequest, tempSource.getFile().toPath());
  }

  @Override
  public S3ApplicationReport getApplicationReport(final String appId, final String scanId) {
    return createS3ObjectApplicationReportEntity(appId, scanId, REPORT_ZIP);
  }

  private S3ApplicationReport createS3ObjectApplicationReportEntity(
      final String appId,
      final String scanId,
      final String name)
  {
    return new S3ApplicationReport(s3Client, s3Config, appId, scanId, name);
  }

  @Override
  public ReportEntity getReportEntityByName(final String applicationId, final String scanId, final String name) {
    return createS3ObjectApplicationReportEntity(applicationId, scanId, name);
  }

  @Override
  public ReportPdf getReportPdf(final String appId, final String scanId) {
    return new S3ReportPdf(s3Client, s3Config, appId, scanId);
  }
}
