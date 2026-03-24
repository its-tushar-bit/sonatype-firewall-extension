/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.KeyValue;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvaluationQueueProducerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(EvaluationQueueProducer.class);

  @Inject
  private EvaluationQueueProducer evaluationQueueProducer;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private ApiConfigurationService apiConfigurationService;

  @Mock
  private ApiConfigurationService mockApiConfigurationService;

  @Inject
  private EvaluationQueueDAO evaluationQueueDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  protected void configure(final Binder binder) {
    super.configure(binder);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bindInterceptor(Matchers.subclassesOf(ApiConfigurationService.class), Matchers.any(), invocation -> {
      if (invocation.getMethod().getModifiers() == Modifier.PUBLIC) {
        invocation.getMethod().invoke(mockApiConfigurationService, invocation.getArguments());
      }
      return invocation.proceed();
    });
  }

  @Test
  public void testRegister_withoutConfig() {
    evaluationQueueProducer.register();

    verify(mockTaskScheduler).unscheduleTask(evaluationQueueProducer);
  }

  @Test
  public void testRegister_withConfig() {
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .producerPeriod(EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD.plusMillis(1))
        .build();
    setEvaluationQueueConfig(evaluationQueueConfig);

    evaluationQueueProducer.register();

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer,
        Duration.ofMillis(EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD.toMillis() + 1));
  }

  @Test
  public void testExecute_noPolicyMonitoringLicense() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_MONITORING);

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Evaluation queue requires policy monitoring, skipping execution.");
  }

  @Test
  public void testExecute_disabled() throws Exception {
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(evaluationQueueConfig);

    evaluationQueueProducer.execute(null);

    assertThat(logOutput).atDebugLevel().contains("Evaluation queue is disabled, skipping execution.");
  }

  @Test
  public void testExecute_noAppsOrSboms() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");
  }

  @Test
  public void testExecute_noSboms() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    tempEntity.newApplicationWithParent();

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");
  }

  @Test
  public void testExecute_sbomAdded() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    ThirdPartySbomMetadata sbom = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");
    Date now = new Date();

    evaluationQueueProducer.execute(null);

    List<EvaluationQueue> stored = evaluationQueueDAO.getAll();
    assertThat(stored).hasSize(1);
    EvaluationQueue storedQueue = stored.get(0);
    assertThat(storedQueue.getPriority()).isZero();
    assertThat(storedQueue.getApplicationId()).isEqualTo(app.getId());
    assertThat(storedQueue.getStageTypeId()).isEqualTo(ComplianceStageType.ID);
    assertThat(storedQueue.getVersion()).isEqualTo(sbom.getSbomVersion());
    assertThat(storedQueue.getCreateTime()).isAfterOrEqualTo(now);
    assertThat(storedQueue.getUpdateTime()).isAfterOrEqualTo(now);
    assertThat(storedQueue.getWorkerId()).isNull();
  }

  @Test
  public void testExecute_resetCycle() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    evaluationQueueProducer.execute(null);

    assertThat(logOutput).atDebugLevel().contains("Processed 1 SBOM(s) up to latest offset 1 across 1 application(s).");
    assertThat(evaluationQueueDAO.getAll()).hasSize(1);

    // Pretend the SBOM got processed
    evaluationQueueDAO.getAll().forEach(evaluationQueueDAO::delete);

    // Executing again does nothing because the cycle already completed
    evaluationQueueProducer.execute(null);
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");

    // Resetting the cycle though allows us to continue
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put("resetCycle", "true");
    evaluationQueueProducer.execute(mockJobExecutionContext);
    assertThat(logOutput).atDebugLevel().contains("Processed 1 SBOM(s) up to latest offset 1 across 1 application(s).");
    assertThat(evaluationQueueDAO.getAll()).hasSize(1);
  }

  @Test
  public void testExecute_notMonitored() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getAll()).isEmpty();
  }

  @Test
  public void testExecute_maxRows() throws Exception {
    EvaluationQueueConfig evaluationQueueConfig =
        EvaluationQueueConfig.builder().enabled(true).producerMaxQueuedRows(1).build();
    setEvaluationQueueConfig(evaluationQueueConfig);
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(1));

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion).contains(sbom2.getSbomVersion());
  }

  @Test
  public void testExecute_maxVersions() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newVersionEvaluationWindow(app.getId(), ComplianceStageType.ID, 2, null);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata sbom3 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(2));

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());
  }

  @Test
  public void testExecute_maxAgeInDays() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newVersionEvaluationWindow(app.getId(), ComplianceStageType.ID, null, 1);
    long now = System.currentTimeMillis();
    tempEntity.newThirdPartySbomMetadata("id1", app.getId(), ACTIVE, new Date(now - Duration.ofDays(2).toMillis()));
    tempEntity.newThirdPartySbomMetadata("id2", app.getId(), ACTIVE, new Date(now - Duration.ofDays(1).toMillis()));
    ThirdPartySbomMetadata sbom3 = tempEntity.newThirdPartySbomMetadata("id3", app.getId(), ACTIVE,
        new Date(now - Duration.ofDays(1).minusMinutes(1).toMillis()));
    ThirdPartySbomMetadata sbom4 = tempEntity.newThirdPartySbomMetadata("id4", app.getId(), ACTIVE, new Date());

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom4.getSbomVersion(), sbom3.getSbomVersion());
  }

  @Test
  public void testExecute_alreadyEvaluated() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    ThirdPartySbomMetadata sbom1 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(1));
    ThirdPartyFile tf3 = tempEntity.newThirdPartyFile();
    ThirdPartyScan ts3 = tempEntity.newThirdPartyScan("scanRequestId", "scanId3", tf3);
    Date futureTime = new Date(System.currentTimeMillis() + Duration.ofDays(1).toMillis());
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, ts3.getScanId(), futureTime);
    tempEntity.newThirdPartySbomMetadata(tf3.getId(), app.getId(), ACTIVE, "fileName3");

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom2.getSbomVersion(), sbom1.getSbomVersion());
  }

  @Test
  public void testExecute_resumeAndReset() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .producerMaxQueuedRows(2)
        .cyclePeriod(Duration.ofSeconds(2))
        .build();
    setEvaluationQueueConfig(evaluationQueueConfig);
    ThirdPartySbomMetadata sbom1 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata sbom3 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(2));

    // Only 2 rows added due to the limit
    evaluationQueueProducer.execute(null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());

    // 0 rows added due to the limit
    evaluationQueueProducer.execute(null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());

    // Pretend sbom3 got processed
    evaluationQueueDAO.delete(
        evaluationQueueDAO.getAll()
            .stream()
            .filter(e -> e.getVersion().equals(sbom3.getSbomVersion()))
            .findFirst()
            .orElse(null));
    evaluationQueueProducer.execute(null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom2.getSbomVersion(), sbom1.getSbomVersion());

    // Pretend the rest got processed
    evaluationQueueDAO.getAll().forEach(evaluationQueueDAO::delete);
    evaluationQueueProducer.execute(null);
    assertThat(evaluationQueueDAO.getAll()).isEmpty();

    // Sleep for 2 seconds to ensure the cycle starts again
    Thread.sleep(2000);

    evaluationQueueProducer.execute(null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());
  }

  @Test
  public void testExecute_roundRobinOrdering() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    String orgId = tempEntity.newOrganization().getId();
    Application app1 = tempEntity.newApplicationWithSpecificId("app-aaa", "App A", "app-aaa-public", orgId);
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    Application app2 = tempEntity.newApplicationWithSpecificId("app-bbb", "App B", "app-bbb-public", orgId);
    tempEntity.newPolicyMonitoring(app2.getId(), ComplianceStageType.ID);
    Application app3 = tempEntity.newApplicationWithSpecificId("app-ccc", "App C", "app-ccc-public", orgId);
    tempEntity.newPolicyMonitoring(app3.getId(), ComplianceStageType.ID);

    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata app1Sbom3 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(2));

    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(3));
    ThirdPartySbomMetadata app2Sbom2 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(4));

    ThirdPartySbomMetadata app3Sbom1 = tempEntity.newThirdPartySbomMetadata(app3.getId(), ACTIVE, new Date(5));

    evaluationQueueProducer.execute(null);

    List<EvaluationQueue> queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(6);
    assertThat(queued).map(EvaluationQueue::getVersion)
        .containsExactly(
            app3Sbom1.getSbomVersion(),
            app2Sbom2.getSbomVersion(),
            app1Sbom3.getSbomVersion(),
            app2Sbom1.getSbomVersion(),
            app1Sbom2.getSbomVersion(),
            app1Sbom1.getSbomVersion());
  }

  @Test
  public void testExecute_resetCycleOnTimeout() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .producerMaxQueuedRows(2)
        .cyclePeriod(Duration.ofSeconds(1))
        .resetCycleOnTimeout(true)
        .build();
    setEvaluationQueueConfig(evaluationQueueConfig);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata sbom3 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(2));

    evaluationQueueProducer.execute(null);

    List<EvaluationQueue> queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(2);
    assertThat(queued).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());

    Thread.sleep(1000);

    evaluationQueueProducer.execute(null);
    queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(2);
    assertThat(queued).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());
    assertThat(logOutput).atWarnLevel().contains("Target cycle period exceeded without completion.");
  }

  @Test
  public void testConfigurationChanged_enabledChangedToTrue_shouldSchedule() {
    EvaluationQueueConfig disabledConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(disabledConfig);
    evaluationQueueProducer.register();
    verify(mockTaskScheduler).unscheduleTask(evaluationQueueProducer);
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig enabledConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(enabledConfig);

    evaluationQueueProducer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
  }

  @Test
  public void testConfigurationChanged_enabledChangedToFalse_shouldUnschedule() {
    EvaluationQueueConfig enabledConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(enabledConfig);
    evaluationQueueProducer.register();
    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig disabledConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(disabledConfig);

    evaluationQueueProducer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    verify(mockTaskScheduler).unscheduleTask(evaluationQueueProducer);
  }

  @Test
  public void testConfigurationChanged_periodChanged_shouldReschedule() {
    EvaluationQueueConfig initialConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(initialConfig);
    evaluationQueueProducer.register();
    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));

    EvaluationQueueConfig newConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(2)).build();
    setEvaluationQueueConfig(newConfig);

    evaluationQueueProducer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(2));
  }

  @Test
  public void testConfigurationChanged_periodUnchanged_shouldNotReschedule() {
    EvaluationQueueConfig initialConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(initialConfig);
    evaluationQueueProducer.register();
    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig newConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(newConfig);

    evaluationQueueProducer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    verify(mockTaskScheduler, never()).schedulePeriodicTask(eq(evaluationQueueProducer), any(Duration.class));
  }

  @Test
  public void testConfigurationChanged_differentProperty_shouldNotReschedule() {
    EvaluationQueueConfig initialConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(initialConfig);
    evaluationQueueProducer.register();
    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
    Mockito.reset(mockTaskScheduler);

    evaluationQueueProducer.configurationChanged(Set.of("SOME_OTHER_PROPERTY"));

    verify(mockTaskScheduler, never()).schedulePeriodicTask(eq(evaluationQueueProducer), any(Duration.class));
  }

  @Test
  public void testExecute_disallowsConcurrentExecution() {
    assertThat(JobBuilder.newJob(EvaluationQueueProducer.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute_triggerTaskNow_noCycleReset() throws Exception {
    when(mockTaskScheduler.isTaskScheduled(evaluationQueueProducer)).thenReturn(true);
    evaluationQueueProducer.execute(new HashMap<>(), mock(PrintWriter.class));

    Map<String, String> expectedJobDataMap = Map.of("resetCycle", "false");
    verify(mockTaskScheduler).triggerTaskNow(evaluationQueueProducer, expectedJobDataMap);
  }

  @Test
  public void testExecute_triggerTaskNow_resetCycle() throws Exception {
    when(mockTaskScheduler.isTaskScheduled(evaluationQueueProducer)).thenReturn(true);
    evaluationQueueProducer.execute(Map.of("resetCycle", List.of("true")), mock(PrintWriter.class));

    Map<String, String> expectedJobDataMap = Map.of("resetCycle", "true");
    verify(mockTaskScheduler).triggerTaskNow(evaluationQueueProducer, expectedJobDataMap);
  }

  @Test
  public void testExecute_doesNot_triggerTaskNow_ifNotScheduled() throws Exception {
    when(mockTaskScheduler.isTaskScheduled(evaluationQueueProducer)).thenReturn(false);
    evaluationQueueProducer.execute(Map.of("resetCycle", List.of("true")), mock(PrintWriter.class));

    Map<String, String> expectedJobDataMap = Map.of("resetCycle", "true");
    verify(mockTaskScheduler, never()).triggerTaskNow(evaluationQueueProducer, expectedJobDataMap);
  }

  @Test
  public void testExecute_unparseableCheckpoint() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    // Add unparseable content for the EVALUATION_QUEUE_PRODUCER_CHECKPOINT key
    tempEntity.newKeyValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT, "{");

    evaluationQueueProducer.execute(null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");
    assertThat(logOutput).atErrorLevel()
        .contains("Failed to parse the 'EVALUATION_QUEUE_PRODUCER_CHECKPOINT' value '{'.");
    assertThat(logOutput).atErrorLevel()
        .contains("This may be due to the" +
            " com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducerCheckpoint class changing.");
    assertThat(logOutput).atDebugLevel().contains("Cleaning up unparsable evaluation queue producer checkpoint.");
  }

  @Test
  public void testExecute_ignoresDuplicateInsert() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .cyclePeriod(Duration.ofSeconds(1))
        .build();
    setEvaluationQueueConfig(evaluationQueueConfig);
    ThirdPartySbomMetadata sbom1 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));

    // First SBOM to process initially which gets added to the queue
    evaluationQueueProducer.execute(null);
    List<EvaluationQueue> queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(1);
    assertThat(queued).map(EvaluationQueue::getVersion).containsExactly(sbom1.getSbomVersion());

    // Second SBOM gets added and the queue gets reset
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    Thread.sleep(1000);
    evaluationQueueProducer.execute(null);
    // Even though the second SBOM is more recent, since the queue isn't reset it gets added after the first SBOM
    queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(2);
    assertThat(queued)
        .map(evaluationQueue -> Pair.of(evaluationQueue.getPriority(), evaluationQueue.getVersion()))
        .containsExactlyInAnyOrder(Pair.of(0, sbom1.getSbomVersion()), Pair.of(1, sbom2.getSbomVersion()));
  }

  private void setEvaluationQueueConfig(final EvaluationQueueConfig evaluationQueueConfig) {
    apiConfigurationService.setConfiguration(Map.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG,
        JsonUtils.convertValue(evaluationQueueConfig, Map.class)));
  }
}
