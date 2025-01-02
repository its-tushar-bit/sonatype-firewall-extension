/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.WITH_REPORTS;
import static com.sonatype.insight.brain.report.ApplicationReport.DATA_JSON_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportPurgerTest
    extends AbstractComponentTest
{
  @Inject
  private ReportPurger reportPurger;

  @Inject
  private InsightWork work;

  @Inject
  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private Configuration configuration;

  @Mock
  private TaskScheduler taskSchedulerMock;

  private Organization org;

  private Application app;

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = work.getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      Files.write(reportDir.resolve("report.pdf"), Collections.singletonList("report.pdf"));
      reportDir = reportDir.resolve(FileApplicationReport.CACHE_DIRECTORY_NAME);
      Files.createDirectories(reportDir);
      for (String filename : new String[]{
          "index.html", "bom.json", DATA_JSON_FILENAME, "licenses.json",
          "licensethreats.json", "partialmatched.json", "policyalerts.json", "policythreats.json", "security.json",
          "summary.json"
      }) {
        Files.write(reportDir.resolve(filename),
            Collections.singletonList(FileApplicationReport.CACHE_DIRECTORY_NAME + "/" + filename));
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void mockScanFile(String report) throws IOException {
    Path scanDir = work.getScanDir(app.getId()).toPath();
    if (!Files.exists(scanDir)) {
      scanDir = Files.createDirectories(scanDir);
    }

    Path scanFile = scanDir.resolve("scan-" + report + ".xml.gz");
    Files.createFile(scanFile);
  }

  private Date daysAgo(int days) {
    return Date.from(ZonedDateTime.now().minusDays(days).toInstant());
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testPurgeReports_ScanFilesPurged() throws IOException {
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
    assertThat(work.getReportDir(app.getId()).list()).containsExactly("report-1");
    assertThat(work.getScanDir(app.getId()).list()).containsExactly("scan-report-1.xml.gz");
  }

  @Test
  public void testPurgeReports_ScanFilesNotPurged() throws IOException {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 1, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(600)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(500)));
    mockScanFile("report-0");
    mockScanFile("report-1");

    reportPurger.purgeReports();

    // Latest report and scan file remains
    // Previous report and scan file was deleted
    assertThat(work.getReportDir(app.getId()).list()).containsExactly("report-1");
    assertThat(work.getScanDir(app.getId()).list()).containsExactlyInAnyOrder("scan-report-0.xml.gz",
        "scan-report-1.xml.gz");
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
        {Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE};
    for (String stageId : appEvalStageIds) {
      dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), stageId, true, 1, null));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-obsolete", daysAgo(1)));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-monitored", false, true, daysAgo(1)));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-latest"));
    }

    reportPurger.purgeReports();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder( //
        Stage.ID_DEVELOP + "-latest", Stage.ID_DEVELOP + "-monitored", //
        Stage.ID_SOURCE + "-latest", Stage.ID_SOURCE + "-monitored", //
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
    try (FileSystem zipFileSystem = FileSystems.newFileSystem(trashFile.toPath(), (ClassLoader) null)) {
      String[] expectedZipEntries = {
          "report.zip", FileApplicationReport.CACHE_DIRECTORY_NAME + "/index.html",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/bom.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/" + DATA_JSON_FILENAME,
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/licenses.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/licensethreats.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/partialmatched.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/policyalerts.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/policythreats.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/security.json",
          FileApplicationReport.CACHE_DIRECTORY_NAME + "/summary.json"
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
  @H2DiskTest(customSettings = "DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=50;MV_STORE=FALSE")
  public void testPurgeReports_RetryAfterLockTimeout() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 2));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(6)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(5)));

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Exception> error = new AtomicReference<>();
    Thread thread = new Thread(() -> {
      try (TransactionContext tx = dataRetentionPolicyDAO.createTransactionContext()) {
        tx.begin();
        policyEvaluationDAO //
            .createQuery("SELECT entity FROM PolicyEvaluation entity") //
            .setLockModeType(LockModeType.PESSIMISTIC_WRITE) //
            .getList(tx);
        latch.countDown();
        Thread.sleep(2 * 1000);
        tx.commit();
      }
      catch (Exception e) {
        error.set(e);
      }
    });
    thread.start();

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    reportPurger = spy(reportPurger);
    long start = System.currentTimeMillis();
    reportPurger.purgeReports();
    long stop = System.currentTimeMillis();

    assertThat(work.getReportDir(app.getId()).list()).containsExactlyInAnyOrder("report-1");
    verify(reportPurger).getDelayForRetry(0);
    verify(reportPurger).getDelayForRetry(1);
    verify(reportPurger, never()).getDelayForRetry(2);
    assertThat(stop - start).isLessThan(5 * 1000);
    assertThat(error).hasValue(null);
  }

  @Test
  @H2DiskTest(customSettings = "DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=50;MV_STORE=FALSE")
  public void testPurgeReports_RetryAfterLockTimeout_LimitedRetry() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 2));

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Exception> error = new AtomicReference<>();
    Thread thread = new Thread(() -> {
      try (TransactionContext tx = dataRetentionPolicyDAO.createTransactionContext()) {
        tx.begin();
        policyEvaluationDAO //
            .createQuery("SELECT entity FROM PolicyEvaluation entity") //
            .setLockModeType(LockModeType.PESSIMISTIC_WRITE) //
            .getList(tx);
        latch.countDown();
        Thread.sleep(2 * 1000);
        tx.commit();
      }
      catch (Exception e) {
        error.set(e);
      }
    });
    thread.start();

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    reportPurger = spy(reportPurger);
    when(reportPurger.getDelayForRetry(anyInt())).thenReturn(Duration.ZERO);
    assertThatExceptionOfType(OptimisticLockException.class).isThrownBy(() -> reportPurger.purgeReports());
    verify(reportPurger).getDelayForRetry(9);
    verify(reportPurger, never()).getDelayForRetry(10);

    assertThat(error).hasValue(null);

    thread.join(); // wait for `thread` to complete so that cleanup (e.g. TemporaryEntity) works correctly
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ReportPurger.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute_QuartzJob() {
    ReportPurger reportPurgerSpy = spy(reportPurger);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    reportPurgerSpy.execute(mockJobExecutionContext);
    verify(reportPurgerSpy).purgeReports();
  }

  @Test
  public void testExecute_AdminTask() {
    reportPurger.execute(null, new PrintWriter(new StringWriter()));
    verify(taskSchedulerMock).triggerTaskNow(reportPurger, null);
  }
}
