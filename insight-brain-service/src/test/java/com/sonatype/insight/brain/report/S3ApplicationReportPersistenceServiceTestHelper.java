/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.sonatype.insight.brain.service.InsightConfig;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ApplicationReportPersistenceServiceTestHelper
    implements ApplicationReportPersistenceServiceTestHelper
{
  private final InsightConfig insightConfig;

  private final S3Client s3Client;

  private final Supplier<String> expectedEffectivePrefix;

  public S3ApplicationReportPersistenceServiceTestHelper(
      final InsightConfig insightConfig,
      final S3Client s3Client,
      final Supplier<String> expectedEffectivePrefix)
  {
    this.insightConfig = insightConfig;
    this.s3Client = s3Client;
    this.expectedEffectivePrefix = expectedEffectivePrefix;
  }

  @Override
  public void saveMockReport(String reportName) throws IOException {
    saveEmptyMockReport();

    Path reportDir;
    try {
      reportDir = Path.of(getClass().getResource(TEST_REPORT_CLASSPATH + reportName).toURI());
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    try (var fileWalk = Files.walk(reportDir)) {
      fileWalk.filter(Files::isRegularFile).forEach(file -> {
        String name = reportDir.relativize(file).toString();
        String key = "%sreport/%s/%s/report.files/%s".formatted(
            expectedEffectivePrefix.get(),
            APPLICATION_ID,
            SCAN_ID,
            name);

        s3Client.putObject(
            PutObjectRequest.builder().bucket(getBucketName()).key(key).build(),
            file);
      });
    }
  }

  @Override
  public void saveEmptyMockReport(String scanId) {
    String key = "%sreport/%s/%s/report.files/index.html".formatted(
        expectedEffectivePrefix.get(),
        APPLICATION_ID,
        scanId);

    s3Client.putObject(
        PutObjectRequest.builder().bucket(getBucketName()).key(key).build(),
        RequestBody.fromString("<html></html>"));
  }

  @Override
  public String readFromLocalFiles(String applicationId, String scanId, String name) {
    return readKey("report/%s/%s/report.cache/%s".formatted(applicationId, scanId, name));
  }

  @Override
  public String readFromOriginalFiles(String applicationId, String scanId, String name) {
    return readKey("report/%s/%s/report.files/%s".formatted(applicationId, scanId, name));
  }

  @Override
  public String readFromAdditionalFiles(String applicationId, String scanId, String name) {
    return readKey("report/%s/%s/additional.files/%s".formatted(applicationId, scanId, name));
  }

  @Override
  public String readPdf(String applicationId, String scanId) {
    return readKey("report/%s/%s/report.pdf".formatted(applicationId, scanId));
  }

  @Override
  public String readVulnerabilitySignatures(String applicationId, String scanId) {
    return readKey("report/%s/%s/vulnerability-signatures.json".formatted(applicationId, scanId));
  }

  @Override
  public void writeAdditionalFile(
      String applicationId,
      String scanId,
      String name,
      String content)
  {
    String key = "report/%s/%s/additional.files/%s".formatted(applicationId, scanId, name);
    writeKey(key, content);
  }

  @Override
  public void writeLocalFile(
      String applicationId,
      String scanId,
      String name,
      String content)
  {
    String key = "report/%s/%s/report.cache/%s".formatted(applicationId, scanId, name);
    writeKey(key, content);
  }

  @Override
  public void writePdf(String applicationId, String scanId, String content) {
    String key = "report/%s/%s/report.pdf".formatted(applicationId, scanId);
    writeKey(key, content);
  }

  @Override
  public void writeVulnerabilitySignatures(String applicationId, String scanId, String content) {
    String key = "report/%s/%s/vulnerability-signatures.json".formatted(applicationId, scanId);
    writeKey(key, content);
  }

  public void writeZipFile(String applicationId, String scanId, byte[] zipContent) {
    String key = "report/%s/%s/report.zip".formatted(applicationId, scanId);
    s3Client.putObject(
        PutObjectRequest.builder().bucket(getBucketName()).key(expectedEffectivePrefix.get() + key).build(),
        RequestBody.fromBytes(zipContent));
  }

  public boolean zipFileExists(String applicationId, String scanId) {
    String key = "report/%s/%s/report.zip".formatted(applicationId, scanId);
    return readKey(key) != null;
  }

  @Override
  public void waitForNewFileTime() throws InterruptedException {
    // S3 times come from the Last-Modified HTTP header with gives the appearance of 1-second resolution
    Thread.sleep(1000);
  }

  private String getBucketName() {
    return insightConfig.getStorage().getS3Config().getBucketName();
  }

  private String readKey(String key) {
    try {
      byte[] responseContents = s3Client.getObjectAsBytes(
          GetObjectRequest.builder()
              .bucket(getBucketName())
              .key(expectedEffectivePrefix.get() + key)
              .build())
          .asByteArray();

      return new String(responseContents, StandardCharsets.UTF_8);
    }
    catch (NoSuchKeyException e) {
      return null;
    }
  }

  private void writeKey(String key, String content) {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(getBucketName()).key(expectedEffectivePrefix.get() + key).build(),
        RequestBody.fromString(content));
  }
}
