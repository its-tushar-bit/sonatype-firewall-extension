/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.consumption.ConsumptionContext;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class EvaluationQueueConsumerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(EvaluationQueueConsumer.class);

  @Inject
  private EvaluationQueueConsumer evaluationQueueConsumer;

  @Inject
  private EvaluationQueueProducer evaluationQueueProducer;

  @Mock
  private EvaluationQueueService mockEvaluationQueueService;

  @Inject
  private ApiConfigurationService apiConfigurationService;

  @Inject
  private ScanPersistenceService scanPersistenceService;

  @Mock
  private ScanUploadService mockScanUploadService;

  @Mock
  private ScanPolicyEvaluator mockScanPolicyEvaluator;

  @Mock
  private PolicyAlertNotifier mockPolicyAlertNotifier;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private QuartzJobStoreTX mockQuartzJobStoreTX;

  @Before
  public void before() {
    evaluationQueueProducer.disableForTesting = true;
    lenient().when(mockQuartzJobStoreTX.getInstanceId()).thenReturn("worker");
    lenient().when(mockEvaluationQueueService.getInstanceId()).thenReturn("worker");
  }

  @After
  public void after() {
    evaluationQueueConsumer.cleanup();
  }

  @Test
  public void testRegister() {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerPeriod(Duration.ofMillis(1000))
        .build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.register();

    await().atMost(2000, TimeUnit.MILLISECONDS)
        .until(() -> mockingDetails(mockEvaluationQueueService).getInvocations()
            .stream()
            .filter(i -> i.getMethod().getName().equals("acquireRows"))
            .count() >= 2);
    verify(mockEvaluationQueueService, times(2)).acquireRows(
        config.consumerMaxQueuedRows() + config.consumerThreadsPerTenant());
  }

  @Test
  public void testRegister_disabled() {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.register();

    logOutput.assertThat().atDebugLevel().contains("Unscheduling evaluation queue consumer.");
    verifyNoInteractions(mockEvaluationQueueService);
  }

  @Test
  public void testRun_disabled() throws Exception {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.run();

    logOutput.assertThat().atDebugLevel().contains("Evaluation queue consumer is disabled, skipping execution.");
    verifyNoInteractions(mockEvaluationQueueService);
  }

  @Test
  public void testRun_enabled() throws Exception {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerMaxQueuedRows(EvaluationQueueConfig.DEFAULT_CONSUMER_MAX_QUEUED_ROWS + 1)
        .build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.run();

    verify(mockEvaluationQueueService).acquireRows(config.consumerMaxQueuedRows() + config.consumerThreadsPerTenant());
  }

  @Test
  public void testRun_noRowsToAcquire() throws Exception {
    // Only allow 1 row to be added to the queue
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerThreadsPerTenant(1)
        .consumerMaxQueuedRows(1)
        .build();
    setEvaluationQueueConfig(config);

    // Delay execution so we can ensure the queue fills up
    CountDownLatch evaluationStarted = new CountDownLatch(1);
    CountDownLatch releaseEvaluation = new CountDownLatch(1);
    EvaluationQueueService spyEvaluationQueueService = spy(
        new EvaluationQueueService(mockQuartzJobStoreTX, daoFactory.createEvaluationQueueDAO()));
    EvaluationQueueConsumer blockingEvaluationQueueConsumer = new EvaluationQueueConsumer(
        apiConfigurationService,
        spyEvaluationQueueService,
        lookup(ThirdPartySbomMetadataDAO.class),
        lookup(ApplicationDAO.class),
        lookup(ThirdPartyScanDAO.class),
        lookup(PolicyEvaluationDAO.class),
        daoFactory.createEvaluationQueueDAO(),
        scanPersistenceService,
        mockScanUploadService,
        mockScanPolicyEvaluator,
        mockPolicyAlertNotifier,
        mockShutdownHandler,
        mockProductLicense)
    {
      @Override
      void evaluate(final EvaluationQueue item) throws IOException, InterruptedException {
        evaluationStarted.countDown();
        releaseEvaluation.await(2, TimeUnit.SECONDS);
        super.evaluate(item);
      }
    };

    // Create some items to execute
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, app.getId(), ComplianceStageType.ID, "2.0.0", new Date(0), new Date(0), null);
    tempEntity.newEvaluationQueue(2, app.getId(), ComplianceStageType.ID, "1.0.0", new Date(0), new Date(0), null);

    try {
      // First run should start executing 1 item and queue 1 item
      blockingEvaluationQueueConsumer.run();
      verify(spyEvaluationQueueService).acquireRows(2);
      assertThat(evaluationStarted.await(2, TimeUnit.SECONDS)).isTrue();
      clearInvocations(spyEvaluationQueueService);

      // Second run should add nothing and not try to acquire rows once the executor is already full
      blockingEvaluationQueueConsumer.run();
      verifyNoInteractions(spyEvaluationQueueService);
    }
    finally {
      releaseEvaluation.countDown();
      blockingEvaluationQueueConsumer.cleanup();
    }
  }

  @Test
  public void testRun_disallowsConcurrentExecution() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    ApiConfigurationService blockingApiConfigurationService = mock(ApiConfigurationService.class);
    doAnswer(invocation -> {
      latch.await(5, TimeUnit.SECONDS);
      return EvaluationQueueConfig.builder().enabled(true).build();
    }).when(blockingApiConfigurationService)
        .getConfigurationNoAuthz(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG);
    applyBeanFieldOverride(EvaluationQueueConsumer.class, "apiConfigurationService", blockingApiConfigurationService);

    new Thread(() -> {
      try {
        evaluationQueueConsumer.run();
      }
      catch (InterruptedException e) {
        throw new UncheckedInterruptedException(e);
      }
    }).start();

    evaluationQueueConsumer.run();

    latch.countDown();
    logOutput.assertThat().atDebugLevel().contains("Evaluation queue consumer is already running, skipping execution.");

    logOutput.clear();
    evaluationQueueConsumer.run();
    assertThat(logOutput).atDebugLevel().contains("Starting evaluation queue consumer.");
  }

  @Test
  public void testRun_exceptionUnacquiresRow() throws Exception {
    setEvaluationQueueConfig(EvaluationQueueConfig.builder().enabled(true).build());
    EvaluationQueueConsumer spyEvaluationQueueConsumer = spy(evaluationQueueConsumer);
    lenient().doAnswer((invocation) -> {
      throw new IOException("some exception");
    }).when(spyEvaluationQueueConsumer).evaluate(any());
    Application app = tempEntity.newApplicationWithParent();
    EvaluationQueue evaluationQueue =
        tempEntity.newEvaluationQueue(1, app.getId(), ComplianceStageType.ID, "2.0.0", new Date(0), new Date(0), null);

    spyEvaluationQueueConsumer.run();

    EvaluationQueueDAO evaluationQueueDAO = daoFactory.createEvaluationQueueDAO();
    await().atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(evaluationQueueDAO.getById(evaluationQueue.getId()).getWorkerId()).isNull());
  }

  @Test
  public void testEvaluate_unsupported() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    EvaluationQueue item =
        tempEntity.newEvaluationQueue(1, app.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0), null);
    EvaluationQueueConsumer spyEvaluationQueueConsumer = spy(evaluationQueueConsumer);

    spyEvaluationQueueConsumer.evaluate(item);

    logOutput.assertThat()
        .atWarnLevel()
        .contains(
            "Unsupported evaluation queue item with priority %s for application id %s, stage %s, and version %s."
                .formatted(
                    item.getPriority(), item.getApplicationId(), item.getStageTypeId(), item.getVersion()));
    verify(spyEvaluationQueueConsumer, never()).evaluateSbom(item);
    EvaluationQueueDAO evaluationQueueDAO = daoFactory.createEvaluationQueueDAO();
    assertThat(evaluationQueueDAO.getById(item.getId())).isNull();
  }

  @Test
  public void testEvaluate_supported() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    EvaluationQueue item =
        tempEntity.newEvaluationQueue(1, app.getId(), ComplianceStageType.ID, "1.0.0", new Date(0), new Date(0), null);
    EvaluationQueueConsumer spyEvaluationQueueConsumer = spy(evaluationQueueConsumer);

    spyEvaluationQueueConsumer.evaluate(item);

    verify(spyEvaluationQueueConsumer).evaluateSbom(item);
    EvaluationQueueDAO evaluationQueueDAO = daoFactory.createEvaluationQueueDAO();
    assertThat(evaluationQueueDAO.getById(item.getId())).isNull();
  }

  @Test
  public void testEvaluateSbom_noSbom() throws Exception {
    EvaluationQueue item = new EvaluationQueue();
    item.setApplicationId("doesNotExist");
    item.setVersion("1.0");

    evaluationQueueConsumer.evaluateSbom(item);

    logOutput.assertThat().atDebugLevel().contains("No SBOM found for application id doesNotExist and version 1.0.");
  }

  @Test
  public void testEvaluateSbom_noApp() throws Exception {
    ThirdPartySbomMetadata sbom =
        tempEntity.newThirdPartySbomMetadata("doesNotExist", ThirdPartySbomMetadataStatus.ACTIVE, new Date());
    EvaluationQueue item = new EvaluationQueue();
    item.setApplicationId(sbom.getApplicationId());
    item.setVersion(sbom.getSbomVersion());

    evaluationQueueConsumer.evaluateSbom(item);

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "No application found for SBOM with application id doesNotExist and version %s.".formatted(
                sbom.getSbomVersion()));
  }

  @Test
  public void testEvaluateSbom_noThirdPartyScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbom =
        tempEntity.newThirdPartySbomMetadata(app.getId(), ThirdPartySbomMetadataStatus.ACTIVE, new Date());
    ThirdPartyScanDAO thirdPartyScanDAO = daoFactory.createThirdPartyScanDAO();
    ThirdPartyScan thirdPartyScan = thirdPartyScanDAO.getByThirdPartyFileId(sbom.getThirdPartyFileId());
    assertThat(thirdPartyScan).isNull();
    EvaluationQueue item = new EvaluationQueue();
    item.setApplicationId(sbom.getApplicationId());
    item.setVersion(sbom.getSbomVersion());

    evaluationQueueConsumer.evaluateSbom(item);

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "No third party scan found with id %s for SBOM with application id %s and version %s.".formatted(
                sbom.getThirdPartyFileId(), sbom.getApplicationId(), sbom.getSbomVersion()));
  }

  @Test
  public void testEvaluateSbom_nullFilteredScanFile() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbom =
        tempEntity.newThirdPartySbomMetadata(app.getId(), ThirdPartySbomMetadataStatus.ACTIVE, new Date());
    ThirdPartyFileDAO thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFileDAO.getById(sbom.getThirdPartyFileId()));
    assertThat(thirdPartyScan.getFilteredScanFile()).isNull();
    EvaluationQueue item = new EvaluationQueue();
    item.setApplicationId(sbom.getApplicationId());
    item.setVersion(sbom.getSbomVersion());

    evaluationQueueConsumer.evaluateSbom(item);

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            ("No filtered scan found at %s for third party scan with id %s for SBOM with application id %s and" +
                " version %s.").formatted(thirdPartyScan.getFilteredScanFile(), thirdPartyScan.getId(),
                    sbom.getApplicationId(), sbom.getSbomVersion()));
  }

  @Test
  public void testEvaluateSbom_noFilteredScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbom =
        tempEntity.newThirdPartySbomMetadata(app.getId(), ThirdPartySbomMetadataStatus.ACTIVE, new Date());
    ThirdPartyFileDAO thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getById(sbom.getThirdPartyFileId());
    ThirdPartyScan thirdPartyScan =
        tempEntity.newThirdPartyScan("scanRequestId", "scanId", thirdPartyFile, "filteredScanFile");
    EvaluationQueue item = new EvaluationQueue();
    item.setApplicationId(sbom.getApplicationId());
    item.setVersion(sbom.getSbomVersion());

    evaluationQueueConsumer.evaluateSbom(item);

    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            ("No filtered scan found at %s for third party scan with id %s for SBOM with application id %s and" +
                " version %s.").formatted(thirdPartyScan.getFilteredScanFile(), thirdPartyScan.getId(),
                    sbom.getApplicationId(), sbom.getSbomVersion()));
  }

  @Test
  public void testEvaluateSbom_withLastPolicyEvaluation() throws Exception {
    testEvaluateSbom(true);
  }

  @Test
  public void testEvaluateSbom_withoutLastPolicyEvaluation() throws Exception {
    testEvaluateSbom(false);
  }

  private void testEvaluateSbom(final boolean withLastPolicyEvaluation) throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbom =
        tempEntity.newThirdPartySbomMetadata(app.getId(), ThirdPartySbomMetadataStatus.ACTIVE, new Date());
    ThirdPartyFileDAO thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getById(sbom.getThirdPartyFileId());
    ThirdPartyScan thirdPartyScan =
        tempEntity.newThirdPartyScan("scanRequestId", "scanId", thirdPartyFile, "filteredScanFile");
    ScanEntity filteredScan =
        scanPersistenceService.getScanByName(sbom.getApplicationId(), thirdPartyScan.getFilteredScanFile());
    try (OutputStream outputStream = filteredScan.getOutputStream()) {
      outputStream.write("dummy content".getBytes(StandardCharsets.UTF_8));
    }
    EvaluationQueue item = new EvaluationQueue();
    item.setApplicationId(sbom.getApplicationId());
    item.setVersion(sbom.getSbomVersion());
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("newScanId");
    when(mockScanUploadService.upload(any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(
        scanReceipt);
    if (withLastPolicyEvaluation) {
      tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, thirdPartyScan.getScanId(),
          ClientScanType.SONATYPE);
    }
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = new ScanPolicyEvaluatorResults();
    when(mockScanPolicyEvaluator.evaluateForMonitoring(any(), any(), any(), any(), any())).thenReturn(
        scanPolicyEvaluatorResults);

    evaluationQueueConsumer.evaluateSbom(item);

    ArgumentCaptor<ScanEntity> scanEntityArgumentCaptor = ArgumentCaptor.forClass(ScanEntity.class);
    ArgumentCaptor<Application> applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
    verify(mockScanUploadService).upload(
        scanEntityArgumentCaptor.capture(),
        applicationArgumentCaptor.capture(),
        eq(ComplianceStageType.ID),
        eq(ClientScanType.SONATYPE),
        eq(null),
        eq(null),
        eq(null),
        eq(false));
    ScanEntity capturedScanEntity = scanEntityArgumentCaptor.getValue();
    assertThat(capturedScanEntity.getLocation()).isEqualTo(filteredScan.getLocation());
    Application capturedApplication = applicationArgumentCaptor.getValue();
    assertThat(capturedApplication.getId()).isEqualTo(sbom.getApplicationId());
    ThirdPartyScanDAO thirdPartyScanDAO = daoFactory.createThirdPartyScanDAO();
    thirdPartyScan = thirdPartyScanDAO.getById(thirdPartyScan.getId());
    assertThat(thirdPartyScan.getPreviousScanId()).isEqualTo("scanId");
    assertThat(thirdPartyScan.getScanId()).isEqualTo("newScanId");

    applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
    verify(mockScanPolicyEvaluator).evaluateForMonitoring(
        applicationArgumentCaptor.capture(),
        eq(scanReceipt.getScanId()),
        eq(new Stage(ComplianceStageType.ID)),
        eq(withLastPolicyEvaluation ? ScanTriggerType.CLI : ScanTriggerType.SBOM_UI),
        eq(withLastPolicyEvaluation ? ClientScanType.SONATYPE : ClientScanType.SONATYPE_THIRD_PARTY));
    capturedApplication = applicationArgumentCaptor.getValue();
    assertThat(capturedApplication.getId()).isEqualTo(sbom.getApplicationId());

    applicationArgumentCaptor = ArgumentCaptor.forClass(Application.class);
    verify(mockPolicyAlertNotifier).sendNotifications(
        applicationArgumentCaptor.capture(),
        eq(scanPolicyEvaluatorResults));
  }

  @Test
  public void testConfigurationChanged_differentProperty() {
    ApiConfigurationService unusedApiConfigurationService = spy(apiConfigurationService);
    applyBeanFieldOverride(EvaluationQueueConsumer.class, "apiConfigurationService", unusedApiConfigurationService);

    evaluationQueueConsumer.configurationChanged(Set.of("unrelated"));

    verifyNoInteractions(unusedApiConfigurationService);
  }

  @Test
  public void testConfigurationChanged_evaluationQueueConfig_consumerPeriodChanged() {
    EvaluationQueueConfig initialConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .build();
    setEvaluationQueueConfig(initialConfig);
    evaluationQueueConsumer.register();
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerPeriod(EvaluationQueueConfig.DEFAULT_CONSUMER_PERIOD.plusMillis(1))
        .build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    logOutput.assertThat()
        .atDebugLevel()
        .contains("Scheduling evaluation queue consumer every " + config.consumerPeriodInMilliseconds()
            + " ms with initial delay");
  }

  @Test
  public void testConfigurationChanged_evaluationQueueConfig_enabledChangedToTrue_shouldSchedule() {
    EvaluationQueueConfig disabledConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(disabledConfig);
    evaluationQueueConsumer.register();

    EvaluationQueueConfig enabledConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerPeriod(Duration.ofMillis(1000))
        .build();
    setEvaluationQueueConfig(enabledConfig);

    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    await().atMost(2000, TimeUnit.MILLISECONDS)
        .until(() -> mockingDetails(mockEvaluationQueueService).getInvocations()
            .stream()
            .filter(i -> i.getMethod().getName().equals("acquireRows"))
            .count() >= 1);
    logOutput.assertThat()
        .atDebugLevel()
        .contains(
            "Scheduling evaluation queue consumer every " + enabledConfig.consumerPeriodInMilliseconds()
                + " ms with initial delay");
  }

  @Test
  public void testConfigurationChanged_evaluationQueueConfig_enabledChangedToFalse_shouldUnschedule() {
    EvaluationQueueConfig enabledConfig = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerPeriod(Duration.ofMillis(1000))
        .build();
    setEvaluationQueueConfig(enabledConfig);
    evaluationQueueConsumer.register();
    await().atMost(2000, TimeUnit.MILLISECONDS)
        .until(() -> mockingDetails(mockEvaluationQueueService).getInvocations()
            .stream()
            .filter(i -> i.getMethod().getName().equals("acquireRows"))
            .count() >= 1);

    EvaluationQueueConfig disabledConfig = EvaluationQueueConfig.builder().enabled(false).build();
    setEvaluationQueueConfig(disabledConfig);

    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    logOutput.assertThat().atDebugLevel().contains("Unscheduling evaluation queue consumer.");
  }

  @Test
  public void testConfigurationChanged_evaluationQueueConfig_consumerThreadsPerTenantIncreased() {
    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .consumerThreadsPerTenant(EvaluationQueueConfig.DEFAULT_CONSUMER_THREADS_PER_TENANT + 1)
        .build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    logOutput.assertThat()
        .atDebugLevel()
        .contains("Increased evaluation queue consumer thread pool size to " + config.consumerThreadsPerTenant() + ".");
  }

  @Test
  public void testConfigurationChanged_evaluationQueueConfig_consumerThreadsPerTenantDecreased() {
    EvaluationQueueConfig initialConfig = EvaluationQueueConfig.builder()
        .consumerThreadsPerTenant(2)
        .build();
    setEvaluationQueueConfig(initialConfig);
    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .consumerThreadsPerTenant(initialConfig.consumerThreadsPerTenant() - 1)
        .build();
    setEvaluationQueueConfig(config);

    evaluationQueueConsumer.configurationChanged(Set.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG));

    logOutput.assertThat()
        .atDebugLevel()
        .contains("Decreased evaluation queue consumer thread pool size to " + config.consumerThreadsPerTenant() + ".");
  }

  @Test
  public void testCreateScheduledExecutorService() {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = evaluationQueueConsumer.createScheduledExecutorService();

    assertThat(scheduledThreadPoolExecutor).isNotNull();
    assertThat(scheduledThreadPoolExecutor.getCorePoolSize()).isEqualTo(1);
    Thread thread = scheduledThreadPoolExecutor.getThreadFactory().newThread(() -> {
    });
    assertThat(thread.getName()).isEqualTo("EvaluationQueueConsumerScheduler-0");
    verify(mockShutdownHandler).add(scheduledThreadPoolExecutor);
  }

  @Test
  public void testCreateExecutorService() throws Exception {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .consumerThreadsPerTenant(1)
        .build();
    setEvaluationQueueConfig(config);

    TenantThreadPoolExecutor threadPoolExecutor = evaluationQueueConsumer.createExecutorService();

    assertThat(threadPoolExecutor).isNotNull();
    assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(config.consumerThreadsPerTenant());
    assertThat(threadPoolExecutor.getMaximumPoolSize()).isEqualTo(config.consumerThreadsPerTenant());
    Thread thread = threadPoolExecutor.getThreadFactory().newThread(() -> {
    });
    assertThat(thread.getName()).isEqualTo("EvaluationQueueConsumer-0");
    assertThat(threadPoolExecutor.allowsCoreThreadTimeOut()).isTrue();
    verify(mockShutdownHandler).add(threadPoolExecutor);

    Application app = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(1, app.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    threadPoolExecutor.submit(evaluationQueueConsumer.new EvaluationQueueTask(item1, () -> {
    }));
    CountDownLatch blockingTaskStarted = new CountDownLatch(1);
    CountDownLatch releaseBlockingTask = new CountDownLatch(1);
    Future<?> future = threadPoolExecutor.submit(() -> {
      try {
        blockingTaskStarted.countDown();
        releaseBlockingTask.await();
      }
      catch (InterruptedException e) {
        throw new UncheckedInterruptedException(e);
      }
    });
    blockingTaskStarted.await();
    EvaluationQueue item2 =
        tempEntity.newEvaluationQueue(1, app.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1), null);
    threadPoolExecutor.submit(evaluationQueueConsumer.new EvaluationQueueTask(item2, () -> {
    }));
    threadPoolExecutor.shutdown();
    assertThat(threadPoolExecutor.getQueue()).isEmpty();
    releaseBlockingTask.countDown();
    assertThatNoException().isThrownBy(future::get);
    verify(mockEvaluationQueueService).unacquireRows(Set.of(item2.getId()));
  }

  @Test
  public void testExecute_AdminTask_callsRun() throws Exception {
    EvaluationQueueConsumer spyEvaluationQueueConsumer = spy(evaluationQueueConsumer);

    spyEvaluationQueueConsumer.execute(null, new PrintWriter(OutputStream.nullOutputStream()));

    verify(spyEvaluationQueueConsumer).run();
  }

  @Test
  public void testEvaluate_scopesConsumptionContextToInternalApplicationId() throws Exception {
    EvaluationQueueConsumer spyEvaluationQueueConsumer = spy(evaluationQueueConsumer);
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);
    try {
      Application app = tempEntity.newApplicationWithParent();
      EvaluationQueue item =
          tempEntity.newEvaluationQueue(1, app.getId(), ComplianceStageType.ID, "1.0.0", new Date(0), new Date(0),
              null);

      AtomicReference<String> capturedAppId = new AtomicReference<>();
      doAnswer(invocation -> {
        ConsumptionContext ctx = ConsumptionContext.get();
        capturedAppId.set(ctx == null ? null : ctx.getAppId());
        return null;
      }).when(spyEvaluationQueueConsumer).evaluateSbom(item);

      spyEvaluationQueueConsumer.evaluate(item);

      assertThat(capturedAppId.get()).isEqualTo(app.getId());
    }
    finally {
      SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    }
  }

  private void setEvaluationQueueConfig(final EvaluationQueueConfig evaluationQueueConfig) {
    apiConfigurationService.setConfiguration(Map.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG,
        JsonUtils.convertValue(evaluationQueueConfig, Map.class)));
  }
}
