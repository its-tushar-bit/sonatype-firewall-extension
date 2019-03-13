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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportPurgerTest
    extends AbstractComponentTest
{
  @Inject
  private ReportPurger reportPurger;

  @Inject
  private InsightWork work;

  @Inject
  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private Organization org;

  private Application app;

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = work.getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Arrays.asList("report.zip"));
      Files.write(reportDir.resolve("report.pdf"), Arrays.asList("report.pdf"));
      reportDir = reportDir.resolve(Report.CACHE_DIRECTORY_NAME);
      Files.createDirectories(reportDir);
      for (String filename : new String[]{"index.html", "bom.json", "data.json", "licenses.json", "licensethreats.json",
          "partialmatched.json", "policyalerts.json", "policythreats.json", "security.json", "summary.json"}) {
        Files.write(reportDir.resolve(filename), Arrays.asList(Report.CACHE_DIRECTORY_NAME + "/" + filename));
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Date daysAgo(int days) {
    return Date.from(ZonedDateTime.now().minusDays(days).toInstant());
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testPurgeReports_MaxCount() {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 3, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(600)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(500)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-2", daysAgo(400)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-3", daysAgo(300)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-4", daysAgo(200)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-5", daysAgo(100)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-6", daysAgo(0)));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder("report-6", "report-5", "report-4");
  }

  @Test
  public void testPurgeReports_MaxAge() {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 2));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(6)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(5)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-2", daysAgo(4)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-3", daysAgo(3)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-4", daysAgo(2)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-5", daysAgo(1)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-6", daysAgo(0)));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder("report-6", "report-5", "report-4");
  }

  @Test
  public void testPurgeReports_MaxAge_LatestReportKeptRegardless() {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 1));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(9)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(8)));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder("report-1");
  }

  @Test
  public void testPurgeReports_NoEvaluations() {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 3, null));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId())).doesNotExist();
  }

  @Test
  public void testPurgeReports_ReportAlreadyDeleted() {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 1, null));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(3));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(2)));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-2", daysAgo(1));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-3", daysAgo(0)));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder("report-3");
  }

  @Test
  public void testPurgeReports_ForRegularStages() {
    String[] appEvalStageIds =
        {Stage.ID_DEVELOP, Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE};
    for (String stageId : appEvalStageIds) {
      dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), stageId, true, 1, null));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-obsolete", daysAgo(1)));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-monitored", false, true, daysAgo(1)));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-latest"));
    }

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder( //
        Stage.ID_DEVELOP + "-latest", Stage.ID_DEVELOP + "-monitored", //
        Stage.ID_BUILD + "-latest", Stage.ID_BUILD + "-monitored", //
        Stage.ID_STAGE_RELEASE + "-latest", Stage.ID_STAGE_RELEASE + "-monitored", //
        Stage.ID_RELEASE + "-latest", Stage.ID_RELEASE + "-monitored", //
        Stage.ID_OPERATE + "-latest", Stage.ID_OPERATE + "-monitored");
  }

  @Test
  public void testPurgeReports_ForContinuousMonitoring() {
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING, true, 1, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, Stage.ID_RELEASE + "-latest", false, true,
        daysAgo(9)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_OPERATE, Stage.ID_OPERATE + "-obsolete", false,
        true, daysAgo(3)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_OPERATE, Stage.ID_OPERATE + "-latest", false, true,
        daysAgo(2)));

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder(Stage.ID_RELEASE + "-latest",
        Stage.ID_OPERATE + "-latest");
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
    try (FileSystem zipFileSystem = FileSystems.newFileSystem(trashFile.toPath(), null)) {
      String[] expectedZipEntries = {"report.zip", Report.CACHE_DIRECTORY_NAME + "/index.html",
          Report.CACHE_DIRECTORY_NAME + "/bom.json", Report.CACHE_DIRECTORY_NAME + "/data.json",
          Report.CACHE_DIRECTORY_NAME + "/licenses.json", Report.CACHE_DIRECTORY_NAME + "/licensethreats.json",
          Report.CACHE_DIRECTORY_NAME + "/partialmatched.json", Report.CACHE_DIRECTORY_NAME + "/policyalerts.json",
          Report.CACHE_DIRECTORY_NAME + "/policythreats.json", Report.CACHE_DIRECTORY_NAME + "/security.json",
          Report.CACHE_DIRECTORY_NAME + "/summary.json"};
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
}
