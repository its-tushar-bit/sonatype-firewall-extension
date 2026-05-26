/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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

  @Inject
  private EvaluationQueueDAO evaluationQueueDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testRegister_withoutConfig() {
    evaluationQueueProducer.register();

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer,
        Duration.ofMillis(EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD.toMillis()));
  }

  @Test
  public void testRegister_withConfig() {
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .producerPeriod(EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD.plusMillis(1))
        .build();
    setEvaluationQueueConfig(evaluationQueueConfig);
    Mockito.reset(mockTaskScheduler);

    evaluationQueueProducer.register();

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer,
        Duration.ofMillis(EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD.toMillis() + 1));
  }

  @Test
  public void testExecute_noPolicyMonitoringLicense() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_MONITORING);

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Evaluation queue requires policy monitoring, skipping execution.");
  }

  @Test
  public void testExecute_disabled() throws Exception {
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(evaluationQueueConfig);

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(logOutput).atDebugLevel().contains("Evaluation queue is disabled, skipping execution.");
  }

  @Test
  public void testExecute_noAppsOrSboms() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");
  }

  @Test
  public void testExecute_noSboms() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    tempEntity.newApplicationWithParent();

    evaluationQueueProducer.execute((JobExecutionContext) null);

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

    evaluationQueueProducer.execute((JobExecutionContext) null);

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

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(logOutput).atDebugLevel().contains("Processed 1 SBOM(s) up to latest offset 1.");
    assertThat(evaluationQueueDAO.getAll()).hasSize(1);

    // Pretend the SBOM got processed
    evaluationQueueDAO.getAll().forEach(evaluationQueueDAO::delete);

    // Executing again does nothing because the cycle already completed
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");

    // Resetting the cycle though allows us to continue
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put("resetCycle", "true");
    evaluationQueueProducer.execute(mockJobExecutionContext);
    assertThat(logOutput).atDebugLevel().contains("Processed 1 SBOM(s) up to latest offset 1.");
    assertThat(evaluationQueueDAO.getAll()).hasSize(1);
  }

  @Test
  public void testExecute_notMonitored() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    evaluationQueueProducer.execute((JobExecutionContext) null);

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

    evaluationQueueProducer.execute((JobExecutionContext) null);

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

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());
  }

  @Test
  public void testExecute_maxVersions_earlyBreakWhenAllAppsBounded() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    String orgId = tempEntity.newOrganization().getId();
    Application app1 = tempEntity.newApplicationWithSpecificId("app-aaa", "App A", "app-aaa-public", orgId);
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    Application app2 = tempEntity.newApplicationWithSpecificId("app-bbb", "App B", "app-bbb-public", orgId);
    tempEntity.newPolicyMonitoring(app2.getId(), ComplianceStageType.ID);

    tempEntity.newVersionEvaluationWindow(app1.getId(), ComplianceStageType.ID, 1, null);
    tempEntity.newVersionEvaluationWindow(app2.getId(), ComplianceStageType.ID, 2, null);

    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(1));
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(2));
    ThirdPartySbomMetadata app1Sbom4 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(3));

    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(4));
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(5));
    ThirdPartySbomMetadata app2Sbom3 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(6));
    ThirdPartySbomMetadata app2Sbom4 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(7));

    evaluationQueueProducer.execute((JobExecutionContext) null);

    // app2 maxVersions=2: rank 0 (app2Sbom4) + rank 1 (app2Sbom3)
    // app1 maxVersions=1: only rank 0 (app1Sbom4)
    // rank 2: both filtered by maxVersions, nothing passes → early break
    // (maxAppActiveSboms=4 but stopped at offset 3)
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(
            app2Sbom4.getSbomVersion(),
            app1Sbom4.getSbomVersion(),
            app2Sbom3.getSbomVersion());
    assertThat(logOutput).atDebugLevel()
        .contains("Processed 6 SBOM(s) up to latest offset 3.");
    assertThat(logOutput).atInfoLevel().contains("Completed cycle.");

    evaluationQueueDAO.getAll().forEach(evaluationQueueDAO::delete);
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(logOutput).atDebugLevel().contains("Cycle already completed, skipping execution.");
    assertThat(evaluationQueueDAO.getAll()).isEmpty();
  }

  @Test
  public void testExecute_maxVersions_noEarlyBreakWhenAppUnbounded() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    String orgId = tempEntity.newOrganization().getId();
    Application app1 = tempEntity.newApplicationWithSpecificId("app-aaa", "App A", "app-aaa-public", orgId);
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    Application app2 = tempEntity.newApplicationWithSpecificId("app-bbb", "App B", "app-bbb-public", orgId);
    tempEntity.newPolicyMonitoring(app2.getId(), ComplianceStageType.ID);

    tempEntity.newVersionEvaluationWindow(app1.getId(), ComplianceStageType.ID, 1, null);
    // app2 has no window — unbounded

    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(1));

    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(2));
    ThirdPartySbomMetadata app2Sbom2 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(3));
    ThirdPartySbomMetadata app2Sbom3 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(4));

    evaluationQueueProducer.execute((JobExecutionContext) null);

    // app2 no window: all ranks — sbom3 (rank 0), sbom2 (rank 1), sbom1 (rank 2)
    // app1 maxVersions=1: only rank 0 (sbom2)
    // no early break because app2 is unbounded — iterates all 3 ranks (maxAppActiveSboms=3)
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(
            app2Sbom3.getSbomVersion(),
            app1Sbom2.getSbomVersion(),
            app2Sbom2.getSbomVersion(),
            app2Sbom1.getSbomVersion());
    assertThat(logOutput).atDebugLevel()
        .contains("Processed 5 SBOM(s) up to latest offset 3.");
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

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom4.getSbomVersion(), sbom3.getSbomVersion());
  }

  @Test
  public void testExecute_maxAgeInDays_earlyBreak() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    String orgId = tempEntity.newOrganization().getId();
    Application app1 = tempEntity.newApplicationWithSpecificId("app-aaa", "App A", "app-aaa-public", orgId);
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    Application app2 = tempEntity.newApplicationWithSpecificId("app-bbb", "App B", "app-bbb-public", orgId);
    tempEntity.newPolicyMonitoring(app2.getId(), ComplianceStageType.ID);

    long now = System.currentTimeMillis();
    // maxAgeInDays=1 for both, no maxVersions
    tempEntity.newVersionEvaluationWindow(app1.getId(), ComplianceStageType.ID, null, 1);
    tempEntity.newVersionEvaluationWindow(app2.getId(), ComplianceStageType.ID, null, 1);

    // app1: 3 SBOMs, newest is recent, older two are > 1 day old
    tempEntity.newThirdPartySbomMetadata("a1s1", app1.getId(), ACTIVE,
        new Date(now - Duration.ofDays(3).toMillis()));
    tempEntity.newThirdPartySbomMetadata("a1s2", app1.getId(), ACTIVE,
        new Date(now - Duration.ofDays(2).toMillis()));
    ThirdPartySbomMetadata app1Recent = tempEntity.newThirdPartySbomMetadata("a1s3", app1.getId(), ACTIVE,
        new Date(now));

    // app2: 3 SBOMs, newest is recent, older two are > 1 day old
    tempEntity.newThirdPartySbomMetadata("a2s1", app2.getId(), ACTIVE,
        new Date(now - Duration.ofDays(3).toMillis()));
    tempEntity.newThirdPartySbomMetadata("a2s2", app2.getId(), ACTIVE,
        new Date(now - Duration.ofDays(2).toMillis()));
    ThirdPartySbomMetadata app2Recent = tempEntity.newThirdPartySbomMetadata("a2s3", app2.getId(), ACTIVE,
        new Date(now));

    evaluationQueueProducer.execute((JobExecutionContext) null);

    // rank 0: both newest SBOMs pass age filter → queued
    // rank 1: both 2nd SBOMs are > 1 day old → filtered by age, nothing passes → early break
    // (maxAppActiveSboms=3 but stopped at offset 2)
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(app2Recent.getSbomVersion(), app1Recent.getSbomVersion());
    assertThat(logOutput).atDebugLevel()
        .contains("Processed 4 SBOM(s) up to latest offset 2.");
    assertThat(logOutput).atInfoLevel().contains("Completed cycle.");
  }

  @Test
  public void testExecute_maxAgeInDays_noEarlyBreakWhenAllRecent() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    String orgId = tempEntity.newOrganization().getId();
    Application app1 = tempEntity.newApplicationWithSpecificId("app-aaa", "App A", "app-aaa-public", orgId);
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    Application app2 = tempEntity.newApplicationWithSpecificId("app-bbb", "App B", "app-bbb-public", orgId);
    tempEntity.newPolicyMonitoring(app2.getId(), ComplianceStageType.ID);

    long now = System.currentTimeMillis();
    tempEntity.newVersionEvaluationWindow(app1.getId(), ComplianceStageType.ID, null, 1);
    tempEntity.newVersionEvaluationWindow(app2.getId(), ComplianceStageType.ID, null, 1);

    // All SBOMs are recent (< 1 day old) — age filter never triggers
    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE,
        new Date(now - 3000));
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE,
        new Date(now - 2000));

    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE,
        new Date(now - 1000));
    ThirdPartySbomMetadata app2Sbom2 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(now));

    evaluationQueueProducer.execute((JobExecutionContext) null);

    // All SBOMs pass age filter at every rank — no early break, iterates all ranks
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(
            app2Sbom2.getSbomVersion(),
            app1Sbom2.getSbomVersion(),
            app2Sbom1.getSbomVersion(),
            app1Sbom1.getSbomVersion());
    assertThat(logOutput).atDebugLevel()
        .contains("Processed 4 SBOM(s) up to latest offset 2.");
  }

  @Test
  public void testExecute_maxVersionsAndMaxAgeInDays_earlyBreak() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    String orgId = tempEntity.newOrganization().getId();
    Application app1 = tempEntity.newApplicationWithSpecificId("app-aaa", "App A", "app-aaa-public", orgId);
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    Application app2 = tempEntity.newApplicationWithSpecificId("app-bbb", "App B", "app-bbb-public", orgId);
    tempEntity.newPolicyMonitoring(app2.getId(), ComplianceStageType.ID);

    long now = System.currentTimeMillis();
    // app1: maxVersions=1 (filtered by version at rank 1+)
    // app2: maxAgeInDays=1, no maxVersions (filtered by age for old SBOMs)
    tempEntity.newVersionEvaluationWindow(app1.getId(), ComplianceStageType.ID, 1, null);
    tempEntity.newVersionEvaluationWindow(app2.getId(), ComplianceStageType.ID, null, 1);

    // app1: 3 SBOMs, all recent
    tempEntity.newThirdPartySbomMetadata("a1s1", app1.getId(), ACTIVE, new Date(now - 3000));
    tempEntity.newThirdPartySbomMetadata("a1s2", app1.getId(), ACTIVE, new Date(now - 2000));
    ThirdPartySbomMetadata app1Latest = tempEntity.newThirdPartySbomMetadata("a1s3", app1.getId(), ACTIVE,
        new Date(now - 1000));

    // app2: 3 SBOMs, newest is recent, older two are > 1 day old
    tempEntity.newThirdPartySbomMetadata("a2s1", app2.getId(), ACTIVE,
        new Date(now - Duration.ofDays(3).toMillis()));
    tempEntity.newThirdPartySbomMetadata("a2s2", app2.getId(), ACTIVE,
        new Date(now - Duration.ofDays(2).toMillis()));
    ThirdPartySbomMetadata app2Recent = tempEntity.newThirdPartySbomMetadata("a2s3", app2.getId(), ACTIVE,
        new Date(now));

    evaluationQueueProducer.execute((JobExecutionContext) null);

    // rank 0: app1Latest (passes maxVersions=1) + app2Recent (passes age) → both queued
    // rank 1: app1 filtered by maxVersions, app2's 2nd SBOM filtered by age → nothing passes → early break
    // (maxAppActiveSboms=3 but stopped at offset 2)
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(app2Recent.getSbomVersion(), app1Latest.getSbomVersion());
    assertThat(logOutput).atDebugLevel()
        .contains("Processed 4 SBOM(s) up to latest offset 2.");
    assertThat(logOutput).atInfoLevel().contains("Completed cycle.");
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

    evaluationQueueProducer.execute((JobExecutionContext) null);

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
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());

    // 0 rows added due to the limit
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());

    // Pretend sbom3 got processed
    evaluationQueueDAO.delete(
        evaluationQueueDAO.getAll()
            .stream()
            .filter(e -> e.getVersion().equals(sbom3.getSbomVersion()))
            .findFirst()
            .orElse(null));
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getVersion)
        .containsExactly(sbom2.getSbomVersion(), sbom1.getSbomVersion());

    // Pretend the rest got processed
    evaluationQueueDAO.getAll().forEach(evaluationQueueDAO::delete);
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(evaluationQueueDAO.getAll()).isEmpty();

    // Sleep for 2 seconds to ensure the cycle starts again
    Thread.sleep(2000);

    evaluationQueueProducer.execute((JobExecutionContext) null);
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

    evaluationQueueProducer.execute((JobExecutionContext) null);

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

    evaluationQueueProducer.execute((JobExecutionContext) null);

    List<EvaluationQueue> queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(2);
    assertThat(queued).map(EvaluationQueue::getVersion)
        .containsExactly(sbom3.getSbomVersion(), sbom2.getSbomVersion());

    Thread.sleep(1000);

    evaluationQueueProducer.execute((JobExecutionContext) null);
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
    Mockito.reset(mockTaskScheduler);

    evaluationQueueProducer.register();

    verify(mockTaskScheduler).unscheduleTask(evaluationQueueProducer);
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig enabledConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(enabledConfig);

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
  }

  @Test
  public void testConfigurationChanged_enabledChangedToFalse_shouldUnschedule() {
    EvaluationQueueConfig enabledConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(enabledConfig);
    Mockito.reset(mockTaskScheduler);

    evaluationQueueProducer.register();

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig disabledConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(disabledConfig);

    verify(mockTaskScheduler).unscheduleTask(evaluationQueueProducer);
  }

  @Test
  public void testConfigurationChanged_periodChanged_shouldReschedule() {
    EvaluationQueueConfig initialConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(initialConfig);
    Mockito.reset(mockTaskScheduler);

    evaluationQueueProducer.register();

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig newConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(2)).build();
    setEvaluationQueueConfig(newConfig);

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(2));
  }

  @Test
  public void testConfigurationChanged_periodUnchanged_shouldNotReschedule() {
    EvaluationQueueConfig initialConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(initialConfig);
    Mockito.reset(mockTaskScheduler);

    evaluationQueueProducer.register();

    verify(mockTaskScheduler).schedulePeriodicTask(evaluationQueueProducer, Duration.ofSeconds(1));
    Mockito.reset(mockTaskScheduler);

    EvaluationQueueConfig newConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(newConfig);

    verify(mockTaskScheduler, never()).schedulePeriodicTask(eq(evaluationQueueProducer), any(Duration.class));
  }

  @Test
  public void testConfigurationChanged_differentProperty_shouldNotReschedule() {
    EvaluationQueueConfig initialConfig =
        EvaluationQueueConfig.builder().enabled(true).producerPeriod(Duration.ofSeconds(1)).build();
    setEvaluationQueueConfig(initialConfig);
    Mockito.reset(mockTaskScheduler);

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
  public void testExecute_QuartzJob() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getMergedJobDataMap()).thenReturn(new JobDataMap());

    assertThatNoException().isThrownBy(() -> evaluationQueueProducer.execute(mockContext));
  }

  @Test
  public void testExecute_AdminTask_triggerTaskNow_noCycleReset() throws Exception {
    evaluationQueueProducer.register();
    Mockito.reset(mockTaskScheduler);
    when(mockTaskScheduler.isTaskScheduled(evaluationQueueProducer)).thenReturn(true);

    Map<String, List<String>> params = Map.of();
    evaluationQueueProducer.execute(params, new PrintWriter(OutputStream.nullOutputStream()));

    verify(mockTaskScheduler).triggerTaskNow(eq(evaluationQueueProducer),
        eq(Map.of("resetCycle", "false")));
  }

  @Test
  public void testExecute_AdminTask_triggerTaskNow_resetCycle() throws Exception {
    evaluationQueueProducer.register();
    Mockito.reset(mockTaskScheduler);
    when(mockTaskScheduler.isTaskScheduled(evaluationQueueProducer)).thenReturn(true);

    Map<String, List<String>> params = Map.of("resetCycle", List.of("true"));
    evaluationQueueProducer.execute(params, new PrintWriter(OutputStream.nullOutputStream()));

    verify(mockTaskScheduler).triggerTaskNow(eq(evaluationQueueProducer),
        eq(Map.of("resetCycle", "true")));
  }

  @Test
  public void testExecute_AdminTask_doesNot_triggerTaskNow_ifNotScheduled() throws Exception {
    when(mockTaskScheduler.isTaskScheduled(evaluationQueueProducer)).thenReturn(false);

    Map<String, List<String>> params = Map.of("resetCycle", List.of("true"));
    evaluationQueueProducer.execute(params, new PrintWriter(OutputStream.nullOutputStream()));

    verify(mockTaskScheduler, never()).triggerTaskNow(any(), any());
  }

  @Test
  public void testExecute_unparseableCheckpoint() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    // Add unparseable content for the EVALUATION_QUEUE_PRODUCER_CHECKPOINT key
    tempEntity.newKeyValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT, "{");

    evaluationQueueProducer.execute((JobExecutionContext) null);

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
    evaluationQueueProducer.execute((JobExecutionContext) null);
    List<EvaluationQueue> queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(1);
    assertThat(queued).map(EvaluationQueue::getVersion).containsExactly(sbom1.getSbomVersion());

    // Second SBOM gets added and the queue gets reset
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));
    Thread.sleep(1000);
    evaluationQueueProducer.execute((JobExecutionContext) null);
    // Even though the second SBOM is more recent, since the queue isn't reset it gets added after the first SBOM
    queued = evaluationQueueDAO.getAll();
    assertThat(queued).hasSize(2);
    assertThat(queued)
        .map(evaluationQueue -> Pair.of(evaluationQueue.getPriority(), evaluationQueue.getVersion()))
        .containsExactlyInAnyOrder(Pair.of(0, sbom1.getSbomVersion()), Pair.of(1, sbom2.getSbomVersion()));
  }

  @Test
  public void testExecute_waitsForPolicyMonitoringHour() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    EvaluationQueueProducerCheckpoint checkpoint = new EvaluationQueueProducerCheckpoint(
        new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()),
        null, 0, 1);
    tempEntity.newKeyValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT,
        JsonUtils.writeUnformatted(checkpoint));

    evaluationQueueProducer.execute((JobExecutionContext) null);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Waiting for policy monitoring hour, skipping execution.");
  }

  @Test
  public void testExecute_resetCycleWithStartTimeOverride() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    // Pre-seed a checkpoint with a future start time
    EvaluationQueueProducerCheckpoint checkpoint = new EvaluationQueueProducerCheckpoint(
        new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()),
        null, 0, 1);
    tempEntity.newKeyValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT,
        JsonUtils.writeUnformatted(checkpoint));

    // resetCycle + startTime=now starts a fresh cycle immediately
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put("resetCycle", "true");
    jobDataMap.put("startTime", String.valueOf(System.currentTimeMillis()));

    evaluationQueueProducer.execute(mockJobExecutionContext);

    assertThat(evaluationQueueDAO.getAll()).hasSize(1);
  }

  @Test
  public void testExecute_resetCycleStillWaitsForStartTime() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    // Complete a cycle first so resetCycle has something to reset
    evaluationQueueProducer.execute((JobExecutionContext) null);
    assertThat(evaluationQueueDAO.getAll()).hasSize(1);
    evaluationQueueDAO.getAll().forEach(evaluationQueueDAO::delete);

    // resetCycle clears queue + checkpoint, but startTime in the future means it still waits
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put("resetCycle", "true");
    jobDataMap.put("startTime", String.valueOf(System.currentTimeMillis() + Duration.ofHours(1).toMillis()));

    evaluationQueueProducer.execute(mockJobExecutionContext);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Waiting for policy monitoring hour, skipping execution.");
  }

  @Test
  public void testExecute_startTimeOverrideWithFutureTime_waits() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app.getId(), ComplianceStageType.ID);
    tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "fileName");

    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put("startTime", String.valueOf(System.currentTimeMillis() + Duration.ofHours(1).toMillis()));

    evaluationQueueProducer.execute(mockJobExecutionContext);

    assertThat(evaluationQueueDAO.getCount()).isZero();
    assertThat(logOutput).atDebugLevel().contains("Waiting for policy monitoring hour, skipping execution.");
  }

  @Test
  public void testExecute_manualTriggerPassesStartTime() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());

    String startTime = String.valueOf(System.currentTimeMillis());
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("startTime", startTime);
    when(mockContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    evaluationQueueProducer.execute(mockContext);
  }

  @Test
  public void testExecute_manualTriggerStartTimeNow() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());

    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("startTime", String.valueOf(System.currentTimeMillis()));
    when(mockContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    evaluationQueueProducer.execute(mockContext);
  }

  @Test
  public void testGetInitialCycleStartTime_nullHour_returnsNow() {
    long now = System.currentTimeMillis();

    Date result = EvaluationQueueProducer.getInitialCycleStartTime(null, 0, now);

    assertThat(result).isEqualTo(new Date(now));
  }

  @Test
  public void testGetInitialCycleStartTime_hourAlreadyPassedToday_returnsTomorrow() {
    LocalDateTime pastToday =
        LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    long now = pastToday.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getInitialCycleStartTime(0, 0, now);

    long expectedTomorrow = pastToday.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expectedTomorrow));
  }

  @Test
  public void testGetInitialCycleStartTime_hourNotYetReachedToday_returnsToday() {
    LocalDateTime todayAt22 =
        LocalDateTime.now().withHour(22).withMinute(0).withSecond(0).withNano(0);
    long now = todayAt22.minusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getInitialCycleStartTime(22, 0, now);

    long expectedToday = todayAt22.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expectedToday));
  }

  @Test
  public void testGetInitialCycleStartTime_exactlyAtHour_returnsTomorrow() {
    LocalDateTime todayAtMidnight = LocalDateTime.now()
        .withHour(0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0);
    long now = todayAtMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getInitialCycleStartTime(0, 0, now);

    long expectedTomorrow =
        todayAtMidnight.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expectedTomorrow));
  }

  @Test
  public void testGetRenewalCycleStartTime_nullHour_returnsNow() {
    long now = System.currentTimeMillis();

    Date result = EvaluationQueueProducer.getRenewalCycleStartTime(null, 0, now);

    assertThat(result).isEqualTo(new Date(now));
  }

  @Test
  public void testGetRenewalCycleStartTime_hourAlreadyPassedToday_returnsToday() {
    LocalDateTime todayAtMidnight =
        LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    long now = todayAtMidnight.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getRenewalCycleStartTime(0, 0, now);

    long expectedToday = todayAtMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expectedToday));
  }

  @Test
  public void testGetRenewalCycleStartTime_hourNotYetReachedToday_returnsToday() {
    LocalDateTime todayAt22 =
        LocalDateTime.now().withHour(22).withMinute(0).withSecond(0).withNano(0);
    long now = todayAt22.minusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getRenewalCycleStartTime(22, 0, now);

    long expectedToday = todayAt22.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expectedToday));
  }

  @Test
  public void testGetInitialCycleStartTime_jitterOffsetsStartTime() {
    LocalDateTime todayAt10 = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
    long now = todayAt10.minusHours(5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getInitialCycleStartTime(10, 45, now);

    long expected = todayAt10.plusMinutes(45).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expected));
  }

  @Test
  public void testGetRenewalCycleStartTime_jitterOffsetsStartTime() {
    LocalDateTime todayAt10 = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
    long now = todayAt10.plusHours(5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getRenewalCycleStartTime(10, 45, now);

    long expected = todayAt10.plusMinutes(45).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expected));
  }

  @Test
  public void testGetInitialCycleStartTime_jitterWrapsPastMidnight() {
    LocalDateTime todayAt23 = LocalDateTime.now().withHour(23).withMinute(0).withSecond(0).withNano(0);
    long now = todayAt23.minusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getInitialCycleStartTime(23, 119, now);

    long expected = todayAt23.plusMinutes(119).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expected));
  }

  @Test
  public void testGetRenewalCycleStartTime_jitterWrapsPastMidnight() {
    LocalDateTime todayAt23 = LocalDateTime.now().withHour(23).withMinute(0).withSecond(0).withNano(0);
    long now = todayAt23.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Date result = EvaluationQueueProducer.getRenewalCycleStartTime(23, 119, now);

    long expected = todayAt23.plusMinutes(119).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    assertThat(result).isEqualTo(new Date(expected));
  }

  private void setEvaluationQueueConfig(final EvaluationQueueConfig evaluationQueueConfig) {
    Map<String, Object> configMap = JsonUtils.convertValue(evaluationQueueConfig, Map.class);
    configMap.put("startTimeDelayEnabled", false);
    apiConfigurationService.setConfigurationNoAuthz(Map.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG,
        configMap));
  }
}
