/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Before;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.InsightWork;

import com.google.common.collect.Sets;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.WITH_REPORTS;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportPurgerTest
    extends AbstractReportPurgerTest
{
  @Inject
  private InsightWork work;

  @Before
  public void cleanTrashDir() throws IOException {
    File trashDir = work.getTrashDir();
    if (trashDir.exists()) {
      FileUtils.deleteDirectory(trashDir);
    }
  }

  @Override
  void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = work.getReportDir(evaluation.getOwnerId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      Files.write(reportDir.resolve("report.pdf"), Collections.singletonList("report.pdf"));
      reportDir = reportDir.resolve("report.cache");
      Files.createDirectories(reportDir);
      for (String filename : new String[]{
        "index.html", "bom.json", "data.json", "licenses.json",
        "licensethreats.json", "partialmatched.json", "policyalerts.json", "policythreats.json", "security.json",
        "summary.json"
      })
      {
        Files.write(reportDir.resolve(filename),
            Collections.singletonList("report.cache/" + filename));
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void mockScanFile(String report) throws Exception {
    Path scanDir = work.getScanDir(app.getId()).toPath();
    if (!Files.exists(scanDir)) {
      scanDir = Files.createDirectories(scanDir);
    }

    Path scanFile = scanDir.resolve("scan-" + report + ".xml.gz");
    Files.createFile(scanFile);
  }

  @Test
  public void testPurgeReports_Trash() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 1, null));
    String reportId = "to-be-trashed";
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, reportId, daysAgo(1)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "latest", daysAgo(0)));

    String dateBefore = LocalDate.now().toString();
    reportPurger.purgeReports();
    String dateAfter = LocalDate.now().toString();

    assertThat(work.getTrashDir().list()).hasSize(1).containsOnly(dateBefore, dateAfter);
    File trashDir = work.getTrashDir().listFiles()[0];
    assertThat(trashDir).isDirectory();
    assertThat(trashDir.list()).containsExactly(app.getId().substring(0, 2));
    trashDir = trashDir.listFiles()[0];
    assertThat(trashDir).isDirectory();
    File trashFile = new File(trashDir, "app-" + app.getId() + "-report-" + reportId + ".zip");
    assertThat(trashFile).isFile();
    assertThat(trashDir.list()).containsExactly(trashFile.getName());
    try (FileSystem zipFileSystem = FileSystems.newFileSystem(trashFile.toPath(), (ClassLoader) null)) {
      String[] expectedZipEntries = {
        "report.zip", "report.cache/index.html",
        "report.cache/bom.json",
        "report.cache/data.json",
        "report.cache/licenses.json",
        "report.cache/licensethreats.json",
        "report.cache/partialmatched.json",
        "report.cache/policyalerts.json",
        "report.cache/policythreats.json",
        "report.cache/security.json",
        "report.cache/summary.json"
      };
      for (String zipEntry : expectedZipEntries) {
        Path zipEntryPath = zipFileSystem.getPath(app.getId(), reportId, zipEntry);
        assertThat(zipEntryPath).isRegularFile();
        assertThat(Files.readAllLines(zipEntryPath)).containsExactly(zipEntry);
      }
      String[] unexpectedZipEntries = {"report.pdf"};
      for (String zipEntry : unexpectedZipEntries) {
        Path zipEntryPath = zipFileSystem.getPath(app.getId(), reportId, zipEntry);
        assertThat(zipEntryPath).doesNotExist();
      }
    }
  }

  @Test
  public void testPurgeReports_ScanFilesPurged() throws Exception {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.PURGE_SCAN_FILES, WITH_REPORTS);
    configuration.configurationChanged(Sets.newHashSet(SystemConfigurationProperty.PURGE_SCAN_FILES));

    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 1, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(600)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(500)));
    mockScanFile("report-0");
    mockScanFile("report-1");

    reportPurger.purgeReports();

    // Latest report and scan file remains
    // Previous report and scan file was deleted
    assertThat(lifecycleReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(lifecycleReportPersistenceService.reportExists(app.getId(), "report-1")).isTrue();
    assertThat(work.getScanDir(app.getId()).list()).containsExactly("scan-report-1.xml.gz");
  }

  @Test
  public void testPurgeReports_ScanFilesNotPurged() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 1, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(600)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(500)));
    mockScanFile("report-0");
    mockScanFile("report-1");

    reportPurger.purgeReports();

    // Latest report and scan file remains
    // Previous report and scan file was deleted
    assertThat(lifecycleReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(lifecycleReportPersistenceService.reportExists(app.getId(), "report-1")).isTrue();
    assertThat(work.getScanDir(app.getId()).list()).containsExactlyInAnyOrder("scan-report-0.xml.gz",
        "scan-report-1.xml.gz");
  }

  @Test
  public void testPurgeReports_NoEvaluations() {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 3, null));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId())).doesNotExist();
  }
}
