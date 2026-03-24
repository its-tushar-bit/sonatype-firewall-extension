/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.configuration.KeyValueDAO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.configuration.KeyValue;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.PageIterator;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.servlets.tasks.Task;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class EvaluationQueueProducer
    extends Task
    implements InsightJob, ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(EvaluationQueueProducer.class);

  private static final int DEFAULT_PAGE_SIZE = 10000;

  private static final String TASK_NAME = "EvaluationQueueProducer";

  private static final String RESET_CYCLE = "resetCycle";

  private final ApiConfigurationService apiConfigurationService;

  private final TaskScheduler taskScheduler;

  private final VersionEvaluationWindowDAO versionEvaluationWindowDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final KeyValueDAO keyValueDAO;

  private final EvaluationQueueDAO evaluationQueueDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final EvaluationQueueService evaluationQueueService;

  private final TenantReference<EvaluationQueueConfig> evaluationQueueConfigs;

  private final ProductLicense productLicense;

  @Inject
  public EvaluationQueueProducer(
      final ApiConfigurationService apiConfigurationService,
      final TaskScheduler taskScheduler,
      final VersionEvaluationWindowDAO versionEvaluationWindowDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final KeyValueDAO keyValueDAO,
      final EvaluationQueueDAO evaluationQueueDAO,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final EvaluationQueueService evaluationQueueService,
      final ProductLicense productLicense)
  {
    super(TASK_NAME);
    this.apiConfigurationService = apiConfigurationService;
    this.taskScheduler = taskScheduler;
    this.versionEvaluationWindowDAO = versionEvaluationWindowDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.keyValueDAO = keyValueDAO;
    this.evaluationQueueDAO = evaluationQueueDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.evaluationQueueConfigs = new TenantReference<>(this::getEvaluationQueueConfig);
    this.evaluationQueueService = evaluationQueueService;
    this.productLicense = productLicense;
  }

  @Override
  public void register() {
    reschedule(evaluationQueueConfigs.get());
  }

  private void reschedule(final EvaluationQueueConfig config) {
    if (config.enabled()) {
      log.debug("Scheduling evaluation queue producer every {} ms.", config.producerPeriodInMilliseconds());
      taskScheduler.schedulePeriodicTask(this, Duration.ofMillis(config.producerPeriodInMilliseconds()));
    }
    else {
      log.debug("Unscheduling evaluation queue producer.");
      taskScheduler.unscheduleTask(this);
    }
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run {}.", TASK_NAME);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(RESET_CYCLE, String.valueOf(map.containsKey(RESET_CYCLE)));
    if (taskScheduler.isTaskScheduled(this)) {
      taskScheduler.triggerTaskNow(this, parameters);
    }
    else {
      log.debug("Evaluation queue is disabled, skipping execution.");
    }
    printWriter.write("Completed manual execution of " + TASK_NAME + ".\n");
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    EvaluationQueueConfig config = evaluationQueueConfigs.get();

    if (!config.enabled()) {
      log.debug("Evaluation queue is disabled, skipping execution.");
      return;
    }

    if (!productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)) {
      log.debug("Evaluation queue requires policy monitoring, skipping execution.");
      return;
    }

    try {
      evaluationQueueService.clearInactiveWorkerIds(config.consumerRowExpirationInMilliseconds());
    }
    catch (Exception e) {
      log.error("Failed to clear inactive worker IDs.", e);
    }

    EvaluationQueueProducerCheckpoint checkpoint;
    if (context != null && context.getMergedJobDataMap().getBoolean(RESET_CYCLE)) {
      log.info("Resetting cycle.");
      resetCycle();
      checkpoint = initializeEvaluationQueueProducerCheckpoint();
    }
    else {
      checkpoint = getEvaluationQueueProducerCheckpoint();
      if (checkpoint == null) {
        checkpoint = initializeEvaluationQueueProducerCheckpoint();
      }
    }

    long millisSinceStart = System.currentTimeMillis() - checkpoint.getStartTime().getTime();
    boolean cycleTimeExceeded = millisSinceStart >= config.cyclePeriodInMilliseconds();

    if (cycleTimeExceeded) {
      if (!checkpoint.isComplete()) {
        log.warn("Target cycle period exceeded without completion.");
      }
      if (checkpoint.isComplete() || config.resetCycleOnTimeout()) {
        if (config.resetCycleOnTimeout()) {
          resetCycle();
        }
        checkpoint = initializeEvaluationQueueProducerCheckpoint();
      }
    }
    else {
      if (checkpoint.isComplete()) {
        log.debug("Cycle already completed, skipping execution.");
        return;
      }
    }

    Queue<ThirdPartySbomMetadata> sbomsToAdd = getSbomsToAdd(config, checkpoint);
    processSbomsToAdd(sbomsToAdd, checkpoint);

    if (checkpoint.isComplete()) {
      log.info("Completed cycle.");
    }
  }

  private void resetCycle() {
    log.info("Resetting cycle.");
    try (TransactionContext tx = evaluationQueueDAO.createTransactionContext()) {
      tx.begin();
      evaluationQueueDAO.deleteAll(tx);
      keyValueDAO.deleteByKey(tx, KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT);
      tx.commit();
    }
  }

  private EvaluationQueueProducerCheckpoint initializeEvaluationQueueProducerCheckpoint() {
    log.info("Starting cycle.");
    EvaluationQueueProducerCheckpoint checkpoint = new EvaluationQueueProducerCheckpoint(
        new Date(System.currentTimeMillis()),
        null,
        0,
        (int) thirdPartySbomMetadataDAO.getMaxActiveSbomsAcrossApplications());
    keyValueDAO.setValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT,
        JsonUtils.writeUnformatted(checkpoint));
    return checkpoint;
  }

  private EvaluationQueueConfig getEvaluationQueueConfig() {
    return (EvaluationQueueConfig) apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG);
  }

  private EvaluationQueueProducerCheckpoint getEvaluationQueueProducerCheckpoint() {
    String value = keyValueDAO.getValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT);
    if (value == null) {
      return null;
    }
    try {
      return JsonUtils.parse(value, EvaluationQueueProducerCheckpoint.class);
    }
    catch (JsonProcessingException e) {
      log.error("Failed to parse the '{}' value '{}'. This may be due to the {} class changing.",
          KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT, value, EvaluationQueueProducerCheckpoint.class.getName(), e);
      log.debug("Cleaning up unparsable evaluation queue producer checkpoint.");
      keyValueDAO.deleteByKey(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT);
      return null;
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new UncheckedIOException(e);
    }
  }

  private Queue<ThirdPartySbomMetadata> getSbomsToAdd(
      final EvaluationQueueConfig config,
      final EvaluationQueueProducerCheckpoint checkpoint)
  {
    int maxSbomsToAdd = (int) (config.producerMaxQueuedRows() - evaluationQueueDAO.getCount());
    Queue<ThirdPartySbomMetadata> sbomsToAdd = new ArrayDeque<>(maxSbomsToAdd);

    int appCount = 0;
    int sbomCount = 0;
    String applicationId = checkpoint.getApplicationId();
    int latestOffset = checkpoint.getLatestOffset();

    outer:
    while (latestOffset < checkpoint.getMaxAppActiveSboms()) {
      Iterator<SbomWithData> sbomWithDataIterator = createSbomWithDataIterator(
          DEFAULT_PAGE_SIZE,
          applicationId,
          latestOffset);
      while (sbomWithDataIterator.hasNext()) {
        if (sbomsToAdd.size() >= maxSbomsToAdd) {
          break outer;
        }

        SbomWithData sbomWithData = sbomWithDataIterator.next();
        ThirdPartySbomMetadata sbom = sbomWithData.sbom();
        VersionEvaluationWindow window = sbomWithData.window();
        Date lastScanTime = sbomWithData.lastScanTime();

        if (!sbom.getApplicationId().equals(applicationId)) {
          applicationId = sbom.getApplicationId();
          appCount++;
        }
        sbomCount++;

        if (window != null && window.getMaxVersions() != null && latestOffset >= window.getMaxVersions()) {
          continue;
        }

        long ageInMillis = System.currentTimeMillis() - sbom.getCreatedAt().getTime();
        if (window != null && window.getMaxAgeInDays() != null &&
            ageInMillis >= Duration.ofDays(window.getMaxAgeInDays()).toMillis())
        {
          continue;
        }

        if (lastScanTime != null && lastScanTime.getTime() >= checkpoint.getStartTime().getTime()) {
          continue;
        }

        sbomsToAdd.add(sbom);
        checkpoint.setApplicationId(sbom.getApplicationId());
      }
      latestOffset++;
      checkpoint.setLatestOffset(latestOffset);
      applicationId = null;
      checkpoint.setApplicationId(applicationId);
    }
    log.debug("Processed {} SBOM(s) up to latest offset {} across {} application(s).", sbomCount, latestOffset,
        appCount);
    return sbomsToAdd;
  }

  private void processSbomsToAdd(
      final Queue<ThirdPartySbomMetadata> sbomsToAdd,
      final EvaluationQueueProducerCheckpoint checkpoint)
  {
    try (TransactionContext tx = evaluationQueueDAO.createTransactionContext()) {
      tx.begin();

      Integer maxPriority = evaluationQueueDAO.getMaxPriority(tx);
      int priority = maxPriority == null ? -1 : maxPriority;
      List<EvaluationQueue> entities = new ArrayList<>();
      Set<ThirdPartySbomMetadata> existing = evaluationQueueDAO.findExisting(tx, sbomsToAdd);
      while (!sbomsToAdd.isEmpty()) {
        ThirdPartySbomMetadata sbom = sbomsToAdd.poll();
        if (existing.contains(sbom)) {
          continue;
        }

        EvaluationQueue evaluationQueue = new EvaluationQueue();
        evaluationQueue.setPriority(++priority);
        evaluationQueue.setApplicationId(sbom.getApplicationId());
        evaluationQueue.setStageTypeId(ComplianceStageType.ID);
        evaluationQueue.setVersion(sbom.getSbomVersion());
        Date date = new Date();
        evaluationQueue.setCreateTime(date);
        evaluationQueue.setUpdateTime(date);
        entities.add(evaluationQueue);
      }
      evaluationQueueDAO.insertBatch(tx, entities, true);

      keyValueDAO.setValue(tx, KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT,
          JsonUtils.writeUnformatted(checkpoint));
      tx.commit();
    }
  }

  private record SbomWithData(ThirdPartySbomMetadata sbom, Date lastScanTime, VersionEvaluationWindow window)
  {
  }

  private Iterator<SbomWithData> createSbomWithDataIterator(
      final int pageSize,
      final String lastApplicationId,
      final int latestOffset)
  {
    return new PageIterator<>(1, pageSize, (p, pSize) -> {
      List<ThirdPartySbomMetadata> sboms =
          new ArrayList<>(thirdPartySbomMetadataDAO.getActiveAtLatestOffset(latestOffset, lastApplicationId, p, pSize));

      if (sboms.isEmpty()) {
        return List.of();
      }

      Set<String> appIds = sboms.stream().map(ThirdPartySbomMetadata::getApplicationId).collect(Collectors.toSet());

      Map<String, PolicyMonitoring> monitors =
          policyMonitoringDAO.getByOwnerIdsAndStageTypeIdsWithInheritance(appIds, ComplianceStageType.ID);

      sboms.removeIf(sbom -> monitors.get(sbom.getApplicationId()) == null);
      appIds.removeIf(appId -> monitors.get(appId) == null);

      Map<String, VersionEvaluationWindow> windows =
          versionEvaluationWindowDAO.getByOwnerIdsAndContextIdWithInheritance(appIds, ComplianceStageType.ID);

      Set<String> thirdPartyFileIds =
          sboms.stream().map(ThirdPartySbomMetadata::getThirdPartyFileId).collect(Collectors.toSet());

      Map<String, Date> lastScanTimeByThirdPartyFileId = thirdPartySbomMetadataDAO.getLastScanTimes(thirdPartyFileIds);

      return sboms.stream()
          .map(sbom -> new SbomWithData(
              sbom,
              lastScanTimeByThirdPartyFileId.get(sbom.getThirdPartyFileId()),
              windows.get(sbom.getApplicationId())))
          .toList();
    });
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG)) {
      EvaluationQueueConfig oldConfig = evaluationQueueConfigs.get();
      EvaluationQueueConfig newConfig = getEvaluationQueueConfig();
      evaluationQueueConfigs.set(newConfig);
      if (oldConfig.enabled() != newConfig.enabled() ||
          oldConfig.producerPeriodInMilliseconds() != newConfig.producerPeriodInMilliseconds())
      {
        reschedule(newConfig);
      }
    }
  }
}
