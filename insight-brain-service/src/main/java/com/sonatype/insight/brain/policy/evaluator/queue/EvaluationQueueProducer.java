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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
import com.sonatype.insight.brain.service.Configuration;
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

  private static final String START_TIME = "startTime";

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

  private final Configuration configuration;

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
      final ProductLicense productLicense,
      final Configuration configuration)
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
    this.configuration = configuration;
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
    if (map.containsKey(START_TIME)) {
      String startTimeValue = map.get(START_TIME).get(0);
      parameters.put(START_TIME, "now".equalsIgnoreCase(startTimeValue)
          ? String.valueOf(System.currentTimeMillis())
          : startTimeValue);
    }
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
    boolean isResetCycle = context != null && context.getMergedJobDataMap().getBoolean(RESET_CYCLE);
    Date startTimeOverride = getStartTimeOverride(context);
    Date initialStartTime = startTimeOverride != null ? startTimeOverride : getInitialCycleStartTime();
    if (isResetCycle) {
      resetCycle();
      checkpoint = initializeEvaluationQueueProducerCheckpoint(initialStartTime);
    }
    else {
      checkpoint = getEvaluationQueueProducerCheckpoint();
      if (checkpoint == null) {
        checkpoint = initializeEvaluationQueueProducerCheckpoint(initialStartTime);
      }
    }

    if (System.currentTimeMillis() < checkpoint.getStartTime().getTime()) {
      log.debug("Waiting for policy monitoring hour, skipping execution.");
      return;
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
        checkpoint = initializeEvaluationQueueProducerCheckpoint(getRenewalCycleStartTime());
        if (System.currentTimeMillis() < checkpoint.getStartTime().getTime()) {
          log.debug("Waiting for policy monitoring hour, skipping execution.");
          return;
        }
      }
    }
    else {
      if (checkpoint.isComplete()) {
        log.debug("Cycle already completed, skipping execution.");
        return;
      }
    }

    ProducerSbomResult result = getSbomsToAdd(config, checkpoint);
    int queued = processSbomsToAdd(result.sbomsToAdd(), checkpoint);
    if (result.sbomCount() > 0 || queued > 0) {
      log.info("Processed {} SBOM(s), queued {} for evaluation. Progress: offset {} / {}.",
          result.sbomCount(), queued, checkpoint.getLatestOffset(), checkpoint.getMaxAppActiveSboms());
    }

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

  private EvaluationQueueProducerCheckpoint initializeEvaluationQueueProducerCheckpoint(final Date startTime) {
    EvaluationQueueProducerCheckpoint checkpoint = new EvaluationQueueProducerCheckpoint(
        startTime,
        null,
        0,
        (int) thirdPartySbomMetadataDAO.getMaxActiveSbomsAcrossApplications());
    log.info("Starting cycle with {} max SBOM version(s) per application.", checkpoint.getMaxAppActiveSboms());
    keyValueDAO.setValue(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT,
        JsonUtils.writeUnformatted(checkpoint));
    return checkpoint;
  }

  private Date getInitialCycleStartTime() {
    if (!evaluationQueueConfigs.get().startTimeDelayEnabled()) {
      return new Date(System.currentTimeMillis());
    }
    return getInitialCycleStartTime(configuration.getPolicyMonitoringHour(), System.currentTimeMillis());
  }

  private Date getRenewalCycleStartTime() {
    if (!evaluationQueueConfigs.get().startTimeDelayEnabled()) {
      return new Date(System.currentTimeMillis());
    }
    return getRenewalCycleStartTime(configuration.getPolicyMonitoringHour(), System.currentTimeMillis());
  }

  static Date getInitialCycleStartTime(final Integer policyMonitoringHour, final long now) {
    if (policyMonitoringHour == null) {
      return new Date(now);
    }
    LocalTime targetTime = LocalTime.of(policyMonitoringHour, 0);
    LocalDate today = new Date(now).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    long todayAtTargetHour = today.atTime(targetTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    if (todayAtTargetHour <= now) {
      return new Date(today.plusDays(1).atTime(targetTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    return new Date(todayAtTargetHour);
  }

  static Date getRenewalCycleStartTime(final Integer policyMonitoringHour, final long now) {
    if (policyMonitoringHour == null) {
      return new Date(now);
    }
    LocalTime targetTime = LocalTime.of(policyMonitoringHour, 0);
    LocalDate today = new Date(now).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    return new Date(today.atTime(targetTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
  }

  private Date getStartTimeOverride(final JobExecutionContext context) {
    if (context == null) {
      return null;
    }
    String value = context.getMergedJobDataMap().getString(START_TIME);
    if (value == null) {
      return null;
    }
    return new Date(Long.parseLong(value));
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

  private record ProducerSbomResult(Queue<ThirdPartySbomMetadata> sbomsToAdd, int sbomCount)
  {
  }

  private ProducerSbomResult getSbomsToAdd(
      final EvaluationQueueConfig config,
      final EvaluationQueueProducerCheckpoint checkpoint)
  {
    int maxSbomsToAdd = (int) (config.producerMaxQueuedRows() - evaluationQueueDAO.getCount());
    Queue<ThirdPartySbomMetadata> sbomsToAdd = new ArrayDeque<>(maxSbomsToAdd);

    int sbomCount = 0;
    String applicationId = checkpoint.getApplicationId();
    int latestOffset = checkpoint.getLatestOffset();
    boolean anyPassedVersionAndAgeFilters = false;
    // Usually when the inner while loop completes, we would have iterated over all applications.
    // However, if we resumed from some application, then this would not be true
    // i.e. we only set this to true initially if we haven't resumed
    boolean completedFullRank = applicationId == null;

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

        anyPassedVersionAndAgeFilters = true;

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

      if (completedFullRank && !anyPassedVersionAndAgeFilters) {
        checkpoint.setLatestOffset(checkpoint.getMaxAppActiveSboms());
        break;
      }
      completedFullRank = true;
      anyPassedVersionAndAgeFilters = false;
    }
    log.debug("Processed {} SBOM(s) up to latest offset {}.", sbomCount, latestOffset);
    return new ProducerSbomResult(sbomsToAdd, sbomCount);
  }

  private int processSbomsToAdd(
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
      return entities.size();
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
