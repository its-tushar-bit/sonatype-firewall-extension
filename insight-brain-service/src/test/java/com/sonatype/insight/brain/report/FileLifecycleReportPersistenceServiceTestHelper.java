/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.testing.FileTimestampTestUtil;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.rules.TemporaryFolder;

public class FileLifecycleReportPersistenceServiceTestHelper
    implements LifecycleReportPersistenceServiceTestHelper
{
  private final TemporaryFolder tempDir;

  private final InsightConfig insightConfig;

  private final InsightWork insightWork;

  public FileLifecycleReportPersistenceServiceTestHelper(
      final TemporaryFolder tempDir,
      final InsightConfig insightConfig,
      final InsightWork insightWork)
  {
    this.tempDir = tempDir;
    this.insightConfig = insightConfig;
    this.insightWork = insightWork;
  }

  @Override
  public void saveMockReport(String reportName) throws IOException {
    ReportHelper.saveMockReport(
        insightWork,
        tempDir,
        TEST_REPORT_CLASSPATH + reportName,
        APPLICATION_ID,
        SCAN_ID);
  }

  @Override
  public void saveEmptyMockReport(String scanId) throws IOException {
    ReportHelper.saveMockReport(insightWork, APPLICATION_ID, scanId);
  }

  @Override
  public String readFromLocalFiles(String applicationId, String scanId, String name) throws IOException {
    Path entityDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/report.cache/" + name);

    if (Files.exists(entityDiskPath)) {
      return Files.readString(entityDiskPath, StandardCharsets.UTF_8);
    }
    else {
      return null;
    }
  }

  @Override
  public String readFromOriginalFiles(String applicationId, String scanId, String name) throws IOException {
    Path pathToZip = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" + scanId
        + "/report.zip");

    if (Files.exists(pathToZip)) {
      try (var zipFs = FileSystems.newFileSystem(URI.create("jar:" + pathToZip.toUri()), Map.of("create", "false"))) {
        Path pathInZip = zipFs.getPath(name);

        if (Files.exists(pathInZip)) {
          return Files.readString(pathInZip, StandardCharsets.UTF_8);
        }
        else {
          return null;
        }
      }
    }
    else {
      return null;
    }
  }

  @Override
  public String readFromAdditionalFiles(String applicationId, String scanId, String name) throws IOException {
    Path entityDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/additional.files/" + name);

    if (Files.exists(entityDiskPath)) {
      return Files.readString(entityDiskPath, StandardCharsets.UTF_8);
    }
    else {
      return null;
    }
  }

  @Override
  public String readPdf(String applicationId, String scanId) throws IOException {
    Path entityDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/report.pdf");

    if (Files.exists(entityDiskPath)) {
      return Files.readString(entityDiskPath, StandardCharsets.UTF_8);
    }
    else {
      return null;
    }
  }

  @Override
  public String readVulnerabilitySignatures(String applicationId, String scanId) throws IOException {
    Path entityDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/vulnerability-signatures.json");

    if (Files.exists(entityDiskPath)) {
      return Files.readString(entityDiskPath, StandardCharsets.UTF_8);
    }
    else {
      return null;
    }
  }

  @Override
  public void writeAdditionalFile(
      String applicationId,
      String scanId,
      String name,
      String content) throws IOException
  {
    Path entityDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/additional.files/" + name);

    Files.createDirectories(entityDiskPath.getParent());
    Files.writeString(entityDiskPath, content, StandardCharsets.UTF_8);
  }

  @Override
  public void writeLocalFile(
      String applicationId,
      String scanId,
      String name,
      String content) throws IOException
  {
    Path entityDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/report.cache/" + name);

    Files.createDirectories(entityDiskPath.getParent());
    Files.writeString(entityDiskPath, content, StandardCharsets.UTF_8);
  }

  @Override
  public void writePdf(String applicationId, String scanId, String content) throws IOException {
    Path pdfDiskPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/report.pdf");

    Files.createDirectories(pdfDiskPath.getParent());
    Files.writeString(pdfDiskPath, content, StandardCharsets.UTF_8);
  }

  @Override
  public void writeVulnerabilitySignatures(String applicationId, String scanId, String content) throws IOException {
    Path signaturesPath = Path.of(insightConfig.getClusterDirectory().toString() + "/report/" + applicationId + "/" +
        scanId + "/vulnerability-signatures.json");

    Files.createDirectories(signaturesPath.getParent());
    Files.writeString(signaturesPath, content, StandardCharsets.UTF_8);
  }

  @Override
  public void waitForNewFileTime() throws InterruptedException {
    FileTimestampTestUtil.waitForNewFileTime(insightWork.getWorkDir().toPath());
  }
}
