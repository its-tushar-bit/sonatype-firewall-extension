/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.scan.model.ClientScanType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.function.ThrowingConsumer;

@Named
@Singleton
public class EvaluationQueueConsumer
    extends AdminTask
    implements ConfigurationListener, TenantManaged
{
  public static final String PATH = "EvaluationQueueConsumer";

  private static final Logger log = LoggerFactory.getLogger(EvaluationQueueConsumer.class);

  private final ApiConfigurationService apiConfigurationService;

  private final EvaluationQueueService evaluationQueueService;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ApplicationDAO applicationDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final EvaluationQueueDAO evaluationQueueDAO;

  private final ScanPersistenceService scanPersistenceService;

  private final ScanUploadService scanUploadService;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final TenantReference<EvaluationQueueConfig> evaluationQueueConfigs;

  private final TenantReference<ScheduledFuture<?>> scheduledFutures;

  private final TenantReference<TenantScheduledThreadPoolExecutor> scheduleExecutors;

  private final TenantReference<TenantThreadPoolExecutor> executors;

  private final TenantReference<Set<String>> queuedItemIds;

  private final TenantReference<AtomicBoolean> running;

  private final TenantReference<AtomicInteger> acquired = new TenantReference<>(AtomicInteger::new);

  private final TenantReference<AtomicInteger> evaluated = new TenantReference<>(AtomicInteger::new);

  private final TenantReference<AtomicInteger> failed = new TenantReference<>(AtomicInteger::new);

  private final TenantReference<AtomicInteger> skipped = new TenantReference<>(AtomicInteger::new);

  private final ShutdownHandler shutdownHandler;

  public boolean disableForTesting;

  @Inject
  public EvaluationQueueConsumer(
      final ApiConfigurationService apiConfigurationService,
      final EvaluationQueueService evaluationQueueService,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ApplicationDAO applicationDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final EvaluationQueueDAO evaluationQueueDAO,
      final ScanPersistenceService scanPersistenceService,
      final ScanUploadService scanUploadService,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyAlertNotifier policyAlertNotifier,
      final ShutdownHandler shutdownHandler)
  {
    super(PATH);
    this.apiConfigurationService = apiConfigurationService;
    this.evaluationQueueService = evaluationQueueService;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.applicationDAO = applicationDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.evaluationQueueDAO = evaluationQueueDAO;
    this.scanPersistenceService = scanPersistenceService;
    this.scanUploadService = scanUploadService;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.evaluationQueueConfigs = new TenantReference<>(this::getEvaluationQueueConfig);
    this.scheduleExecutors = new TenantReference<>(this::createScheduledExecutorService);
    this.scheduledFutures = new TenantReference<>(() -> null);
    this.executors = new TenantReference<>(this::createExecutorService);
    this.queuedItemIds = new TenantReference<>(ConcurrentHashMap::newKeySet);
    this.running = new TenantReference<>(AtomicBoolean::new);
    this.shutdownHandler = shutdownHandler;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    log.info("Manual request to run {}.", PATH);
    run();
    output.write("Completed manual execution of " + PATH + ".\n");
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    reschedule(evaluationQueueConfigs.get());
  }

  private String getJitterSeed() {
    return evaluationQueueService.getInstanceId() + TenantThreadLocal.getTenant().tenantSlug;
  }

  private void reschedule(final EvaluationQueueConfig config) {
    if (config.enabled()) {
      long initialDelay = getInitialDelay(getJitterSeed(), config.consumerPeriodInMilliseconds());
      log.debug("Scheduling evaluation queue consumer every {} ms with initial delay {} ms.",
          config.consumerPeriodInMilliseconds(), initialDelay);
      cancelScheduledFutureIfNeeded();
      scheduledFutures.set(scheduleExecutors.get()
          .scheduleAtFixedRate(
              this::tryRun,
              initialDelay,
              config.consumerPeriodInMilliseconds(),
              TimeUnit.MILLISECONDS));
    }
    else {
      log.debug("Unscheduling evaluation queue consumer.");
      cancelScheduledFutureIfNeeded();
    }
  }

  static long getInitialDelay(final String jitterSeed, final long periodInMilliseconds) {
    return Integer.toUnsignedLong(jitterSeed.hashCode()) % periodInMilliseconds;
  }

  private void cancelScheduledFutureIfNeeded() {
    ScheduledFuture<?> scheduledFuture = scheduledFutures.remove();
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
  }

  private EvaluationQueueConfig getEvaluationQueueConfig() {
    return (EvaluationQueueConfig) apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG);
  }

  private void tryRun() {
    try {
      run();
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }
  }

  // Visible for testing
  void run() throws InterruptedException {
    try {
      if (running.get().getAndSet(true)) {
        log.debug("Evaluation queue consumer is already running, skipping execution.");
        return;
      }
      EvaluationQueueConfig config = evaluationQueueConfigs.get();
      if (!config.enabled()) {
        log.debug("Evaluation queue consumer is disabled, skipping execution.");
        return;
      }
      logSummary();
      long start = System.currentTimeMillis();
      log.debug("Starting evaluation queue consumer.");
      ThreadPoolExecutor executor = executors.get();
      int rowsToAcquire = (executor.getCorePoolSize() - executor.getActiveCount()) + config.consumerMaxQueuedRows() -
          executor.getQueue().size();
      if (rowsToAcquire > 0) {
        List<EvaluationQueue> queued = evaluationQueueService.acquireRows(rowsToAcquire);
        queued.forEach(item -> executor.submit(new EvaluationQueueTask(item, () -> tryEvaluate(item))));
        acquired.get().addAndGet(queued.size());
        log.debug("Acquired {} items for evaluation, pool state: {}.", queued.size(), executor);
      }
      else {
        log.debug("No rows to acquire.");
      }
      log.debug("Finished evaluation queue consumer in {} ms.", System.currentTimeMillis() - start);
    }
    finally {
      running.get().set(false);
    }
  }

  private void logSummary() {
    int acquiredCount = acquired.get().getAndSet(0);
    int evaluatedCount = evaluated.get().getAndSet(0);
    int failedCount = failed.get().getAndSet(0);
    int skippedCount = skipped.get().getAndSet(0);
    if (acquiredCount > 0 || evaluatedCount > 0 || failedCount > 0 || skippedCount > 0) {
      log.info("Acquired {}, evaluated {} ({} failed, {} skipped) since last poll.",
          acquiredCount, evaluatedCount, failedCount, skippedCount);
    }
  }

  class EvaluationQueueTask
      implements Runnable
  {
    private final EvaluationQueue item;

    private final Runnable delegate;

    EvaluationQueueTask(final EvaluationQueue item, final Runnable delegate) {
      this.item = item;
      this.delegate = delegate;
    }

    @Override
    public void run() {
      queuedItemIds.get().remove(item.getId());
      delegate.run();
    }

    public EvaluationQueue getItem() {
      return item;
    }

    public Runnable getDelegate() {
      return delegate;
    }
  }

  private void tryEvaluate(final EvaluationQueue item) {
    try {
      evaluate(item);
    }
    catch (IOException | InterruptedException | RuntimeException e) {
      failed.get().incrementAndGet();
      log.warn(
          "Failed evaluation queue item with priority {} for application id {}, stage {}, and version {}. Unacquiring row.",
          item.getPriority(), item.getApplicationId(), item.getStageTypeId(), item.getVersion());
      evaluationQueueDAO.unacquireRows(Set.of(item.getId()));
      if (e instanceof IOException ioException) {
        throw new UncheckedIOException(ioException);
      }
      if (e instanceof InterruptedException interruptedException) {
        throw new UncheckedInterruptedException(interruptedException);
      }
      throw (RuntimeException) e;
    }
  }

  // Visible for testing
  void evaluate(final EvaluationQueue item) throws IOException, InterruptedException {
    Consumer<EvaluationQueue> consumer = getConsumer(item);

    if (consumer == null) {
      log.warn("Unsupported evaluation queue item with priority {} for application id {}, stage {}, and version {}.",
          item.getPriority(), item.getApplicationId(), item.getStageTypeId(), item.getVersion());
      evaluationQueueDAO.delete(item);
      skipped.get().incrementAndGet();
      return;
    }

    long start = System.currentTimeMillis();
    log.debug("Starting evaluation queue item with priority {} for application id {}, stage {}, and version {}.",
        item.getPriority(), item.getApplicationId(), item.getStageTypeId(), item.getVersion());

    consumer.accept(item);
    evaluationQueueDAO.delete(item);
    evaluated.get().incrementAndGet();

    log.debug(
        "Finished evaluation queue item with priority {} for application id {}, stage {}, and version {} in {} ms.",
        item.getPriority(), item.getApplicationId(), item.getStageTypeId(), item.getVersion(),
        System.currentTimeMillis() - start);
  }

  private ThrowingConsumer<EvaluationQueue> getConsumer(final EvaluationQueue item) {
    if (ComplianceStageType.ID.equals(item.getStageTypeId())) {
      return this::evaluateSbom;
    }
    return null;
  }

  // Visible for testing
  void evaluateSbom(final EvaluationQueue item) throws IOException, InterruptedException {
    ThirdPartySbomMetadata sbom =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(item.getApplicationId(), item.getVersion());
    if (sbom == null) {
      skipped.get().incrementAndGet();
      log.debug("No SBOM found for application id {} and version {}.", item.getApplicationId(), item.getVersion());
      return;
    }

    Application app = applicationDAO.getById(sbom.getApplicationId());
    if (app == null) {
      skipped.get().incrementAndGet();
      log.debug("No application found for SBOM with application id {} and version {}.", sbom.getApplicationId(),
          sbom.getSbomVersion());
      return;
    }

    ThirdPartyScan thirdPartyScan = thirdPartyScanDAO.getByThirdPartyFileId(sbom.getThirdPartyFileId());
    if (thirdPartyScan == null) {
      skipped.get().incrementAndGet();
      log.debug("No third party scan found with id {} for SBOM with application id {} and version {}.",
          sbom.getThirdPartyFileId(), sbom.getApplicationId(), sbom.getSbomVersion());
      return;
    }

    ScanEntity filteredScan = null;
    if (thirdPartyScan.getFilteredScanFile() != null) {
      filteredScan =
          scanPersistenceService.getScanByName(sbom.getApplicationId(), thirdPartyScan.getFilteredScanFile());
    }
    if (filteredScan == null || !filteredScan.exists()) {
      skipped.get().incrementAndGet();
      log.debug(
          "No filtered scan found at {} for third party scan with id {} for SBOM with application id {} and" +
              " version {}.",
          thirdPartyScan.getFilteredScanFile(), thirdPartyScan.getId(), sbom.getApplicationId(),
          sbom.getSbomVersion());
      return;
    }

    ScanReceipt scanReceipt = scanUploadService.upload(
        filteredScan,
        app,
        ComplianceStageType.ID,
        ClientScanType.SONATYPE,
        null,
        null,
        null,
        false);
    scanReceipt.waitForReport();
    thirdPartyScan.setPreviousScanId(thirdPartyScan.getScanId());
    thirdPartyScan.setScanId(scanReceipt.getScanId());
    thirdPartyScanDAO.update(thirdPartyScan);

    PolicyEvaluation eval = policyEvaluationDAO.getLastByOwnerIdAndScanId(
        app.getId(),
        thirdPartyScan.getPreviousScanId());
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluateForMonitoring(
        app,
        scanReceipt.getScanId(),
        new Stage(ComplianceStageType.ID),
        Optional.ofNullable(eval).map(PolicyEvaluation::getScanTriggerType).orElse(ScanTriggerType.SBOM_UI),
        Optional.ofNullable(eval).map(PolicyEvaluation::getClientScanType).orElse(ClientScanType.SONATYPE_THIRD_PARTY));
    policyAlertNotifier.sendNotifications(app, results);
  }

  // Visible for testing
  TenantScheduledThreadPoolExecutor createScheduledExecutorService() {
    TenantScheduledThreadPoolExecutor executor = new TenantScheduledThreadPoolExecutor(
        1,
        new ThreadFactoryBuilder().setNameFormat("EvaluationQueueConsumerScheduler-%d").build());
    shutdownHandler.add(executor);
    return executor;
  }

  // Visible for testing
  TenantThreadPoolExecutor createExecutorService() {
    EvaluationQueueConfig config = evaluationQueueConfigs.get();
    TenantThreadPoolExecutor executor = new TenantThreadPoolExecutor(
        config.consumerThreadsPerTenant(),
        config.consumerThreadsPerTenant(),
        5L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("EvaluationQueueConsumer-%d").build(),
        new AbortPolicy(),
        "evaluation_queue_consumer",
        getClass().getSimpleName())
    {
      @Override
      public Future<?> submit(final Runnable task) {
        if (task instanceof EvaluationQueueTask evaluationQueueTask) {
          queuedItemIds.get().add(evaluationQueueTask.getItem().getId());
        }
        return super.submit(task);
      }

      @Override
      public void shutdown() {
        super.shutdown();
        clearQueueAndUnacquireRows();
      }

      @Override
      public List<Runnable> shutdownNow() {
        List<Runnable> result = super.shutdownNow();
        clearQueueAndUnacquireRows();
        return result;
      }

      private void clearQueueAndUnacquireRows() {
        getQueue().clear();
        evaluationQueueService.unacquireRows(queuedItemIds.remove());
      }
    };
    executor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(executor);
    return executor;
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG)) {
      EvaluationQueueConfig oldConfig = evaluationQueueConfigs.get();
      EvaluationQueueConfig newConfig = getEvaluationQueueConfig();
      evaluationQueueConfigs.set(newConfig);

      if (oldConfig.enabled() != newConfig.enabled() ||
          oldConfig.consumerPeriodInMilliseconds() != newConfig.consumerPeriodInMilliseconds())
      {
        reschedule(newConfig);
      }

      int newSize = newConfig.consumerThreadsPerTenant();
      ThreadPoolExecutor executor = executors.get();
      if (newSize > executor.getCorePoolSize()) {
        log.debug("Increased evaluation queue consumer thread pool size to {}.", newSize);
        executor.setMaximumPoolSize(newSize);
        executor.setCorePoolSize(newSize);
      }
      else if (newSize < executor.getCorePoolSize()) {
        log.debug("Decreased evaluation queue consumer thread pool size to {}.", newSize);
        executor.setCorePoolSize(newSize);
        executor.setMaximumPoolSize(newSize);
      }
    }
  }

  // Only for tests
  void cleanup() {
    scheduleExecutors.get().shutdownNow();
    scheduleExecutors.remove();
    scheduledFutures.remove();

    executors.get().shutdownNow();
    executors.remove();

    queuedItemIds.remove();
    evaluationQueueConfigs.remove();

    acquired.remove();
    evaluated.remove();
    failed.remove();
    skipped.remove();
  }
}
