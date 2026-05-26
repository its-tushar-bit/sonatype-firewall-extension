/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.exception.DataAccessException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

public abstract class AbstractReportPurgerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(ReportPurger.class);

  @Inject
  ReportPurger reportPurger;

  @Inject
  DataRetentionPolicyDAO dataRetentionPolicyDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  Configuration configuration;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Inject
  ApplicationReportPersistenceService applicationReportPersistenceService;

  Organization org;

  Application app;

  abstract void mockReport(PolicyEvaluation evaluation) throws Exception;

  Date daysAgo(int days) {
    return Date.from(ZonedDateTime.now().minusDays(days).toInstant());
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testPurgeReports_MaxCount() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 3, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(600)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(500)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-2", daysAgo(400)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-3", daysAgo(300)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-4", daysAgo(200)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-5", daysAgo(100)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-6", daysAgo(0)));

    reportPurger.purgeReports();

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "Using data retention policy for build reports for owner " + org.getId() +
                " with maxCount 3 and maxAgeInDays null.");
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-1")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-2")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-3")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-4")).isTrue();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-5")).isTrue();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-6")).isTrue();
  }

  @Test
  public void testPurgeReports_MaxAge() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 2));
    PolicyEvaluation oldest = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(6));
    mockReport(oldest);
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(5)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-2", daysAgo(4)));
    PolicyEvaluation toEvaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-3", daysAgo(3));
    mockReport(toEvaluation);
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-4", daysAgo(2)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-5", daysAgo(1)));
    PolicyEvaluation newest = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-6", daysAgo(0));
    mockReport(newest);

    reportPurger.purgeReports();

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "Using data retention policy for build reports for owner " + org.getId() +
                " with maxCount null and maxAgeInDays 2.");
    logOutput.assertThat().atDebugLevel().contains("Found 7 primary non-monitoring reports.");
    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "Oldest report report-0 with time " + oldest.getTime() + ". Newest report report-6 with time " +
                newest.getTime() + ".");
    logOutput.assertThat().atDebugLevel().contains("Determined cutoff date to be");
    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "Purging 4 reports from report report-0 with time " + oldest.getTime() + " to report report-3 with time " +
                toEvaluation.getTime() + ".");
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-1")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-2")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-3")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-4")).isTrue();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-5")).isTrue();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-6")).isTrue();
  }

  @Test
  public void testPurgeReports_MaxAge_LatestReportKeptRegardless() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 1));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(9)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(8)));

    reportPurger.purgeReports();

    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-1")).isTrue();
  }

  @Test
  public void testPurgeReports_ReportAlreadyDeleted() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, 1, null));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(3));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(2)));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-2", daysAgo(1));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-3", daysAgo(0)));

    reportPurger.purgeReports();

    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-1")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-2")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-3")).isTrue();
  }

  @Test
  public void testPurgeReports_ForRegularStages() throws Exception {
    String[] appEvalStageIds =
        {Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE};
    for (String stageId : appEvalStageIds) {
      dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), stageId, true, 1, null));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-obsolete", daysAgo(1)));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-monitored", false, true, daysAgo(1)));
      mockReport(tempEntity.newPolicyEvaluation(app.getId(), stageId, stageId + "-latest"));
    }

    reportPurger.purgeReports();

    for (String stageId : appEvalStageIds) {
      logOutput.assertThat()
          .atDebugLevel()
          .contains(
              "Using data retention policy for " + stageId + " reports for owner " + org.getId() +
                  " with maxCount 1 and maxAgeInDays null.");
    }

    for (String stageId : appEvalStageIds) {
      assertThat(applicationReportPersistenceService.reportExists(app.getId(), stageId + "-obsolete")).isFalse();
      assertThat(applicationReportPersistenceService.reportExists(app.getId(), stageId + "-monitored")).isTrue();
      assertThat(applicationReportPersistenceService.reportExists(app.getId(), stageId + "-latest")).isTrue();
    }
  }

  @Test
  public void testPurgeReports_ForContinuousMonitoring() throws Exception {
    dataRetentionPolicyDAO.insert(
        new DataRetentionPolicy(org.getId(), DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING, true, 1, null));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, Stage.ID_RELEASE + "-latest", false, true,
        daysAgo(9)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_OPERATE, Stage.ID_OPERATE + "-obsolete", false,
        true, daysAgo(3)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_OPERATE, Stage.ID_OPERATE + "-latest", false, true,
        daysAgo(2)));

    reportPurger.purgeReports();

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "Using data retention policy for continuous-monitoring reports for owner " + org.getId() +
                " with maxCount 1 and maxAgeInDays null.");
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), Stage.ID_RELEASE + "-latest")).isTrue();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), Stage.ID_OPERATE + "-obsolete")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), Stage.ID_OPERATE + "-latest")).isTrue();
  }

  @Test
  public void testPurgeReports_RetryAfterLockTimeout() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 2));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-0", daysAgo(6)));
    mockReport(tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "report-1", daysAgo(5)));

    PolicyEvaluationDAO spyPolicyEvaluationDAO = spy(policyEvaluationDAO);
    DataAccessException lockTimeout = new DataAccessException("lock timeout");
    AtomicInteger buildCalls = new AtomicInteger();
    doAnswer(invocation -> {
      String stageId = invocation.getArgument(1);
      if (Stage.ID_BUILD.equals(stageId) && buildCalls.getAndIncrement() < 2) {
        throw lockTimeout;
      }
      return invocation.callRealMethod();
    }).when(spyPolicyEvaluationDAO).getPrimaryNonMonitoringByApplicationIdAndStageId(anyString(), anyString());
    applyBeanFieldOverride(ReportPurger.class, "policyEvaluationDAO", spyPolicyEvaluationDAO);

    reportPurger = spy(reportPurger);
    long start = System.currentTimeMillis();
    reportPurger.purgeReports();
    long stop = System.currentTimeMillis();

    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-0")).isFalse();
    assertThat(applicationReportPersistenceService.reportExists(app.getId(), "report-1")).isTrue();
    verify(reportPurger).getDelayForRetry(0);
    verify(reportPurger).getDelayForRetry(1);
    verify(reportPurger, never()).getDelayForRetry(2);
    assertThat(stop - start).isLessThan(5 * 1000);
  }

  @Test
  public void testPurgeReports_RetryAfterLockTimeout_LimitedRetry() throws Exception {
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(org.getId(), Stage.ID_BUILD, true, null, 2));

    PolicyEvaluationDAO spyPolicyEvaluationDAO = spy(policyEvaluationDAO);
    DataAccessException lockTimeout = new DataAccessException("lock timeout");
    doAnswer(invocation -> {
      String stageId = invocation.getArgument(1);
      if (Stage.ID_BUILD.equals(stageId)) {
        throw lockTimeout;
      }
      return invocation.callRealMethod();
    }).when(spyPolicyEvaluationDAO).getPrimaryNonMonitoringByApplicationIdAndStageId(anyString(), anyString());
    applyBeanFieldOverride(ReportPurger.class, "policyEvaluationDAO", spyPolicyEvaluationDAO);

    reportPurger = spy(reportPurger);
    when(reportPurger.getDelayForRetry(anyInt())).thenReturn(Duration.ZERO);
    assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> reportPurger.purgeReports());
    verify(reportPurger).getDelayForRetry(9);
    verify(reportPurger, never()).getDelayForRetry(10);
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

  @SuppressWarnings("deprecation")
  @Test
  public void testExecute_AdminTask() throws Exception {
    ReportPurger reportPurgerSpy = spy(reportPurger);
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    reportPurgerSpy.executeForTest(mockContext, null);
    verify(reportPurgerSpy).purgeReports();
    verifyNoInteractions(taskSchedulerMock);
  }
}
