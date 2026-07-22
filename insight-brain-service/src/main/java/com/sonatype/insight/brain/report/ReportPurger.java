/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.InsightWork;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jooq.exception.DataAccessException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ReportPurger
    extends AdminTask
    implements InsightJob
{
  public static final String PATH = "purgeObsoleteReports";

  public static final String NAME = "ReportPurger";

  private static final String PURGE_ERROR = "Report Purging error";

  @Override
  public String getJobName() {
    return NAME;
  }

  private static class TrashBucket
  {
    private static final int MAX_COUNT = 5000;

    final String bucketId;

    int itemCount;

    TrashBucket(String bucketId) {
      this.bucketId = bucketId;
    }

    void add() {
      itemCount++;
    }

    boolean isFull() {
      return itemCount >= MAX_COUNT;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(ReportPurger.class);

  private static final Comparator<PolicyEvaluation> POLICY_EVALUATION_TIME_COMPARATOR =
      Comparator.comparing(PolicyEvaluation::getTime).reversed();

  private static final int MAX_RETRIES = 10;

  private final InsightWork work;

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final List<String> contextIds;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  private final Configuration configuration;

  private final LifecycleReportPersistenceService lifecycleReportPersistenceService;

  private final ScanPersistenceService scanPersistenceService;

  @Inject
  public ReportPurger(
      final InsightWork work,
      final DataRetentionPolicyDAO dataRetentionPolicyDAO,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final TaskScheduler taskScheduler,
      final Configuration configuration,
      final LifecycleReportPersistenceService lifecycleReportPersistenceService,
      final ScanPersistenceService scanPersistenceService)
  {
    super(PATH);
    this.work = work;
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.taskScheduler = taskScheduler;
    this.configuration = configuration;
    this.scanPersistenceService = scanPersistenceService;
    contextIds = Stream
        .concat(StageTypes.getAll().stream().map(StageType::getId).filter(stageId -> !Stage.ID_PROXY.equals(stageId)),
            Stream.of(DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING))
        .collect(toList());
    this.lifecycleReportPersistenceService = lifecycleReportPersistenceService;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }

    taskScheduler.scheduleDailyTask(this, LocalTime.of(1, 0));
  }

  @Override
  public void deregister() {
    // no-op
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    taskScheduler.triggerTaskNow(this, null);
  }

  /**
   * @since 1.95
   */
  @Override
  public void execute(JobExecutionContext context) {
    execute(this::purgeReports, log, PURGE_ERROR);
  }

  void purgeReports() {
    List<Application> applications = applicationDAO.getAll();
    log.debug("Purging obsolete reports from {} applications", applications.size());
    int purged = 0;
    Map<String, TrashBucket> trashBuckets = new HashMap<>();
    for (Application application : applications) {
      TrashBucket trashBucket = trashBuckets.computeIfAbsent(getTrashBucketId(application), TrashBucket::new);
      purged += purgeReports(application, trashBucket);
    }
    log.info("Purged {} obsolete reports from {} applications", purged, applications.size());
  }

  private int purgeReports(Application application, TrashBucket trashBucket) {
    log.debug("Purging obsolete reports from application {}", application.getName());
    int purged = 0;
    for (String contextId : contextIds) {
      if (trashBucket.isFull()) {
        break;
      }
      for (int retry = 0; retry <= MAX_RETRIES; retry++) {
        try {
          purged += purgeReports(application, contextId, trashBucket);
          break;
        }
        catch (DataAccessException e) {
          // This exception occurs usually when the embedded database is under too much load from concurrent queries.
          // To avoid having to start over the entire purging task, we retry to get this purging run completed.
          if (retry >= MAX_RETRIES) {
            throw e;
          }
          Duration delay = getDelayForRetry(retry);
          log.debug("Failed to determine obsolete {} reports for application {}, retrying in {}", contextId,
              application.getName(), delay, retry == 0 ? e : null);
          try {
            Thread.sleep(delay.toMillis());
          }
          catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            e.addSuppressed(ex);
            throw e;
          }
        }
      }
    }
    if (purged > 0) {
      log.info("Purged {} obsolete reports from application {}", purged, application.getName());
    }
    else {
      log.debug("Purged no obsolete reports from application {}", application.getName());
    }
    return purged;
  }

  Duration getDelayForRetry(int retry) {
    return Duration.ofSeconds(1 << retry);
  }

  private int purgeReports(Application application, String contextId, TrashBucket trashBucket) {
    DataRetentionPolicy dataRetentionPolicy = getDataRetentionPolicy(application, contextId);
    if (dataRetentionPolicy == null || !dataRetentionPolicy.isPurgingEnabled()) {
      return 0;
    }
    log.debug("Using data retention policy for {} reports for owner {} with maxCount {} and maxAgeInDays {}.",
        dataRetentionPolicy.getContextId(), dataRetentionPolicy.getOwnerId(), dataRetentionPolicy.getMaxCount(),
        dataRetentionPolicy.getMaxAgeInDays());
    List<PolicyEvaluation> evaluations;
    boolean isForMonitoring = DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING.equals(contextId);
    if (isForMonitoring) {
      evaluations = policyEvaluationDAO.getPrimaryForMonitoringByApplicationId(application.getId());
    }
    else {
      evaluations =
          policyEvaluationDAO.getPrimaryNonMonitoringByApplicationIdAndStageId(application.getId(), contextId);
    }
    if (evaluations.isEmpty()) {
      return 0;
    }
    evaluations = new ArrayList<>(evaluations);
    evaluations.sort(POLICY_EVALUATION_TIME_COMPARATOR);
    log.debug("Found {} primary {} reports.", evaluations.size(), isForMonitoring ? "monitoring" : "non-monitoring");
    if (!evaluations.isEmpty()) {
      PolicyEvaluation oldestPolicyEvaluation = evaluations.get(evaluations.size() - 1);
      PolicyEvaluation newestPolicyEvaluation = evaluations.get(0);
      log.debug("Oldest report {} with time {}. Newest report {} with time {}.", oldestPolicyEvaluation.getScanId(),
          oldestPolicyEvaluation.getTime(), newestPolicyEvaluation.getScanId(), newestPolicyEvaluation.getTime());
    }
    int deleteFrom = evaluations.size();
    if (dataRetentionPolicy.getMaxCount() != null) {
      deleteFrom = Math.min(deleteFrom, dataRetentionPolicy.getMaxCount());
    }
    if (dataRetentionPolicy.getMaxAgeInDays() != null) {
      PolicyEvaluation oldestRetainedEvaluation = new PolicyEvaluation();
      Date cutoffDate = getCutoffDate(dataRetentionPolicy.getMaxAgeInDays());
      log.debug("Determined cutoff date to be {}.", cutoffDate);
      oldestRetainedEvaluation.setTime(cutoffDate);
      int oldestRetainedIndex =
          Collections.binarySearch(evaluations, oldestRetainedEvaluation, POLICY_EVALUATION_TIME_COMPARATOR);
      if (oldestRetainedIndex < 0) {
        deleteFrom = Math.min(deleteFrom, -oldestRetainedIndex - 1);
      }
      else {
        deleteFrom = Math.min(deleteFrom, oldestRetainedIndex + 1);
      }
    }
    Set<String> purgeableScanIds =
        evaluations.subList(deleteFrom, evaluations.size()).stream().map(PolicyEvaluation::getScanId).collect(toSet());
    if (!evaluations.isEmpty() && !purgeableScanIds.isEmpty()) {
      PolicyEvaluation fromEvaluation = evaluations.get(evaluations.size() - 1);
      PolicyEvaluation toEvaluation = evaluations.get(deleteFrom);
      log.debug("Purging {} reports from report {} with time {} to report {} with time {}.", purgeableScanIds.size(),
          fromEvaluation.getScanId(), fromEvaluation.getTime(), toEvaluation.getScanId(), toEvaluation.getTime());
    }
    policyEvaluationDAO.getLastByApplicationIds(Collections.singleton(application.getId()))
        .stream()
        .map(PolicyEvaluation::getScanId)
        .forEach(purgeableScanIds::remove);
    return purgeReports(application, purgeableScanIds, trashBucket);
  }

  private DataRetentionPolicy getDataRetentionPolicy(Application application, String contextId) {
    return dataRetentionPolicyDAO.getByOwnerIdAndContextIdWithHierarchy(application.getId(), contextId);
  }

  private Date getCutoffDate(int maxAgeInDays) {
    return Date.from(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(maxAgeInDays).toInstant());
  }

  private int purgeReports(Application application, Collection<String> scanIds, TrashBucket trashBucket) {
    int purged = 0;
    for (String scanId : scanIds) {
      try {
        if (purgeReport(application, scanId)) {
          purged++;
          if (lifecycleReportPersistenceService.supportsTrash()) {
            trashBucket.add();
            if (trashBucket.isFull()) {
              log.debug("Trash bucket {} is full for today", trashBucket.bucketId);
              break;
            }
          }
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException(
            "Failed to purge obsolete report " + scanId + " from application " + application.getName(), e);
      }
    }
    return purged;
  }

  private boolean purgeReport(Application application, String scanId) throws IOException {
    String purgeScanFiles = configuration.getPurgeScanFiles();
    if (ConfigurationUtils.WITH_REPORTS.equals(purgeScanFiles)) {
      try {
        scanPersistenceService.deleteScan(application.getId(), scanId);
      }
      catch (Exception suppressed) {
        log.info("Failed to delete scan file for: {}", scanId, suppressed);
      }
    }

    if (!lifecycleReportPersistenceService.reportExists(application.getId(), scanId)) {
      return false;
    }

    if (lifecycleReportPersistenceService.supportsTrash()) {
      Path reportDir = work.getReportDir(application.getId(), scanId).toPath();
      List<Path> reportFiles;
      try (Stream<Path> pathStream = Files.walk(reportDir)) {
        reportFiles = pathStream.collect(toCollection(ArrayList::new));
      }

      Path trashDir = work.getTrashDir().toPath().resolve(LocalDate.now().toString());
      // NOTE: Big installations have 5000+ evaluations per day and hence thousands of reports that need to be purged
      // To avoid having too many files in a single directory, we spread the daily trash load over several buckets
      trashDir = trashDir.resolve(getTrashBucketId(application));
      Files.createDirectories(trashDir);
      Path trashFile = trashDir.resolve("app-" + application.getId() + "-report-" + scanId + ".zip");

      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(trashFile))) {
        String zipEntryPrefix = toZipEntryName(work.getReportDir().toPath().relativize(reportDir)) + "/";
        for (Path reportFile : reportFiles) {
          if (!Files.isRegularFile(reportFile)) {
            continue;
          }
          String zipEntryName = toZipEntryName(reportDir.relativize(reportFile));
          if ("report.pdf".equals(zipEntryName)) {
            continue;
          }
          ZipEntry zipEntry = new ZipEntry(zipEntryPrefix + zipEntryName);
          zos.putNextEntry(zipEntry);
          Files.copy(reportFile, zos);
        }
      }
      catch (Exception e) {
        try {
          Files.deleteIfExists(trashFile);
        }
        catch (Exception suppressed) {
          e.addSuppressed(suppressed);
        }
        throw e;
      }
      log.info("Purging report {} from application {} to {}", reportDir, application.getName(), trashFile);
    }

    lifecycleReportPersistenceService.deleteReport(application.getId(), scanId);

    log.info("Purged report {} from application {}", scanId, application.getName());

    return true;
  }

  private String getTrashBucketId(Application application) {
    // 2 hex chars = 8 bit = 256 buckets
    return application.getId().substring(0, 2);
  }

  private String toZipEntryName(Path path) {
    return path.toString().replace(FileSystems.getDefault().getSeparator(), "/");
  }
}
